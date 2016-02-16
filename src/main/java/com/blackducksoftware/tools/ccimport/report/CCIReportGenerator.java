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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.tools.ccimport.report;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.ccimporter.model.CCIProjectList;
import com.blackducksoftware.tools.commonframework.standard.codecenter.dao.ApplicationDao;
import com.blackducksoftware.tools.commonframework.standard.datatable.DataTable;
import com.blackducksoftware.tools.commonframework.standard.datatable.FieldDef;
import com.blackducksoftware.tools.commonframework.standard.datatable.FieldType;
import com.blackducksoftware.tools.commonframework.standard.datatable.Record;
import com.blackducksoftware.tools.commonframework.standard.datatable.RecordDef;
import com.blackducksoftware.tools.commonframework.standard.datatable.writer.DataSetWriter;
import com.blackducksoftware.tools.commonframework.standard.datatable.writer.DataSetWriterExcel;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.codecenter.dao.CodeCenterApplicationDao;
import com.blackducksoftware.tools.connector.protex.ProtexServerWrapper;

/**
 * Class responsible for the report generation portion of the CCI.
 *
 *
 * @author akamen
 *
 */
public class CCIReportGenerator {

    private final Logger log = LoggerFactory.getLogger(this.getClass()
            .getName());

    private final static int EXCEL_CELL_MAX_CHARS = 32767;

    private final CodeCenterServerWrapper codeCenterWrapper;

    private final ProtexServerWrapper<ProtexProjectPojo> protexWrapper;

    private DataTable dataTable;

    private RecordDef recordDef;

    public CCIReportGenerator(CodeCenterServerWrapper codeCenterWrapper,
            ProtexServerWrapper<ProtexProjectPojo> protexWrapper) {
        this.codeCenterWrapper = codeCenterWrapper;
        this.protexWrapper = protexWrapper;

    }

    private HashMap<String, CCIProject> getProjectMap(
            CCIProjectList projectListObject) {
        List<CCIProject> projectList = projectListObject.getList();
        HashMap<String, CCIProject> projectMap = new HashMap<String, CCIProject>(
                projectList.size());
        for (CCIProject project : projectList) {
            projectMap.put(project.getProjectName(), project);
        }
        return projectMap;
    }

    /**
     * Generate the sync report (an Excel file).
     *
     * @param projectList
     * @throws CodeCenterImportException
     */
    public void generateReport(CCIProjectList projectList)
            throws CodeCenterImportException {
        try {
            generateReportInternal(projectList);
        } catch (Exception e) {
            throw new CodeCenterImportException(e.getMessage());
        }
    }

    private void generateReportInternal(CCIProjectList projectList)
            throws Exception {
        createDataTable();

        processInvalidUserSpecifiedProjects(projectList);
        HashMap<String, CCIProject> leftOvers = processValidProjects(projectList);
        processLeftOverProjects(leftOvers);

        DataSetWriter writerExcel = new DataSetWriterExcel("sync_report.xlsx");
        // DataSetWriter writerStdOut = new DataSetWriterStdOut();
        // writerStdOut.write(dataTable);
        writerExcel.write(dataTable);
    }

    private void createDataTable() {
        // Create the RecordDef
        List<FieldDef> fields = new ArrayList<FieldDef>();
        fields.add(new FieldDef("applicationName", FieldType.STRING,
                "Application Name"));
        fields.add(new FieldDef("applicationVersion", FieldType.STRING,
                "Application Version"));
        fields.add(new FieldDef("status", FieldType.STRING, "Status"));
        fields.add(new FieldDef("foundInCc", FieldType.STRING, "Found in CC"));
        fields.add(new FieldDef("foundInProtex", FieldType.STRING,
                "Found in Protex"));
        fields.add(new FieldDef("compListsMatch", FieldType.STRING,
                "Component Lists Match"));
        fields.add(new FieldDef("compLists", FieldType.STRING,
                "Component Lists"));
        recordDef = new RecordDef(fields);

        // Create a new/empty DataSet that uses that RecordDef
        dataTable = new DataTable(recordDef);
    }

