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
package com.blackducksoftware.tools.ccimport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.administration.data.ServerNameToken;
import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationCreate;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ProjectIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ProtexRequest;
import com.blackducksoftware.sdk.codecenter.application.data.ValidationStatusEnum;
import com.blackducksoftware.sdk.codecenter.approval.data.WorkflowNameToken;
import com.blackducksoftware.sdk.codecenter.fault.ErrorCode;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.sdk.codecenter.request.data.RequestApplicationComponentToken;
import com.blackducksoftware.sdk.codecenter.request.data.RequestCreate;
import com.blackducksoftware.sdk.codecenter.request.data.RequestIdToken;
import com.blackducksoftware.sdk.codecenter.request.data.RequestSummary;
import com.blackducksoftware.sdk.codecenter.role.data.RoleNameToken;
import com.blackducksoftware.sdk.codecenter.user.data.UserNameToken;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.config.CCIConstants;
import com.blackducksoftware.tools.ccimporter.model.CCIApplication;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

/**
 * Performs actual importation and/or validation.
 *
 * @author Ari Kamen
 * @date Jul 1, 2014
 *
 */
public class CodeCenterProjectSynchronizer {
    private final Logger log = LoggerFactory.getLogger(this.getClass()
            .getName());

    private final ICodeCenterServerWrapper ccWrapper;

    private final IProtexServerWrapper<ProtexProjectPojo> protexWrapper;

    private final CCIConfigurationManager configManager;

    private final CCIReportSummary reportSummary = new CCIReportSummary();

    private final PlugInManager plugInManager;

    private final Pattern projectNameFilterPattern;

    public CodeCenterProjectSynchronizer(
            ICodeCenterServerWrapper codeCenterWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexWrapper,
            CCIConfigurationManager config, PlugInManager plugInManager) throws CodeCenterImportException {
        ccWrapper = codeCenterWrapper;
        this.protexWrapper = protexWrapper;
        configManager = config;
        this.plugInManager = plugInManager;

        projectNameFilterPattern = configManager
                .getProtexProjectNameFilterPattern();
    }

    /**
     * Synchronizes a list of projects against the specified Code Center
     * Configuration
     *
     * In the event of multiple Protex servers, the alias is used to lookup the
     * Protex configuration Otherwise the user supplied option is utilized
     * instead.
     *
     * @param projectList
     * @throws CodeCenterImportException
     */
    public void synchronize(List<CCIProject> projectList) {
        reportSummary.setTotalProtexProjects(projectList.size());

        int projectCounter = 1;
        for (CCIProject project : projectList) {
            Date now = new Date();
            long startMilliseconds = now.getTime();
            log.info("[{}/{}] Processing {}", projectCounter++,
                    projectList.size(), project.getProjectName());

            if (!matches(project.getProjectName())) {
                log.info(
                        "Project {} does not match the project name filter; skipping it",
                        project.getProjectName());
                reportSummary.addTotalProjectsSkipped();
                continue;
            }

            int importAttemptCount = importAndSync(project);
            now = new Date();
            long endMilliseconds = now.getTime();
            long duration = endMilliseconds - startMilliseconds;
            log.info("cc-import app import time (seconds): "
                    + Math.round(duration / 1000.0) + " (" + importAttemptCount + " attempts)");
        }

        // Here, we display the basic summary
        log.info("Summary for THIS thread:\n" + reportSummary.toString());
    }

    private int importAndSync(CCIProject project) {
        boolean retryImport = false;
        int importAttemptCount = 0;
        do {
            importAttemptCount++;
            retryImport = false;
            boolean importSuccess = false;
            CCIProject importedProject = null;
            try {
                importedProject = processImport(project);
                importSuccess = true;
            } catch (Exception e) {
                log.error(
                        "[{}] Unable to perform import: " + e.getMessage(),
                        project.getProjectName());
            }

            if (importSuccess) {
                retryImport = adjustApp(project, importedProject,
                        importAttemptCount);
            }
        } while (retryImport);
        return importAttemptCount;
    }

