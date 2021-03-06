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
import java.util.concurrent.Callable;

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
import com.blackducksoftware.sdk.codecenter.request.data.RequestSummary;
import com.blackducksoftware.sdk.codecenter.role.data.RoleNameToken;
import com.blackducksoftware.sdk.codecenter.user.data.UserNameToken;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportNamedException;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.config.CCIConstants;
import com.blackducksoftware.tools.ccimporter.model.CCIApplication;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public class SyncProjectTask implements Callable<CCIReportSummary> {
    private final Logger log = LoggerFactory.getLogger(this.getClass()
            .getName());

    private final CCIConfigurationManager configManager;

    private final CCIProject project;

    private final CCIReportSummary reportSummary = new CCIReportSummary();

    private final ICodeCenterServerWrapper ccWrapper;

    private final IProtexServerWrapper<ProtexProjectPojo> protexWrapper;

    private final PlugInManager plugInManager;

    public SyncProjectTask(CCIConfigurationManager config,
            ICodeCenterServerWrapper codeCenterWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexWrapper,
            PlugInManager plugInManager,
            CCIProject project) {
        configManager = config;
        ccWrapper = codeCenterWrapper;
        this.protexWrapper = protexWrapper;
        this.plugInManager = plugInManager;
        this.project = project;
    }

    /**
     * Imports the project (makes sure the app exists) and then syncs app BOM with Protex BOM.
     */
    @Override
    public CCIReportSummary call() throws CodeCenterImportNamedException {
        try {
            Date now = new Date();
            long startMilliseconds = now.getTime();
            log.info("Processing {}", project.getProjectName());

            boolean retryImport = false;
            int importRetryCount = 0;
            do {
                importRetryCount++;
                retryImport = false;
                boolean importSuccess = false;
                CCIProject importedProject = null;
                // Make sure app exists and is associated
                try {
                    importedProject = processImport(project);
                    importSuccess = true;
                } catch (Exception e) {
                    log.error(
                            "[{}] Unable to perform import: " + e.getMessage(),
                            project.getProjectName());
                }

                // Adjust app BOM so it equals project BOM
                if (importSuccess) {
                    retryImport = validate(project, importedProject,
                            importRetryCount);
                }
            } while (retryImport);
            now = new Date();
            long endMilliseconds = now.getTime();
            long duration = endMilliseconds - startMilliseconds;
            log.info("cc-import app import time (seconds): "
                    + Math.round(duration / 1000.0));

        } catch (Throwable t) {
            throw new CodeCenterImportNamedException(project.getProjectName(),
                    "Error processing project " + project.getProjectName()
                            + ": " + t.getMessage());
        }
        return reportSummary;
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
                cciApp = findOrCreateApplication(project);

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

                    log.info("[{}] LOOKUP SUCCESSFUL for project [{}]:[{}]!",
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

            // Initialize component change interceptor for this app
            plugInManager.invokeComponentChangeInterceptorInitForAppMethod(cciApp.getApp().getId().getId());

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
     * Make the application BOM equal the Project BOM, using validation and component adds/deletes.
     * Run the AppAdjuster, if any, if appropriate.
     *
     * @param project
     * @param importedProject
     * @param importRetryCount
     * @return
     */
    private boolean validate(CCIProject project, CCIProject importedProject,
            int importRetryCount) {
        boolean retryImport = false;
        boolean bomWasChanged = false;
        try {
            boolean performValidate = configManager.isValidate();
            if (performValidate) {
                bomWasChanged = processValidation(importedProject,
                        reportSummary);

                if (configManager.isReValidateAfterBomChange() && bomWasChanged) {
                    reValidate(importedProject);
                }
            }
            if (configManager.isAppAdjusterOnlyIfBomEdits()
                    && (bomWasChanged || importedProject.getCciApplication()
                            .isJustCreated())) {
                try {
                    plugInManager.invokeAppAdjuster(importedProject.getCciApplication(), project);
                } catch (CodeCenterImportException e) {
                    log.error("Application Adjuster failed, but proceeding with validation.");
                }
            }

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
     * @param project
     * @throws CodeCenterImportException
     */
    private CCIApplication findOrCreateApplication(CCIProject project)
            throws CodeCenterImportException {
        // boolean createNewApplication = false;

        // Look for app that corresponds to this project (same name)
        CCIApplication cciApp = findApplication(project);

        // If app not there, create it
        if (cciApp == null) {
            cciApp = createApplication(cciApp, project.getProjectName(), project.getProjectVersion());
        }

        return cciApp;
    }

    private CCIApplication findApplication(CCIProject project) throws CodeCenterImportException {

        // Set up the app name and version token
        ApplicationNameVersionToken appNameVersionToken = new ApplicationNameVersionToken();
        appNameVersionToken.setName(project.getProjectName());
        appNameVersionToken.setVersion(project.getProjectVersion());

        Application app = null;
        try {
            // Check if Application exists (it may or may not)
            app = ccWrapper.getInternalApiWrapper().getApplicationApi()
                    .getApplication(appNameVersionToken);
            log.info("[{}] Exists in Code Center.", project.getProjectName());

            // wrap it in a CCIApplication, which tracks whether it's new or not
            return new CCIApplication(app, false);

        } catch (SdkFault e) {
            ErrorCode code = e.getFaultInfo().getErrorCode();
            if (code == ErrorCode.NO_APPLICATION_NAMEVERISON_FOUND) {
                log.info(
                        "[{}] Does NOT exist in Code Center. Attempting to create it...",
                        project.getProjectName());
                return null; // App not there
            } else {
                log.info(
                        "[{}] Exception occurred when checking if application exists:{}",
                        project.getProjectName(), e.getMessage());
                throw new CodeCenterImportException(
                        "Error when getting Application:" + e.getMessage(), e);
            }
        }
    }

    private CCIApplication createApplication(CCIApplication cciApp, String applicationName, String version) throws CodeCenterImportException {
        Application app;
        ApplicationIdToken appIdToken;
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
            appIdToken = ccWrapper.getInternalApiWrapper().getApplicationApi()
                    .createApplication(appCreate);

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

        String projectName = cciProject.getProjectName();
        String appVersion = cciProject.getProjectVersion();
        String ccProtexAliasName = configManager.getProtexServerName();

        // First attempt to retrieve it.
        com.blackducksoftware.sdk.codecenter.application.data.Project associatedProject = getProjectAssociatedWithThisApp(app, projectName);

        // If there is no association then create one.
        if (associatedProject == null) {
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
                            "Protex project is currently associated to a different application.  Please remove association: "
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

    private com.blackducksoftware.sdk.codecenter.application.data.Project getProjectAssociatedWithThisApp(Application app, String projectName)
            throws CodeCenterImportException {
        com.blackducksoftware.sdk.codecenter.application.data.Project associatedProject = null;
        try {
            associatedProject = ccWrapper.getInternalApiWrapper()
                    .getApplicationApi()
                    .getAssociatedProtexProject(app.getId());

            log.info("[" + projectName + "] Application is already associated with project: " + associatedProject.getName());
            // if this app is already associated with this project, return the app
            if ((associatedProject != null) && associatedProject.getName().equals(projectName)) {
                return associatedProject;
            } else {
                // Break the old/bad association, and force a new one
                disassociateAppFromOldProject(project);
                return null; // force a new association
            }
        } catch (SdkFault e) {
            ErrorCode code = e.getFaultInfo().getErrorCode();
            if (code == ErrorCode.APPLICATION_NOT_ASSOCIATED_WITH_PROTEX_PROJECT
                    || code == ErrorCode.NO_PROTEX_PROJECT_FOUND) {
                return null; // there is no associated project
            } else {
                throw new CodeCenterImportException(
                        "Retrieving Protex association failed:"
                                + e.getMessage(), e);
            }
        }
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
    private boolean processValidation(CCIProject importedProject,
            CCIReportSummary summary) throws CodeCenterImportException {
        // Set up for the validation call
        Application app = importedProject.getApplication();
        String applicationName = app.getName();
        ApplicationIdToken appIdToken = app.getId();

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
                        summary.addToTotalValidatesSkipped();
                        return false;
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

        // Adjust the app's BOM
        boolean ccBomChanged = adjustCcBom(summary, app);

        return ccBomChanged;
    }

    /**
     * Adjust CcBom based on validation results.
     *
     * @param summary
     * @param app
     * @return true if BOM was changed, false otherwise
     */
    private boolean adjustCcBom(CCIReportSummary summary, Application app) {
        int requestsAdded = addRequestsToCodeCenter(app, summary);
        int requestsDeleted = deleteRequestsFromCodeCenter(app, summary);

        if ((requestsAdded > 0) || (requestsDeleted > 0)) {
            return true;
        }
        return false;
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

            // List<RequestIdToken> newRequests = new ArrayList<RequestIdToken>();

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

                    String newRequestId = ccWrapper.getRequestManager().createRequest(app.getId().getId(), protexRequest.getComponentId().getId(),
                            protexRequest.getLicenseInfo().getId().getId(), configManager.isSubmit());

                    requestsAdded++;

                    try {
                        plugInManager.invokeComponentChangeInterceptorPostProcessAddMethod(newRequestId, protexRequest.getComponentId().getId());
                    } catch (CodeCenterImportException e) {
                        log.error("[{}] Error post-processing add request: " + e.getMessage(),
                                applicationName, e);
                        summary.addTotalPlugInProcessingFailed();
                        summary.addToFailedPlugInProcessingList(app.getName() + ":" + app.getVersion());
                    }
                } catch (CommonFrameworkException e) {
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
                    ccWrapper.getRequestManager().deleteRequest(app.getId().getId(), request.getId().getId());
                    totalRequestsDeleted++;
                }
            } catch (CommonFrameworkException e) {
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
        log.info("Disassociating app " + project.getProjectName() + " from currently-associated Protex project");
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

}
