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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.report.CCIReportGenerator;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.ccimporter.model.CCIProjectList;
import com.blackducksoftware.tools.commonframework.core.config.ConfigConstants.APPLICATION;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;
import com.blackducksoftware.tools.commonframework.core.multithreading.ListDistributor;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.ProtexServerWrapper;

/**
 * Synchronizes a single Protex server with a Code Center server.
 * Multi-threaded.
 *
 * OBSOLETE; Replaced by CCISingleServerTaskProcessor.
 *
 * @author Ari Kamen
 * @date Jun 27, 2014
 *
 */
public class CCISingleServerProcessor extends CCIProcessor {

    private static final Logger log = LoggerFactory
            .getLogger(CCISingleServerProcessor.class.getName());

    private final ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper;

    private final int numThreads;

    private final ProjectProcessorThreadWorkerFactory threadFactory;

    private CCIReportGenerator reportGen;

    private boolean threadExceptionThrown = false;

    private boolean threadWaitInterrupted = false;

    private String threadExceptionMessages = "";

    /**
     * @param ccConfigManager
     * @param protexConfigManager
     * @throws Exception
     */
    public CCISingleServerProcessor(CodeCenterConfigManager ccConfigManager,
            ProtexConfigManager protexConfigManager,
            CodeCenterServerWrapper codeCenterServerWrapper) throws Exception {

        super(ccConfigManager, codeCenterServerWrapper);
        protexServerWrapper = createProtexServerWrapper(protexConfigManager);
        PlugInManager plugInManager = new PlugInManager(ccConfigManager, codeCenterServerWrapper, protexServerWrapper);

        // Construct the factory that the processor will use to create
        // the objects (run multi-threaded) to handle each subset of the project
        // list
        threadFactory = new ProjectProcessorThreadWorkerFactoryImpl(
                codeCenterServerWrapper, protexServerWrapper, ccConfigManager,
                plugInManager);

        numThreads = ccConfigManager.getNumThreads();

        // There will only be one in the single instance
        ServerBean protexBean = protexConfigManager.getServerBean(APPLICATION.PROTEX);

        log.info("Using Protex URL [{}]", protexBean.getServerName());
    }

    public CCISingleServerProcessor(CodeCenterConfigManager ccConfigManager,
            ProtexConfigManager protexConfigManager,
            CodeCenterServerWrapper codeCenterServerWrapper,
            ProjectProcessorThreadWorkerFactory threadFactory) throws Exception {

        super(ccConfigManager, codeCenterServerWrapper);
        protexServerWrapper = createProtexServerWrapper(protexConfigManager);
        this.threadFactory = threadFactory;

        numThreads = ccConfigManager.getNumThreads();

        // There will only be one in the single instance
        ServerBean protexBean = protexConfigManager.getServerBean(APPLICATION.PROTEX);

        log.info("Using Protex URL [{}]", protexBean.getServerName());
    }

    /**
     * Synchronize Code Center with Protex.
     */
    @Override
    public void performSynchronize() throws CodeCenterImportException {

        List<CCIProject> projectList = getProjects().getList();

        log.info("Processing {} projects for synchronization", projectList);
        if (projectList.size() == 0) {
            throw new CodeCenterImportException(
                    "No valid projects were specified.");
        }

        ListDistributor distrib = new ListDistributor(numThreads,
                projectList.size());

        // Create a temporary list of summary reports, one for each thread
        List<CCIReportSummary> threadsReportSummaryList = new ArrayList<CCIReportSummary>();
        List<CCIReportSummary> synchronizedThreadsReportSummaryList = Collections
                .synchronizedList(threadsReportSummaryList);

        // Launch a bunch of threads to process apps
        List<Thread> startedThreads = new ArrayList<Thread>(
                distrib.getNumThreads());
        for (int i = 0; i < distrib.getNumThreads(); i++) {
            List<CCIProject> partialProjectList = projectList.subList(
                    distrib.getFromListIndex(i), distrib.getToListIndex(i));

            Runnable threadWorker = threadFactory
                    .createProjectProcessorThreadWorker(partialProjectList,
                            synchronizedThreadsReportSummaryList);

            Thread thread = new Thread(threadWorker, "ProjectProcessorThread"
                    + i);
            thread.setUncaughtExceptionHandler(new WorkerThreadExceptionHandler());
            log.info("Starting thread " + thread.getName());
            thread.start();
            startedThreads.add(thread);
        }

        // Now wait for all threads to finish
        for (Thread startedThread : startedThreads) {
            log.info("Waiting for thread " + startedThread.getName());
            try {
                startedThread.join();
            } catch (InterruptedException e) {
                threadWaitInterrupted = true;
            }
        }

        log.info("Consolidated summary:\n"
                + synchronizedThreadsReportSummaryList.toString());

        if (threadExceptionThrown) {
            log.error("Exception(s) thrown from worker thread(s): "
                    + threadExceptionMessages);
        } else if (threadWaitInterrupted) {
            log.error("Interrupted while waiting for worker threads to finish");
        } else {
            log.info("All threads finished.");
        }

        reportSummaryList = threadsReportSummaryList;
    }

    /**
     * Get exception messages generated during performSynchronize() method.
     *
     * @return
     */
    public String getThreadExceptionMessages() {
        return threadExceptionMessages;
    }

    private CCIProjectList getProjects() throws CodeCenterImportException {
        return getProjects(protexServerWrapper);
    }

    /**
     * Generate report which can be used to verify whether or not Code Center
     * and Protex are in sync.
     *
     */
    @Override
    public void runReport() throws CodeCenterImportException {
        reportGen = new CCIReportGenerator(codeCenterWrapper,
                protexServerWrapper);
        CCIProjectList projectList = getProjects();

        log.info("Processing {} projects for reporting", projectList);
        reportGen.generateReport(projectList);
    }

    /**
     * Get the report generator object.
     */
    @Override
    public CCIReportGenerator getReportGen() {
        return reportGen;
    }

    private class WorkerThreadExceptionHandler implements
            Thread.UncaughtExceptionHandler {
        public WorkerThreadExceptionHandler() {
            log.debug("WorkerThreadExceptionHandler constructed");
        }

        @Override
        public void uncaughtException(Thread t, Throwable e) {
            String msg = "Thread " + t.getName() + " failed: " + e.getMessage();
            log.error(msg, e);
            threadExceptionThrown = true;
            synchronized (this) {
                threadExceptionMessages += ";" + msg;
            }
        }
    }

    private static ProtexServerWrapper<ProtexProjectPojo> createProtexServerWrapper(
            ProtexConfigManager configManager) throws Exception {
        ProtexServerWrapper<ProtexProjectPojo> protexWrapper;
        try {
            // Always just one code center
            ServerBean ccBean = configManager.getServerBean(APPLICATION.CODECENTER);
            if (ccBean == null) {
                throw new Exception(
                        "No valid Protex server configurations found");
            }

            log.info("Using Protex URL [{}]", ccBean.getServerName());

            protexWrapper = new ProtexServerWrapper<>(ccBean, configManager,
                    true);

        } catch (Exception e) {
            throw new Exception("Unable to establish Protex connection: "
                    + e.getMessage());
        }
        return protexWrapper;
    }
}