    private boolean matches(String projectName) {
        if (projectNameFilterPattern == null) {
            return true; // no pattern = do all
        }

        Matcher m = projectNameFilterPattern.matcher(projectName);
        if (m.matches()) {
            return true;
        }
        return false;
    }

    private boolean adjustApp(CCIProject project, CCIProject importedProject,
            int importRetryCount) {
        boolean retryImport = false;

        try {
            boolean bomWasChanged = adjustAppBom(importedProject);
            if (configManager.isAppAdjusterOnlyIfBomEdits()
                    && (bomWasChanged || importedProject.getCciApplication()
                            .isJustCreated())) {
                try {
                    plugInManager.invokeAppAdjuster(
                            importedProject.getCciApplication(), project);
                } catch (CodeCenterImportException e) {
                    log.error("Application Adjuster failed, but proceeding with validation.");
                }
            }
            // Initialize component change interceptor for this app
            plugInManager.invokeComponentChangeInterceptorInitForAppMethod(importedProject.getCciApplication().getApp().getId().getId());

        } catch (Exception e) {
            String exceptionMessage = e.getMessage();
            log.error("[{}] Unable to perform validation: " + exceptionMessage,
                    project.getProjectName());

            // If we got NoRemoteProjectFoundException, app may be associated
            // with wrong projex server
            // clear the assoc and retry (up to a couple times)
            if ((exceptionMessage.contains("NoRemoteProjectFoundException"))
                    && (configManager.isAttemptToFixInvalidAssociation())
                    && (importRetryCount < 2)) {

                retryImport = disassociateAppFromOldProject(project);
            }

            if (!retryImport) {
                // If we're not going to retry: report the error
                Application app = importedProject.getApplication();
                reportSummary.addTotalValidationsFailed();
                reportSummary.addToFailedValidationList(app.getName() + ":"
                        + app.getVersion());
            }
        }
        return retryImport;
    }

    private boolean adjustAppBom(CCIProject importedProject) throws CodeCenterImportException {
        boolean bomWasChanged = false;
        boolean performValidate = configManager.isValidate();
        if (performValidate) {
            bomWasChanged = validateAndSyncBom(importedProject,
                    reportSummary);

            if (configManager.isReValidateAfterBomChange() && bomWasChanged) {
                reValidate(importedProject);
            }
        }
        return bomWasChanged;
    }

    private void reValidate(CCIProject importedProject) {
        log.info("Re-running validation on " + importedProject.getProjectName());
        Application app = importedProject.getApplication();
        ApplicationIdToken appIdToken = app.getId();
        try {
            ccWrapper.getInternalApiWrapper().getApplicationApi()
                    .validate(appIdToken, false, false);
        } catch (SdkFault e) {
            log.error("Re-validate (after BOM change) failed: "
                    + e.getMessage());
        }
    }

    private boolean disassociateAppFromOldProject(CCIProject project) {
        boolean retryImport = true;
        ApplicationNameVersionToken appToken = new ApplicationNameVersionToken();
        appToken.setName(project.getProjectName());
        appToken.setVersion(project.getProjectVersion());
        try {
            ccWrapper.getInternalApiWrapper().getApplicationApi()
                    .disassociateProtexProject(appToken);
        } catch (SdkFault sdkFault) {
            log.error("Disassociate on app " + project.getProjectName() + " / "
                    + project.getProjectVersion() + " failed: "
                    + sdkFault.getMessage());
            retryImport = false;
        }
        return retryImport;
    }

