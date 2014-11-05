/**
 * Copyright (C)2011 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms set forth in the Black Duck App Exchange
 * Terms of Use located at:
 * http://www.blackducksoftware.com/legal/appexchange
 * IF YOU DO NOT AGREE TO THE THESE TERMS OR ANY SPECIAL TERMS, 
 * DO NOT ACCESS OR USE THIS SITE OR THE SOFTWARE.
 * 
 * @author Dave Meurer, dmeurer@blackducksoftware.com
 * 
 * Modified 2014
 * @author Niles Madison, namdison@blackducksoftware.com
 */
package com.blackducksoftware.soleng.ccimport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.sdk.codecenter.administration.data.ServerNameToken;
import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationCreate;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ProjectNameToken;
import com.blackducksoftware.sdk.codecenter.application.data.ProtexRequest;
import com.blackducksoftware.sdk.codecenter.approval.data.WorkflowNameToken;
import com.blackducksoftware.sdk.codecenter.fault.ErrorCode;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.sdk.codecenter.request.data.RequestApplicationComponentToken;
import com.blackducksoftware.sdk.codecenter.request.data.RequestCreate;
import com.blackducksoftware.sdk.codecenter.request.data.RequestIdToken;
import com.blackducksoftware.sdk.codecenter.request.data.RequestSummary;
import com.blackducksoftware.sdk.codecenter.user.data.RoleNameToken;
import com.blackducksoftware.sdk.codecenter.user.data.UserNameToken;
import com.blackducksoftware.soleng.ccimport.appadjuster.AppAdjuster;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimport.report.CCIReportSummary;
import com.blackducksoftware.soleng.ccimport.validate.CodeCenterValidator;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.soleng.ccimporter.config.CCIConstants;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

/**
 * Performs actual importation and/or validation.
 * 
 * @author Ari Kamen
 * @date Jul 1, 2014
 * 
 */
public class CodeCenterProjectSynchronizer
{
    private static Logger log = LoggerFactory
	    .getLogger(CodeCenterProjectSynchronizer.class.getName());

    private Object appAdjusterObject = null;
    private Method appAdjusterMethod = null; 	    // TODO: add mechanism to supply a custom adjuster
    private CodeCenterServerWrapper ccWrapper = null;
    private CCIConfigurationManager configManager = null;
    private CCIReportSummary reportSummary = new CCIReportSummary();

    // Keep track of project id/app id mappings
    private HashMap<String, String> projectAssociationMap  = null;
    
    private TimeZone tz = null; // User-provided: the timezone of the server
    
    public CodeCenterProjectSynchronizer(
	    CodeCenterServerWrapper codeCenterWrapper,
	    CCIConfigurationManager config) throws CodeCenterImportException
    {
	this.ccWrapper = codeCenterWrapper;
	this.configManager = config;
	setAppAdjusterMethod(config);
	setTimeZone(config);
    }
    
    private void setTimeZone(CCIConfigurationManager config) {
    	// TODO: Temporary workaround to provide timezones. This
	    // handles the case whereby
	    // utility is run against a server that is not in the same
	    // time zone.
	    String userSpecifiedTimeZone = configManager.getTimeZone();
	    
	    if (userSpecifiedTimeZone != null
		    && !userSpecifiedTimeZone.isEmpty())
	    {
		tz = TimeZone.getTimeZone(userSpecifiedTimeZone);
		log.info("User specified time zone recognized, using: "
			+ tz.getDisplayName());
	    } else
	    {
		tz = TimeZone.getDefault();
	    }
    }
    
