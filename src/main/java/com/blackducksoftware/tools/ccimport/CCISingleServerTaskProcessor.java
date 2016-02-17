/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/

/**
 *
 */
package com.blackducksoftware.tools.ccimport;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionService;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportNamedException;
import com.blackducksoftware.tools.ccimport.report.CCIReportGenerator;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.ccimporter.model.CCIProjectList;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.ProtexServerWrapper;

/**
 * Synchronizes a single Protex server with a Code Center server using a
 * Producer/Consumer task approach.
 *
 * @author sbillings
 * @date Oct 29, 2015
 *
 */
public class CCISingleServerTaskProcessor extends CCIProcessor {

    private static final Logger log = LoggerFactory
            .getLogger(CCISingleServerTaskProcessor.class.getName());

    private final int numThreads;

    private final ProtexConfigManager protexConfigManager;

    private final ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper;

    private final SyncProjectTaskFactory taskFactory;

    private CCIReportSummary aggregatedResults;

    private StringBuilder threadExceptionMessages = new StringBuilder();

    /**
     * @param ccConfigManager
     * @param protexConfigManager
     * @throws Exception
     */
    public CCISingleServerTaskProcessor(
            CodeCenterConfigManager ccConfigManager,
            ProtexConfigManager protexConfigManager,
            CodeCenterServerWrapper codeCenterServerWrapper,
            ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper,
            SyncProjectTaskFactory taskFactory) throws Exception {

        super(ccConfigManager, codeCenterServerWrapper);
        this.protexConfigManager = protexConfigManager;
        this.protexServerWrapper = protexServerWrapper;
        this.taskFactory = taskFactory;
        numThreads = ccConfigManager.getNumThreads();

        // There will only be one in the single instance
        ServerBean protexBean = protexConfigManager.getServerBean();
        log.info("Using Protex URL [{}]", protexBean.getServerName());
    }

    /**
     * Synchronize Code Center with Protex.
     */
    @Override
    public void performSynchronize() throws CodeCenterImportException {
        ExecutorService exec = Executors.newFixedThreadPool(numThreads);
        boolean threadExceptionThrown = false;
        try {
            CompletionService<CCIReportSummary> completionService = new ExecutorCompletionService<>(
                    exec);
            List<CCIProject> projectList = getProjects().getList();

            log.info("Processing {} projects for synchronization", projectList.size());
            if (projectList.size() == 0) {
                throw new CodeCenterImportException(
                        "No valid projects were specified.");
            }

            aggregatedResults = new CCIReportSummary();
            aggregatedResults.setTotalProtexProjects(projectList.size());

            int numProjectsSubmitted = submitTasks(completionService,
                    projectList, aggregatedResults);

            // Collect results from tasks as they finish

            for (int taskNum = 0; taskNum < numProjectsSubmitted; taskNum++) {
                Future<CCIReportSummary> f = null;
                CCIReportSummary singleTaskResult = null;
                try {
                    f = completionService.take();
                    singleTaskResult = f.get();
                } catch (InterruptedException | ExecutionException e) {
                    String appName = getAppNameFromException(e);
                    String msg = composeErrorMessage(appName, e);
                    log.error(msg, e);
                    threadExceptionThrown = true;
                    threadExceptionMessages.append("; ");
                    threadExceptionMessages.append(msg);
                    singleTaskResult = generateErrorTaskResults(appName);
                }

                aggregatedResults.addReportSummary(singleTaskResult);
            }
        } finally {
            exec.shutdown();
        }
        if (threadExceptionThrown) {
            log.error("Exception(s) thrown from worker thread(s): "
                    + getThreadExceptionMessages());
        } else {
            log.info("All threads finished.");
        }
    }

    private String composeErrorMessage(String appName, Exception e) {
        StringBuilder sb = new StringBuilder("Error in task for app ");
        sb.append(appName);
        sb.append(": ");
        String eMessage = e.getMessage();
        sb.append(eMessage);
        String causeMessage = e.getCause().getMessage();
        if ((eMessage != null) && (e.getCause() != null)
                && (!eMessage.contains(causeMessage))) {
            sb.append(" (");
            sb.append(causeMessage);
            sb.append(")");
        }
        return sb.toString();
    }

    private CCIReportSummary generateErrorTaskResults(String appName) {
        CCIReportSummary singleTaskResult = new CCIReportSummary();
        singleTaskResult.setTotalCCApplications(1);
        // singleTaskResult.setTotalProtexProjects(1);
        singleTaskResult.addTotalImportsFailed();
        singleTaskResult.addToFailedImportList(appName);
        return singleTaskResult;
    }

    private String getAppNameFromException(Exception e) {
        String appName = "<unknown>";
        if (e instanceof ExecutionException) {
            if (e.getCause() instanceof CodeCenterImportNamedException) {
                appName = ((CodeCenterImportNamedException) e.getCause())
                        .getAppName();
            }
        }
        return appName;
    }

    private int submitTasks(
            CompletionService<CCIReportSummary> completionService,
            List<CCIProject> projectList, CCIReportSummary aggregatedResults) {
        int numSubmitted = 0;
        for (CCIProject project : projectList) {
            Pattern projectNameFilterPattern = protexConfigManager
                    .getProtexProjectNameFilterPattern();
            if (projectNameFilterPattern != null) {
                Matcher m = projectNameFilterPattern.matcher(project
                        .getProjectName());
                if (!m.matches()) {
                    log.info(
                            "Project {} does not match the project name filter; skipping it",
                            project.getProjectName());
                    CCIReportSummary skippedProjectResult = new CCIReportSummary();
                    skippedProjectResult.addTotalProjectsSkipped();
                    aggregatedResults.addReportSummary(skippedProjectResult);
                    continue;
                }
            }
            numSubmitted++;
            Callable<CCIReportSummary> task = taskFactory.createTask(project);
            completionService.submit(task);
        }
        return numSubmitted;
    }

    /**
     * Get exception messages generated during performSynchronize() method.
     *
     * @return
     */
    public String getThreadExceptionMessages() {
        return threadExceptionMessages.toString();
    }

    private CCIProjectList getProjects() throws CodeCenterImportException {
        return getProjects(protexServerWrapper);
    }

    /**
     * Generate report which can be used to verify whether or not Code Center
     * and Protex are in sync.
     *
     */
    // TODO remove report functionality?
    @Override
    public void runReport() throws CodeCenterImportException {
        throw new UnsupportedOperationException("runReport() not supported");
    }

    /**
     * Get the report generator object.
     */
    @Override
    public CCIReportGenerator getReportGen() {
        throw new UnsupportedOperationException("getReportGen() not supported");
    }

    public CCIReportSummary getAggregatedResults() {
        return aggregatedResults;
    }
}