    private void processInvalidUserSpecifiedProjects(CCIProjectList projectList)
            throws Exception {
        for (CCIProject project : projectList.getInvalidList()) {
            Record record = new Record(recordDef);
            record.setFieldValue("applicationName", project.getProjectName());
            record.setFieldValue("applicationVersion",
                    project.getProjectVersion());
            record.setFieldValue("status", "Error");
            record.setFieldValue("foundInCc", "N/A");
            record.setFieldValue("foundInProtex", "No");
            record.setFieldValue("compListsMatch", "N/A");
            record.setFieldValue("compLists", "");
            dataTable.add(record);
        }
    }

    // TODO: This method is too big / ugly... need to refactor it, but it would
    // be nice to have a comprehensive test first
    private HashMap<String, CCIProject> processValidProjects(
            CCIProjectList projectList) throws Exception {
        log.info("Project list: " + projectList);

        HashMap<String, CCIProject> protexProjectMap = getProjectMap(projectList);
        boolean projectListIsUserSpecifiedSubset = projectList
                .isUserSpecifiedSubset();

        // We already have all projects, time to get all the applications.
        // Request all applications that belong to the user
        List<Application> apps = null;
        try {
            apps = getAllApplications();
        } catch (CodeCenterImportException e) {
            log.error("Error getting Code Center applications: "
                    + e.getMessage());
        }

        HashMap<String, String> processedAppnames = new HashMap<String, String>(
                apps.size());
        // Go through each application and find corresponding Protex project
        int appCount = apps.size();
        int appIndex = 0;
        for (Application ccApp : apps) {
            log.info("Processing application " + ++appIndex + " of " + appCount
                    + "...");
            String appName = ccApp.getName();
            String appVersion = ccApp.getVersion();

            if (projectListIsUserSpecifiedSubset
                    && !protexProjectMap.containsKey(appName)) {
                log.info("["
                        + appName
                        + ":"
                        + appVersion
                        + "] is not in user-specified protex project list; skipping it");
                continue;
            }

            Record record = new Record(recordDef);
            record.setFieldValue("applicationName", appName);
            record.setFieldValue("applicationVersion", appVersion);
            record.setFieldValue("foundInCc", "Yes");

            // Defaults
            record.setFieldValue("status", "Error");
            record.setFieldValue("foundInProtex", "No");
            record.setFieldValue("compListsMatch", "N/A");
            record.setFieldValue("compLists", "");

            // We need this check for this scenario:
            // User has specified a list of project names to report on
            // One of those names appears twice in Code Center (two applications
            // / different versions)
            // In this scenario: when we get to the second app, because we
            // deleted it from protexProjectMap,
            // it looks like the user just didn't specify it (but they did)
            if (processedAppnames.containsKey(appName)) {
                log.warn("["
                        + appName
                        + "] exists multiple times (multiple versions); in Code Center");
                record.setFieldValue("foundInProtex", "No");
                record.setFieldValue("compListsMatch", "N/A");
                dataTable.add(record);
                continue;
            }

            processedAppnames.put(appName, appName); // remember that we've
            // processed this app so we
            // can check for duplicates

            try {
                codeCenterWrapper.getInternalApiWrapper().getApplicationApi()
                        .getAssociatedProtexProject(ccApp.getId());
            } catch (com.blackducksoftware.sdk.codecenter.fault.SdkFault e) {
                log.warn("[" + appName + ":" + appVersion
                        + "] no association found in CC (cause: "
                        + e.getMessage() + ")");
                record.setFieldValue("foundInProtex", "No");
                record.setFieldValue("compListsMatch", "N/A");
                dataTable.add(record);
                continue;
            } catch (Exception e2) {
                log.warn("Error: " + e2.getMessage());
                record.setFieldValue("foundInProtex", "No");
                record.setFieldValue("compListsMatch", "N/A");
                dataTable.add(record);
                continue;
            }

            log.info("[" + appName + ":" + appVersion
                    + "] application's association found in CC");

            if (protexProjectMap.containsKey(appName)) {
                log.info("[" + appName + ":" + appVersion
                        + "] existed in protexProjectMap");
                CCIProject protexProject = protexProjectMap.get(appName);
                protexProjectMap.remove(appName);
                record.setFieldValue("foundInProtex", "Yes");

                String compDiffString;
                try {
                    if ((compDiffString = getComponentListDiffString(
                            protexProject, ccApp, EXCEL_CELL_MAX_CHARS)) == null) {
                        log.info("["
                                + appName
                                + ":"
                                + appVersion
                                + "] Code Center / Protex Component lists are identical");

                        record.setFieldValue("compListsMatch", "Yes");
                        record.setFieldValue("status", "");
                    } else {
                        log.warn("["
                                + appName
                                + ":"
                                + appVersion
                                + "] Code Center / Protex Component lists are different");
                        record.setFieldValue("compListsMatch", "No");
                        record.setFieldValue("compLists", compDiffString);
                    }
                } catch (Exception e) {
                    log.warn(
                            "["
                                    + appName
                                    + ":"
                                    + appVersion
                                    + "] Error reading components from Code Center / Protex",
                            e);
                    record.setFieldValue("compListsMatch", "No");
                }

            } else {
                if (projectListIsUserSpecifiedSubset) {
                    log.warn("["
                            + appName
                            + ":"
                            + appVersion
                            + "] exists in Code Center but does not exist in protexProjectMap");
                    record.setFieldValue("foundInProtex", "No");
                }
            }
            dataTable.add(record);
        }

        return protexProjectMap;
    }