    /**
     * If a custom app adjuster (to modify app metadata after sync) has been configured, initialize it.
     * @param config
     * @throws CodeCenterImportException
     */
    private void setAppAdjusterMethod(CCIConfigurationManager config) throws CodeCenterImportException {
    	
    	// See if the user has configured a custom app adjuster
    	String appAdjusterClassname = config.getAppAdjusterClassname();
    	if (appAdjusterClassname == null) {
    		return;  // No custom app adjuster has been configured
    	}
    	// Get the user-configured custom app adjuster class
    	Class sourceClass = null;
    	try {
    		sourceClass = Class.forName(appAdjusterClassname);
    	} catch (ClassNotFoundException e) {
    		String msg = "Unable to convert name to class for custom app adjuster: Class not found: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	}
    	
    	// Create an instance of the custom app adjuster class
    	try {
    		appAdjusterObject = sourceClass.newInstance();
    	} catch (IllegalAccessException e) {
    		String msg = "Unable to create instance of app adjuster: Illegal access: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	} catch (InstantiationException e) {
    		String msg = "Unable to create instance of app adjuster: Instantiation exception: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	}
    	
    	// Get the init method on the custom app adjuster class
    	Method initMethod=null;
    	Class[] initMethodArgTypes = { CCIConfigurationManager.class };
    	try {
    		initMethod = sourceClass.getDeclaredMethod("init", initMethodArgTypes);
    	} catch (NoSuchMethodException e) {
    		String msg = "Unable to get init method: No such method exception: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	}
    	
    	// Get the adjustApp method on the custom app adjuster class
    	Class[] adjustAppMethodArgTypes = { Application.class, CCIProject.class };
    	try {
    		appAdjusterMethod = sourceClass.getDeclaredMethod("adjustApp", adjustAppMethodArgTypes);
    	} catch (NoSuchMethodException e) {
    		String msg = "Unable to get app adjuster method: No such method exception: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	}
    	
    	// Call the init method to initialize the custom app adjuster
    	try {
    		initMethod.invoke(appAdjusterObject, configManager);
    	} catch (InvocationTargetException e) {
    		String msg = "Error initializing custom app adjuster: InvocationTargetException: " + e.getMessage();
    		throw new CodeCenterImportException(msg);
    	} catch (IllegalAccessException e) {
    		String msg = "Error initializing custom app adjuster: IllegalAccessException: " + e.getMessage();
    		throw new CodeCenterImportException(msg);
    	}
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
    public void synchronize(List<CCIProject> projectList)
    {
	reportSummary.setTotalProtexProjects(projectList.size());

	int counter = 1;
	for (CCIProject project : projectList)
	{
	    log.info("[{}/{}] Processing {}", counter++, projectList.size(),
		    project.getProjectName());
	    boolean importSuccess = false;
	    CCIProject importedProject = null;
	    try
	    {
		importedProject = processImport(project);
		importSuccess = true;
	    } catch (Exception e)
	    {
		log.error("[{}] Unable to perform import: " + e.getMessage(),
			project.getProjectName());
	    }

	    if (importSuccess)
	    {
		try
		{
		    boolean performValidate = configManager.isValidate();
		    if (performValidate)
			reportSummary = processValidation(importedProject,
				reportSummary);
		} catch (Exception e)
		{
		    log.error(
			    "[{}] Unable to perform validation: "
				    + e.getMessage(), project.getProjectName());
		}
	    }
	}

	// Here, we display the basic summary
	log.info(reportSummary.toString());
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
	    throws CodeCenterImportException
    {
	try
	{
	    log.info("[{}] Attempting Protex project import. (version: {})",
		    project.getProjectName(), project.getProjectVersion());

	    Application app = null;
	    String correspondingApplicationID = lookUpCorrespondingApplication(project);	  
		
	    // Creates the application (if needed) and then perform the association	 
	    if (correspondingApplicationID == null)
	    {
		    	log.info("No association found for project ID [{}], will attempt to create one", project.getProjectKey());
		    	// This will return an existing application, or create a new
			// one. This is generic and not import specific.
			app = createApplication(project);

			// This takes the Code Center app and attempts to associate it
			// with a Protex project. We do not need to return anything
			// because the return object is useless to us. Any failure to obtain an
			// object signifies an error and thus the failure of the import.
			associateApplicationToProtexProject(project, app);
			
			log.info("[{}] IMPORT SUCCESSFUL!", project.getProjectName());
			log.info("-----------------------------");
	    }
	    // Otherwise, use the information to perform the lookup
	    else
	    {
		log.info(
			"Found existing association of application ID [{}] for project ID [{}]",
			correspondingApplicationID, project.getProjectKey());
		ApplicationIdToken token = new ApplicationIdToken();
		token.setId(correspondingApplicationID);

		try
		{
		    app = ccWrapper.getInternalApiWrapper().applicationApi
			    .getApplication(token);

		    log.info(
			    "[{}] LOOKUP SUCCESSFUL for project [{}]:[{}]!",
			    project.getProjectName(), app.getName(),
			    app.getVersion());
		    log.info("-----------------------------");

		} catch (SdkFault e)
		{
		    throw new CodeCenterImportException(
			    "Unable to look up application using id: "
				    + correspondingApplicationID, e);
		}
	    }	
	   

	    invokeAppAdjuster(configManager, app, project);
	    
	    // If everything goes well, set the application name for
	    // potential validation down the road.
	    project.setApplication(app);
	    
	    
	    
	    
	} catch (CodeCenterImportException ccie)
	{
	    log.error("[{}] IMPORT FAILED, reason: [{}]", project.getProjectName(), ccie.getMessage());
	    reportSummary.addTotalImportsFailed();
	    reportSummary.addToFailedImportList(project.getProjectName());
	    throw new CodeCenterImportException(ccie.getMessage());
	}

	return project;
    }
    
    private void invokeAppAdjuster(CCIConfigurationManager configManager, Application app, CCIProject project) throws CodeCenterImportException {
    	if (appAdjusterMethod != null) {
	    	try {
	    		appAdjusterMethod.invoke(appAdjusterObject, app, project);
	    	} catch (InvocationTargetException e) {
	    		String msg = "Error during post-import application metadata adjustment: InvocationTargetException: " + e.getMessage();
	    		throw new CodeCenterImportException(msg);
	    	} catch (IllegalAccessException e) {
	    		String msg = "Error during post-import application metadata adjustment: IllegalAccessException: " + e.getMessage();
	    		throw new CodeCenterImportException(msg);
	    	}
	    }
    }

    /**
     * Perform a DB lookup, and determines based on Protex project ID 
     * the ID of the associated CC Application.
     * 
     * @param project
     * @return
     */
    private String lookUpCorrespondingApplication(CCIProject project)
    {
	String correspondingApplicationID = null;
	if (configManager.isIgnoreAssociations())
	{
	    log.info("**Ignore association mode.***");
	    // If the user has elected to ignore associations, then we will look
	    // up
	    // the DB to see if a project ID is associated with anything.
	    // If it is, then we use the app of that association.
	    if (projectAssociationMap == null)
	    {
		CodeCenterAssociationLookup cal = new CodeCenterAssociationLookup(
			configManager, ccWrapper);
		projectAssociationMap = cal.getAssociationMap();
	    }

	    correspondingApplicationID = projectAssociationMap.get(project
		    .getProjectKey());
	}

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
     * @return
     * @throws CodeCenterImportException
     */
    private CCIReportSummary processValidation(CCIProject importedProject,
	    CCIReportSummary summary) throws CodeCenterImportException
    {
	// VALIDATION CALL
	Application app = importedProject.getApplication();
	String applicationName = app.getName();
	ApplicationIdToken appIdToken = app.getId();

	// If user selected smart validate, then determine the last date of the
	// application
	try
	// validate
	{
	    try
	    // smart validate
	    {
		if (this.configManager.isPerformSmartValidate())
		{
		    // Our formatter
		    SimpleDateFormat formatter = new SimpleDateFormat(
			    "yyyy-MM-dd HH:mm:ss");
		    
		    CodeCenterValidator validator = new CodeCenterValidator(
			    this.configManager, this.ccWrapper, app,
			    importedProject);

		    // Grab the BOM Refresh date

		    Date lastRefreshDate = importedProject
			    .getLastBOMRefreshDate();

		    if (lastRefreshDate == null)
			throw new Exception(
				"The last BOM refresh date is null, cannot perfom smart validate");
		    
		    // Grab the validation date
		    String lastValidatedDateStr = validator
			    .getLastValidatedDate();
		    if (lastValidatedDateStr == null)
		    {
			log.warn("Could not determine last validate date for application: [{}] using BOM refresh date",applicationName);
			lastValidatedDateStr = formatter.format(lastRefreshDate);
		    }
		    
		    // Compare the two dates, if the validate date happened
		    // before
		    // the last refresh
		    // then proceed, otherwise get out.
		    

		    formatter.setTimeZone(tz);

		    Date lastValidatedTime = formatter
			    .parse(lastValidatedDateStr);

		    // We want to check before or equals, since identical dates will return false on just a 'before' check.
		    boolean before = lastValidatedTime.before(lastRefreshDate) || lastValidatedTime.equals(lastRefreshDate);

		    if (before)
		    {
			log.info(
				"[{}] Validation date {} is before refresh date {} proceeding with validation",
				applicationName, lastValidatedTime.toString(),
				lastRefreshDate.toString());
		    } else
		    {
			log.info(
				"[{}] Validation date {} is after refresh date {}, skipping validation.",
				applicationName, lastValidatedTime.toString(),
				lastRefreshDate.toString());
			summary.addToTotalValidatesSkipped();
			return summary;
		    }
		}
	    } catch (Exception e)
	    {
		log.error("Unexpected error during smart validation check:"
			+ e.getMessage());
		throw new Exception("Smart validation failed: "
			+ e.getMessage());
	    }

	    log.info(
		    "[{}] Attempting validation with Protex. This may take some time, depending on the number of components...",
		    applicationName);

	    ccWrapper.getInternalApiWrapper().applicationApi.validate(
		    appIdToken, false, false);
	    reportSummary.addToTotalValidatesPerfomed();
	    log.info("[{}] validation completed. ", applicationName);
	} catch (Exception sfe)
	{
	    reportSummary.addTotalValidationsFailed();
	    reportSummary.addToFailedValidationList(app.getName() + ":"
		    + app.getVersion());
	    log.error("Unable to validate application {}", applicationName);
	    throw new CodeCenterImportException("Error with validation:"
		    + sfe.getMessage(), sfe);
	}

	// ADD REQUESTS
	addRequestsToCodeCenter(app, summary);
	// DELETE REQUESTS
	deleteRequestsFromCodeCenter(app, summary);

	return summary;
    }

    /**
     * Gets the list of components after the last validate that exist in Protex,
     * but not in Code Center Then attempt to make requests out of them. Honor
     * user options to submit requests automatically.
     * 
     * @param app
     * @param summary
     */
    private void addRequestsToCodeCenter(Application app,
	    CCIReportSummary summary)
    {
	String applicationName = app.getName();
	List<ProtexRequest> protexOnlyComponents = new ArrayList<ProtexRequest>();

	try
	{
	    protexOnlyComponents = ccWrapper.getInternalApiWrapper().applicationApi
		    .getProtexOnlyComponentsFromLastValidation(app.getId());

	    // Keep track of success versus potentials
	    summary.addTotalPotentialAdds(protexOnlyComponents.size());
	} catch (SdkFault e)
	{
	    log.error("[{}] Error getting Protex components from validation:"
		    + e.getMessage(), applicationName, e);
	}

	// REQUESTS
	// Perform only if user wishes
	if (this.configManager.isPerformAdd())
	{
	    log.info("[{}] Attempting {} component requests...",
		    applicationName, protexOnlyComponents.size());

	    List<RequestIdToken> newRequests = new ArrayList<RequestIdToken>();

	    log.debug("User specified submit set to: "
		    + configManager.isSubmit());

	    int requestsAdded = 0;
	    for (ProtexRequest protexRequest : protexOnlyComponents)
	    {
		try
		{
		    RequestCreate request = new RequestCreate();

		    // Should this be requested
		    request.setSubmit(configManager.isSubmit());

		    RequestApplicationComponentToken token = new RequestApplicationComponentToken();
		    token.setApplicationId(app.getId());
		    token.setComponentId(protexRequest.getComponentId());

		    request.setApplicationComponentToken(token);

		    newRequests
			    .add(ccWrapper.getInternalApiWrapper().requestApi
				    .createRequest(request));

		    requestsAdded++;
		} catch (SdkFault e)
		{
		    log.error("[{}] Error creating request: " + e.getMessage(),
			    applicationName, e);
		}

	    }
	    // We want to keep track of successful requests.
	    summary.addRequestsAdded(requestsAdded);
	    log.info("[{}] completed adding requests", applicationName);
	} else
	{
	    log.info("Add request option disabled");
	}

    }

    /**
     * Find the number of requests that no longer have any matches against
     * Protex. Delete those requests, honor user options.
     * 
     * @param app
     * @param summary
     */
    private void deleteRequestsFromCodeCenter(Application app,
	    CCIReportSummary summary)
    {

	String applicationName = app.getName();
	List<RequestSummary> ccOnlyComps = new ArrayList<RequestSummary>();

	try
	{
	    ccOnlyComps = ccWrapper.getInternalApiWrapper().applicationApi
		    .getCodeCenterOnlyComponentsFromLastValidation(app.getId());

	    summary.addTotalPotentialDeletes(ccOnlyComps.size());
	} catch (SdkFault e)
	{
	    log.error("[{}] Error getting CC only components from validation:"
		    + e.getMessage(), applicationName, e);
	}

	// Delete the components
	if (this.configManager.isPerformDelete())
	{
	    int totalRequestsDeleted = 0;

	    try
	    {
		for (RequestSummary request : ccOnlyComps)
		{
		    ccWrapper.getInternalApiWrapper().requestApi
			    .deleteRequest(request.getId());
		    totalRequestsDeleted++;
		}
	    } catch (SdkFault e)
	    {
		log.error("[{}] error deleting request", applicationName, e);
	    }

	    summary.addRequestsDeleted(totalRequestsDeleted);
	    log.info("[{}] completed deleting all requests", applicationName);

	} else
	{
	    log.info("Delete requests option disabled");
	}

    }

    /**
     * @param project
     * @throws CodeCenterImportException
     */
    private Application createApplication(CCIProject project)
	    throws CodeCenterImportException
    {
	boolean createNewApplication = false;

	// The object to return (either existing or new)
	Application app = null;

	String applicationName = project.getProjectName();
	String version = project.getProjectVersion();

	ApplicationIdToken appIdToken = null;
	ApplicationNameVersionToken appNameVersionToken = null;

	// Set up the app name and version token
	appNameVersionToken = new ApplicationNameVersionToken();
	appNameVersionToken.setName(applicationName);
	appNameVersionToken.setVersion(version);

	try
	{
	    // Check if Application exists
	    app = ccWrapper.getInternalApiWrapper().applicationApi
		    .getApplication(appNameVersionToken);
	    log.info("[{}] Exists in Code Center.", applicationName);

	    return app;

	} catch (SdkFault e)
	{
	    ErrorCode code = e.getFaultInfo().getErrorCode();
	    if (code == ErrorCode.NO_APPLICATION_NAMEVERISON_FOUND)
	    {
		createNewApplication = true;
		log.info(
			"[{}] Does NOT exist in Code Center. Attempting to create it...",
			applicationName);
	    } else
	    {
		log.info(
			"[{}] Exception occurred when checking if application exists:{}",
			applicationName, e.getMessage());
		throw new CodeCenterImportException(
			"Error when getting Application:" + e.getMessage(), e);
	    }
	}

	if (createNewApplication)
	{
	    try
	    {
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
		role.setName("Application Administrator");
		appCreate.setOwnerRoleId(role);

		// create Application
		appIdToken = ccWrapper.getInternalApiWrapper().applicationApi
			.createApplication(appCreate);

		// retrieve it
		app = ccWrapper.getInternalApiWrapper().applicationApi
			.getApplication(appIdToken);

		log.info("...success!");

	    } catch (SdkFault sdke)
	    {
		throw new CodeCenterImportException(
			"Creating Code Center application failed:"
				+ sdke.getMessage(), sdke);
	    }
	}

	return app;
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
	    throws CodeCenterImportException
    {
	// Use this flag to determine whether we need to perform it
	// In the case where it exists, we can exit out.
	boolean performAssociation = false;

	String projectName = cciProject.getProjectName();
	String appVersion = cciProject.getProjectVersion();
	String ccProtexAliasName = configManager.getProtexServerName();

	// First attempt to retrieve it.
	com.blackducksoftware.sdk.codecenter.application.data.Project associatedProject = null;
	try
	{
	    associatedProject = ccWrapper.getInternalApiWrapper().applicationApi
		    .getAssociatedProtexProject(app.getId());

	    log.info("[{}] Application is already associated!", projectName);

	    return associatedProject;
	} catch (SdkFault e)
	{
	    ErrorCode code = e.getFaultInfo().getErrorCode();
	    if (code == ErrorCode.APPLICATION_NOT_ASSOCIATED_WITH_PROTEX_PROJECT
		    || code == ErrorCode.NO_PROTEX_PROJECT_FOUND)
	    {
		performAssociation = true;
	    } else
	    {
		throw new CodeCenterImportException(
			"Retrieving Protex association failed:"
				+ e.getMessage(), e);
	    }
	}

	// If there is no association and we had a "friendly" error message,
	// then create one.
	if (performAssociation)
	{
	    try
	    {
		log.info("Attempting Protex project association for: "
			+ projectName + " version: " + appVersion);

		ProjectNameToken projectToken = new ProjectNameToken();
		projectToken.setName(projectName);

		ServerNameToken protexServerToken = new ServerNameToken();
		protexServerToken.setName(ccProtexAliasName);
		projectToken.setServerId(protexServerToken);

		ccWrapper.getInternalApiWrapper().applicationApi
			.associateProtexProject(app.getId(), projectToken);

		// Get it
		associatedProject = ccWrapper.getInternalApiWrapper().applicationApi
			.getAssociatedProtexProject(app.getId());

	    } catch (SdkFault e)
	    {
		if (e.getFaultInfo().getErrorCode() == ErrorCode.PROJECT_ALREADY_ASSOCIATED)
		{
		    throw new CodeCenterImportException(
			    "Protex project is already associated to a different application.  Please remove association: "
				    + e.getMessage());
		} else
		{
		    throw new CodeCenterImportException(
			    "Associating Protex project failed:"
				    + e.getMessage(), e);
		}
	    }

	    log.info("...success!");
	}
	return associatedProject;
    }

    public CCIReportSummary getReportSummary()
    {
	return this.reportSummary;
    }
    
}