    /**
     * An import consists of two steps. First creating and/or finding an
     * application within Code Center that matches Protex. Secondly to perform
     * the association between the CC and Protex server.
     *
     * @param projectList
     * @return Returns the existing project with a populated Application bean.
     * @throws CodeCenterImportException
     */
    private CCIProject processImport(CCIProject project)
            throws CodeCenterImportException {
        try {
            log.info("[{}] Attempting Protex project import. (version: {})",
                    project.getProjectName(), project.getProjectVersion());

            CCIApplication cciApp = null;
            String correspondingApplicationID = lookUpCorrespondingApplication(project);

            // Creates the application (if needed) and then perform the
            // association
            if (correspondingApplicationID == null) {
                log.info(
                        "No corresponding application found for project ID [{}], will attempt to create one",
                        project.getProjectKey());
                // This will return an existing application, or create a new
                // one. This is generic and not import specific.
                cciApp = createApplication(project);

                // This takes the Code Center app and attempts to associate it
                // with a Protex project. We do not need to return anything
                // because the return object is useless to us. Any failure to
                // obtain an
                // object signifies an error and thus the failure of the import.
                associateApplicationToProtexProject(project, cciApp.getApp());

                log.info("[{}] IMPORT SUCCESSFUL!", project.getProjectName());
                log.info("-----------------------------");
            }
            // Otherwise, use the information to perform the lookup
            else {
                log.info(
                        "Found existing association of application ID [{}] for project ID [{}]",
                        correspondingApplicationID, project.getProjectKey());
                ApplicationIdToken token = new ApplicationIdToken();
                token.setId(correspondingApplicationID);

                try {
                    Application app = ccWrapper.getInternalApiWrapper()
                            .getApplicationApi().getApplication(token);
                    cciApp = new CCIApplication(app, false);

                    log.info("[{}] Lookup successful for project [{}]:[{}]!",
                            project.getProjectName(),
                            cciApp.getApp().getName(), cciApp.getApp()
                                    .getVersion());
                    log.info("-----------------------------");

                } catch (SdkFault e) {
                    throw new CodeCenterImportException(
                            "Unable to look up application using id: "
                                    + correspondingApplicationID, e);
                }
            }

            if (!configManager.isAppAdjusterOnlyIfBomEdits()) {
                plugInManager.invokeAppAdjuster(cciApp, project);
            }

            // If everything goes well, set the application for
            // potential validation down the road.
            project.setCciApplication(cciApp);

        } catch (CodeCenterImportException ccie) {
            log.error("[{}] IMPORT FAILED, reason: [{}]",
                    project.getProjectName(), ccie.getMessage());
            reportSummary.addTotalImportsFailed();
            reportSummary.addToFailedImportList(project.getProjectName());
            throw new CodeCenterImportException(ccie.getMessage());
        }

        return project;
    }

    /**
     * Determines based on Protex project ID the ID of the associated CC
     * Application.
     *
     * @param project
     * @return
     * @throws CodeCenterImportException
     */
    private String lookUpCorrespondingApplication(CCIProject project)
            throws CodeCenterImportException {
        String correspondingApplicationID = null;

        ProjectIdToken projectIdToken = new ProjectIdToken();
        projectIdToken.setId(project.getProjectKey());
        ServerNameToken serverNameToken = new ServerNameToken();
        serverNameToken.setName(configManager.getProtexServerName());
        projectIdToken.setServerId(serverNameToken);
        Application correspondingApplication;
        try {
            correspondingApplication = ccWrapper.getInternalApiWrapper()
                    .getApplicationApi()
                    .getAssociatedApplication(projectIdToken);
        } catch (SdkFault e) {
            String msg = "Unable to get application associated with project "
                    + project.getProjectName() + ": " + e.getMessage();
            log.info(msg);
            return null;
        }
        log.info("Found association for project " + project.getProjectName()
                + ": " + correspondingApplication.getName() + " / "
                + correspondingApplication.getVersion());
        correspondingApplicationID = correspondingApplication.getId().getId();

        return correspondingApplicationID;
    }