    /**
     * Get the generated report.
     *
     * @return
     */
    public DataTable getDataTable() {
        return dataTable;
    }

    private void processLeftOverProjects(HashMap<String, CCIProject> leftOvers)
            throws Exception {
        for (String key : leftOvers.keySet()) {
            CCIProject project = leftOvers.get(key);

            Record record = new Record(recordDef);
            record.setFieldValue("applicationName", project.getProjectName());
            record.setFieldValue("applicationVersion",
                    project.getProjectVersion());
            record.setFieldValue("status", "Error");
            record.setFieldValue("foundInCc", "No");
            record.setFieldValue("foundInProtex", "Yes");
            record.setFieldValue("compListsMatch", "N/A");
            record.setFieldValue("compLists", "");
            dataTable.add(record);
        }
    }

    /**
     * returns null if the component lists are identical, a string with both
     * lists if not.
     *
     * @param protexProject
     * @param ccApp
     * @param maxLen
     * @return
     * @throws Exception
     */
    private String getComponentListDiffString(CCIProject protexProject,
            Application ccApp, int maxLen) throws Exception {

        if (maxLen < 5) {
            throw new IllegalArgumentException("maxLen must be at least 5");
        }

        // Get / compare components
        ComponentCollector protexComponentCollector = new ProtexComponentCollector(
                protexWrapper, protexProject.getProjectKey());

        ComponentCollector ccComponentCollector;
        try {
            ApplicationDao ccDao = new CodeCenterApplicationDao(
                    codeCenterWrapper, false, ccApp);
            ccComponentCollector = new CodeCenterComponentCollector(ccDao);
        } catch (com.blackducksoftware.sdk.codecenter.fault.SdkFault e) {
            throw new SdkFault(e.getMessage());
        }

        log.debug("\tCode Center Component list: " + ccComponentCollector);
        log.debug("\tProtex      Component list: " + protexComponentCollector);
        log.info("Comparing Protex component list (\"this\") to Code Center component list (\"other\")");
        String diffString = protexComponentCollector
                .getDiffString(ccComponentCollector);

        return diffString; // null if lists are identical
    }

    private List<Application> getAllApplications()
            throws CodeCenterImportException {
        ApplicationPageFilter apf = new ApplicationPageFilter();
        apf.setFirstRowIndex(0);
        apf.setLastRowIndex(Integer.MAX_VALUE);
        List<Application> apps = null;

        try {
            log.info("Getting Code Center applications...");
            apps = codeCenterWrapper.getInternalApiWrapper()
                    .getApplicationApi().searchApplications("", apf);
            log.info("Returned {} applications.", apps.size());
        } catch (Exception ccie) {
            throw new CodeCenterImportException("Error getting applications",
                    ccie);
        }

        return apps;
    }
}