    /**
     * We expand on the concept validation within CCI Not only do we run the
     * actual validation call, but we also perform the adjustments based on what
     * the validation has created.
     *
     * If components do not match, perform the necessary deletes/adds
     *
     * @param importedProject
     * @param summary
     * @return true if the Code Center BOM was changed
     * @throws CodeCenterImportException
     */
    private boolean validateAndSyncBom(CCIProject importedProject,
            CCIReportSummary summary) throws CodeCenterImportException {
        // Set up for the validation call
        Application app = importedProject.getApplication();
        String applicationName = app.getName();
        ApplicationIdToken appIdToken = app.getId();

        boolean ccBomChanged = false;

        // ReValidate mode is really
        // "we don't want to see any validation failures" mode.
        // In re-validate mode: we want to clear any old validation failures. So
        // if the
        // app has a non-PASSED validation status: force validation on it
        boolean forceValidation = configManager.isReValidateAfterBomChange()
                && (app.getValidationStatus() != ValidationStatusEnum.PASSED);

        // If user selected smart validate, then determine the last date of the
        // application
        try
        // validate
        {
            try
            // smart validate
            {
                if (configManager.isPerformSmartValidate() && !forceValidation) {
                    boolean before = smartValidate(importedProject, app, applicationName);
                    if (!before) {
                        summary.addToTotalValidatesSkipped();
                        return ccBomChanged;
                    }
                }
            } catch (Exception e) {
                log.error("Unexpected error during smart validation check:"
                        + e.getMessage());
                throw new Exception("Smart validation failed: "
                        + e.getMessage());
            }

            log.info(
                    "[{}] Attempting validation with Protex. This may take some time, depending on the number of components...",
                    applicationName);

            ccWrapper.getInternalApiWrapper().getApplicationApi()
                    .validate(appIdToken, false, false);
            reportSummary.addToTotalValidatesPerfomed();
            log.info("[{}] validation completed. ", applicationName);
        } catch (Exception sfe) {
            // reportSummary.addTotalValidationsFailed();
            // reportSummary.addToFailedValidationList(app.getName() + ":"
            // + app.getVersion());
            log.error("Unable to validate application {}", applicationName);
            throw new CodeCenterImportException("Error with validation:"
                    + sfe.getMessage(), sfe);
        }

        // ADD REQUESTS
        int requestsAdded = addRequestsToCodeCenter(app, summary);
        // DELETE REQUESTS
        int requestsDeleted = deleteRequestsFromCodeCenter(app, summary);

        if ((requestsAdded > 0) || (requestsDeleted > 0)) {
            ccBomChanged = true;
        }

        return ccBomChanged;
    }

    private boolean smartValidate(CCIProject importedProject, Application app, String applicationName) throws Exception {
        // get BOM refresh date from Protex
        Date lastRefreshDate = null;
        try {
            lastRefreshDate = protexWrapper
                    .getInternalApiWrapper()
                    .getBomApi()
                    .getLastBomRefreshFinishDate(
                            importedProject.getProjectKey());
        } catch (Exception e) {
            throw new Exception(
                    "Unable to get refresh date for project "
                            + importedProject.getProjectName(), e);
        }

        if (lastRefreshDate == null) {
            throw new Exception(
                    "The last BOM refresh date is null, cannot perfom smart validate");
        }

        // Grab the validation date
        Date lastValidatedTime = app.getLastValidationDate();
        log.info("Last validation date from app " + app.getName()
                + ": " + lastValidatedTime);

        // Compare the two dates, if the validate date happened
        // before
        // the last refresh
        // then proceed, otherwise get out.

        // We want to check before or equals, since identical dates
        // will return false on just a 'before' check.
        boolean before = (lastValidatedTime == null)
                || lastValidatedTime.before(lastRefreshDate)
                || lastValidatedTime.equals(lastRefreshDate);

        if (before) {
            log.info(
                    "[{}] Validation date {} is before refresh date {} proceeding with validation",
                    applicationName, lastValidatedTime,
                    lastRefreshDate.toString());
        } else {
            log.info(
                    "[{}] Validation date {} is after refresh date {}, skipping validation.",
                    applicationName, lastValidatedTime,
                    lastRefreshDate.toString());

        }
        return before;
    }

    /**
     * Gets the list of components after the last validate that exist in Protex,
     * but not in Code Center Then attempt to make requests out of them. Honor
     * user options to submit requests automatically.
     *
     * @param app
     * @param summary
     * @return Number of requests added to CC
     */
    private int addRequestsToCodeCenter(Application app,
            CCIReportSummary summary) {
        int requestsAdded = 0;
        String applicationName = app.getName();
        List<ProtexRequest> protexOnlyComponents = new ArrayList<ProtexRequest>();

        try {
            log.info("Fetching components to add");
            protexOnlyComponents = ccWrapper.getInternalApiWrapper()
                    .getApplicationApi()
                    .getProtexOnlyComponentsFromLastValidation(app.getId());

            // Keep track of success versus potentials
            summary.addTotalPotentialAdds(protexOnlyComponents.size());
        } catch (SdkFault e) {
            log.error("[{}] Error getting Protex components from validation:"
                    + e.getMessage(), applicationName, e);
        }

        // REQUESTS
        // Perform only if user wishes
        if (configManager.isPerformAdd()) {
            log.info("[{}] Attempting {} component requests...",
                    applicationName, protexOnlyComponents.size());

            List<RequestIdToken> newRequests = new ArrayList<RequestIdToken>();

            log.debug("User specified submit set to: "
                    + configManager.isSubmit());

            for (ProtexRequest protexRequest : protexOnlyComponents) {
                try {
                    try {
                        plugInManager.invokeComponentChangeInterceptorPreProcessAddMethod(protexRequest.getComponentId().getId());
                    } catch (CodeCenterImportException e) {
                        log.error("[{}] Error pre-processing add request: " + e.getMessage(),
                                applicationName, e);
                        summary.addTotalPlugInProcessingFailed();
                        summary.addToFailedPlugInProcessingList(app.getName() + ":" + app.getVersion());
                    }
                    RequestCreate requestCreate = new RequestCreate();

                    // Should this be requested
                    requestCreate.setSubmit(configManager.isSubmit());

                    RequestApplicationComponentToken token = new RequestApplicationComponentToken();
                    token.setApplicationId(app.getId());
                    token.setComponentId(protexRequest.getComponentId());

                    requestCreate.setApplicationComponentToken(token);
                    requestCreate.setLicenseId(protexRequest.getLicenseInfo().getId());
                    RequestIdToken newRequest = ccWrapper.getInternalApiWrapper()
                            .getRequestApi().createRequest(requestCreate);
                    newRequests.add(newRequest);

                    try {
                        plugInManager.invokeComponentChangeInterceptorPostProcessAddMethod(newRequest.getId(), protexRequest.getComponentId().getId());
                    } catch (CodeCenterImportException e) {
                        log.error("[{}] Error post-processing add request: " + e.getMessage(),
                                applicationName, e);
                        summary.addTotalPlugInProcessingFailed();
                        summary.addToFailedPlugInProcessingList(app.getName() + ":" + app.getVersion());
                    }

                    requestsAdded++;
                } catch (SdkFault e) {
                    log.error("[{}] Error creating request: " + e.getMessage(),
                            applicationName, e);
                }

            }
            // We want to keep track of successful requests.
            summary.addRequestsAdded(requestsAdded);
            log.info("[{}] completed adding {} requests", applicationName,
                    requestsAdded);
        } else {
            log.info("Add request option disabled");
        }
        return requestsAdded;
    }

    /**
     * Find the number of requests that no longer have any matches against
     * Protex. Delete those requests, honor user options.
     *
     * @param app
     * @param summary
     * @return Number of requests deleted from CC
     */
    private int deleteRequestsFromCodeCenter(Application app,
            CCIReportSummary summary) {
        int totalRequestsDeleted = 0;

        String applicationName = app.getName();
        List<RequestSummary> ccOnlyComps = new ArrayList<RequestSummary>();

        try {
            log.info("Fetching components to delete");
            ccOnlyComps = ccWrapper.getInternalApiWrapper().getApplicationApi()
                    .getCodeCenterOnlyComponentsFromLastValidation(app.getId());

            summary.addTotalPotentialDeletes(ccOnlyComps.size());
        } catch (SdkFault e) {
            log.error("[{}] Error getting CC only components from validation:"
                    + e.getMessage(), applicationName, e);
        }

        // Delete the components
        if (configManager.isPerformDelete()) {

            try {
                for (RequestSummary request : ccOnlyComps) {
                    try {
                        plugInManager.invokeComponentChangeInterceptorPreProcessDeleteMethod(request.getId().getId(), request.getComponentId().getId());
                    } catch (CodeCenterImportException e) {
                        log.error("[{}] Error pre-processing delete request: " + e.getMessage(),
                                applicationName, e);
                        summary.addTotalPlugInProcessingFailed();
                        summary.addToFailedPlugInProcessingList(app.getName() + ":" + app.getVersion());
                    }
                    ccWrapper.getInternalApiWrapper().getRequestApi()
                            .deleteRequest(request.getId());
                    totalRequestsDeleted++;
                }
            } catch (SdkFault e) {
                log.error("[{}] error deleting request", applicationName, e);
            }

            summary.addRequestsDeleted(totalRequestsDeleted);
            log.info("[{}] completed deleting {} requests", applicationName,
                    totalRequestsDeleted);

        } else {
            log.info("Delete requests option disabled");
        }
        return totalRequestsDeleted;
    }

    /**
     * @param project
     * @throws CodeCenterImportException
     */
    private CCIApplication createApplication(CCIProject project)
            throws CodeCenterImportException {
        boolean createNewApplication = false;

        // The object to return (either existing or new)
        CCIApplication cciApp = null;
        Application app = null;

        String applicationName = project.getProjectName();
        String version = project.getProjectVersion();

        ApplicationIdToken appIdToken = null;
        ApplicationNameVersionToken appNameVersionToken = null;

        // Set up the app name and version token
        appNameVersionToken = new ApplicationNameVersionToken();
        appNameVersionToken.setName(applicationName);
        appNameVersionToken.setVersion(version);

        try {
            // Check if Application exists
            app = ccWrapper.getInternalApiWrapper().getApplicationApi()
                    .getApplication(appNameVersionToken);
            log.info("[{}] Exists in Code Center.", applicationName);

            // wrap it in a CCIApplication, which tracks whether it's new or not
            return new CCIApplication(app, false);

        } catch (SdkFault e) {
            ErrorCode code = e.getFaultInfo().getErrorCode();
            if (code == ErrorCode.NO_APPLICATION_NAMEVERISON_FOUND) {
                createNewApplication = true;
                log.info(
                        "[{}] Does NOT exist in Code Center. Attempting to create it...",
                        applicationName);
            } else {
                log.info(
                        "[{}] Exception occurred when checking if application exists:{}",
                        applicationName, e.getMessage());
                throw new CodeCenterImportException(
                        "Error when getting Application:" + e.getMessage(), e);
            }
        }

        if (createNewApplication) {
            try {
                String workflowName = configManager.getWorkflow();
                String owner = configManager.getOwner();

                // Setup application to create it
                ApplicationCreate appCreate = new ApplicationCreate();
                appCreate.setName(applicationName);
                appCreate.setVersion(version);

                // This is the description that will show up in the main
                // application
                // view in Code Center.
                String description = CCIConstants.DESCRIPTION
                        + configManager.getVersion();
                appCreate.setDescription(description);
                WorkflowNameToken wf = new WorkflowNameToken();
                wf.setName(workflowName);
                appCreate.setWorkflowId(wf);
                UserNameToken ownerToken = new UserNameToken();
                ownerToken.setName(owner);
                appCreate.setOwnerId(ownerToken);
                RoleNameToken role = new RoleNameToken();
                role.setName("Application Administrator"); // TODO should be
                // configurable
                appCreate.setOwnerRoleId(role);

                // create Application
                appIdToken = ccWrapper.getInternalApiWrapper()
                        .getApplicationApi().createApplication(appCreate);

                // retrieve it
                app = ccWrapper.getInternalApiWrapper().getApplicationApi()
                        .getApplication(appIdToken);

                // wrap it in a CCIApplication, which tracks whether it's new or
                // not
                cciApp = new CCIApplication(app, true);
                log.info("...success!");

            } catch (SdkFault sdke) {
                throw new CodeCenterImportException(
                        "Creating Code Center application failed:"
                                + sdke.getMessage(), sdke);
            }
        }

        return cciApp;
    }

    /**
     * Attempts to associate Protex projec to CC application. Regardless of
     * outcome, then attempts to retrieve it.
     *
     * @param project
     * @param app
     * @throws CodeCenterImportException
     */
    private com.blackducksoftware.sdk.codecenter.application.data.Project associateApplicationToProtexProject(
            CCIProject cciProject, Application app)
            throws CodeCenterImportException {
        // Use this flag to determine whether we need to perform it
        // In the case where it exists, we can exit out.
        boolean performAssociation = false;

        String projectName = cciProject.getProjectName();
        String appVersion = cciProject.getProjectVersion();
        String ccProtexAliasName = configManager.getProtexServerName();

        // First attempt to retrieve it.
        com.blackducksoftware.sdk.codecenter.application.data.Project associatedProject = null;
        try {
            associatedProject = ccWrapper.getInternalApiWrapper()
                    .getApplicationApi()
                    .getAssociatedProtexProject(app.getId());

            log.info("[" + projectName + "] Application is already associated with project: " + associatedProject.getName());

            return associatedProject;
        } catch (SdkFault e) {
            ErrorCode code = e.getFaultInfo().getErrorCode();
            if (code == ErrorCode.APPLICATION_NOT_ASSOCIATED_WITH_PROTEX_PROJECT
                    || code == ErrorCode.NO_PROTEX_PROJECT_FOUND) {
                performAssociation = true;
            } else {
                throw new CodeCenterImportException(
                        "Retrieving Protex association failed:"
                                + e.getMessage(), e);
            }
        }

        // If there is no association and we had a "friendly" error message,
        // then create one.
        if (performAssociation) {
            try {
                log.info("Attempting Protex project association for: "
                        + projectName + " version: " + appVersion
                        + "; Protex Project ID: " + cciProject.getProjectKey());

                ProjectIdToken projectToken = new ProjectIdToken();
                projectToken.setId(cciProject.getProjectKey());

                ServerNameToken protexServerToken = new ServerNameToken();
                protexServerToken.setName(ccProtexAliasName);
                projectToken.setServerId(protexServerToken);

                ccWrapper.getInternalApiWrapper().getApplicationApi()
                        .associateProtexProject(app.getId(), projectToken);

                // Get it
                associatedProject = ccWrapper.getInternalApiWrapper()
                        .getApplicationApi()
                        .getAssociatedProtexProject(app.getId());

            } catch (SdkFault e) {
                if (e.getFaultInfo().getErrorCode() == ErrorCode.PROJECT_ALREADY_ASSOCIATED) {
                    throw new CodeCenterImportException(
                            "Protex project is already associated to a different application.  Please remove association: "
                                    + e.getMessage());
                } else {
                    throw new CodeCenterImportException(
                            "Associating Protex project failed:"
                                    + e.getMessage(), e);
                }
            }

            log.info("...success!");
        }
        return associatedProject;
    }

    /**
     * Get the summary of what happened during synchronization.
     *
     * @return
     */
    public CCIReportSummary getReportSummary() {
        return reportSummary;
    }

}
