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

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.PropertyConfigurator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.protex.ProtexServerWrapper;

import com.blackducksoftware.sdk.codecenter.administration.data.ServerNameToken;
import com.blackducksoftware.sdk.codecenter.application.ApplicationApi;
import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationCreate;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ProjectNameToken;
import com.blackducksoftware.sdk.codecenter.application.data.ProtexRequest;
import com.blackducksoftware.sdk.codecenter.approval.data.WorkflowNameToken;
import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV6_6_0;
import com.blackducksoftware.sdk.codecenter.cola.ColaApi;
import com.blackducksoftware.sdk.codecenter.fault.ErrorCode;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.sdk.codecenter.request.RequestApi;
import com.blackducksoftware.sdk.codecenter.request.data.RequestApplicationComponentToken;
import com.blackducksoftware.sdk.codecenter.request.data.RequestCreate;
import com.blackducksoftware.sdk.codecenter.request.data.RequestIdToken;
import com.blackducksoftware.sdk.codecenter.user.data.RoleNameToken;
import com.blackducksoftware.sdk.codecenter.user.data.UserNameToken;
import com.blackducksoftware.sdk.protex.client.util.ProtexServerProxyV6_3;
import com.blackducksoftware.sdk.protex.project.Project;
import com.blackducksoftware.sdk.protex.project.ProjectApi;
import com.blackducksoftware.sdk.protex.project.ProjectColumn;
import com.blackducksoftware.sdk.protex.project.ProjectPageFilter;
import com.blackducksoftware.sdk.protex.util.PageFilterFactory;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.soleng.ccimporter.config.CCIConstants;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.StringConstants;
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
    
    private CodeCenterServerWrapper ccWrapper = null;
    private CCIConfigurationManager configManager = null;

    public CodeCenterProjectSynchronizer(
	    CodeCenterServerWrapper codeCenterWrapper,
	    CCIConfigurationManager config)
    {
	this.ccWrapper = codeCenterWrapper;
	this.configManager = config;
    }

    
    /**
     * Synchronizes a list of projects against the specified Code Center Configuration
     * 
     * In the event of multiple Protex servers, the alias is used to lookup the Protex configuration
     * Otherwise the user supplied option is utilized instead.
     * 
     * @param projectList
     * @throws CodeCenterImportException 
     */
    public void synchronize(List<CCIProject> projectList) throws CodeCenterImportException
    {
	for (CCIProject project : projectList)
	{
	    CCIProject importedProject = null;
	    try
	    {
		importedProject = processImport(project);

	    } catch (Exception e)
	    {
		throw new CodeCenterImportException("Unable to perform import",
			e);
	    }

	    try
	    {
		boolean performValidate = configManager.isValidate();
		if (performValidate)
		    processValidation(importedProject);
	    } catch (Exception e)
	    {
		throw new CodeCenterImportException(
			"Unable to perform validation", e);
	    }
	}
	
    }
    
    
    /**
     * An import consists of two steps. First creating and/or finding an
     * application within Code Center that matches Protex. Secondly to perform
     * the association between the CC and Protex server.
     * 
     * @param projectList
     * @return Returns the existing project with a populated Application bean.
     */
    private CCIProject processImport(CCIProject project)
    {
	try
	{

	    log.info("[{}] Attempting Protex project import. (version: {})",
		    project.getProjectName(), project.getProjectVersion());

	    // This will return an existing application, or create a new
	    // one. This is generic and not import specific.
	    Application app = createApplication(project);

	    // This takes the Code Center app and attempts to associate it
	    // with a Protex project. We do not need to return anything because
	    // the return object is useless to us. Any failure to obtain an
	    // object signifies an error and thus the failure of the import.
	    associateApplicationToProtexProject(project, app);

	    // If everything goes well, set the application name for
	    // potential validation down the road.
	    project.setApplication(app);

	    log.info("[{}] IMPORT SUCCESSFUL!", project.getProjectName());
	    log.info("-----------------------------");
	} catch (CodeCenterImportException ccie)
	{
	    if (ccie.getMessage().contains(
		    "already associated with Protex Project"))
	    {
		String text = ccie.getMessage().replace(
			"Associating Protex project failed:The Application ",
			"The Application [")
			+ "]";
		text = text.replace(
			" is already associated with Protex Project ",
			"] is already associated with Protex Project [");

		log.info(text);
		log.info("-----------------------------");
	    } else
	    {
		log.error("Skipping application import due to ERROR: {}",
			ccie.getMessage());
		log.info("[{}] IMPORT FAILED :-(", project.getProjectName());
		log.info("-----------------------------");
	    }
	}

	log.info("---- Done ----");

	return project;
    }

    private void processValidation(CCIProject importedProject)
	    throws CodeCenterImportException
    {
	Application app = importedProject.getApplication();
	String applicationName = app.getName();
	ApplicationIdToken appIdToken = app.getId();

	log.info(
		"[{}] Attempting validation with Protex. This may take some time, depending on the number of components...",
		applicationName);
	try
	{
	    ccWrapper.getInternalApiWrapper().applicationApi.validate(
		    appIdToken, false, false);
	} catch (SdkFault e)
	{
	    throw new CodeCenterImportException(
		    "Could not validate Application with Protex project:"
			    + e.getMessage(), e);
	} catch (Exception sfe)
	{
	    throw new CodeCenterImportException("Error with validation:"
		    + sfe.getMessage(), sfe);
	}

	log.info("...success!");

	List<ProtexRequest> protexOnlyComponents = new ArrayList<ProtexRequest>();

	try
	{
	    protexOnlyComponents = ccWrapper.getInternalApiWrapper().applicationApi
		    .getProtexOnlyComponentsFromLastValidation(appIdToken);
	} catch (SdkFault e)
	{
	    throw new CodeCenterImportException(
		    "Error getting Protex only components from validation:"
			    + e.getMessage(), e);
	}

	// REQUESTS
	log.info("[{}] Attempting {} component requests...", applicationName,
		protexOnlyComponents.size());

	List<RequestIdToken> newRequests = new ArrayList<RequestIdToken>();

	for (ProtexRequest protexRequest : protexOnlyComponents)
	{

	    try
	    {

		RequestCreate request = new RequestCreate();

		// Should this be requested
		log.info("User specified submit set to: " + configManager.isSubmit());
		request.setSubmit(configManager.isSubmit());

		RequestApplicationComponentToken token = new RequestApplicationComponentToken();
		token.setApplicationId(appIdToken);
		token.setComponentId(protexRequest.getComponentId());

		request.setApplicationComponentToken(token);

		newRequests.add(ccWrapper.getInternalApiWrapper().requestApi
			.createRequest(request));

	    } catch (SdkFault e)
	    {
		throw new CodeCenterImportException("Error creating request:"
			+ e.getMessage(), e);
	    }

	}
	log.info("...success!");
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
	
	if(createNewApplication)
	{
	    try
	    {
		String workflowName = configManager.getWorkflow();
		String owner = configManager.getOwner();

		// Setup application to create it
		ApplicationCreate appCreate = new ApplicationCreate();
		appCreate.setName(applicationName);
		appCreate.setVersion(version);
		
		// This is the description that will show up in the main application 
		// view in Code Center.
		String description = CCIConstants.DESCRIPTION + configManager.getVersion();
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
     * Attempts to associate Protex projec to CC application.  
     * Regardless of outcome, then attempts to retrieve it.  
     * 
     * @param project 
     * @param app
     * @throws CodeCenterImportException
     */
    private com.blackducksoftware.sdk.codecenter.application.data.Project associateApplicationToProtexProject(
	    CCIProject cciProject, Application app) throws CodeCenterImportException
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
	    if(code == ErrorCode.APPLICATION_NOT_ASSOCIATED_WITH_PROTEX_PROJECT  || code == ErrorCode.NO_PROTEX_PROJECT_FOUND)
	    {
		performAssociation = true;
	    }
	    else
	    {
		throw new CodeCenterImportException(
			"Retrieving Protex association failed:"
				+ e.getMessage(), e);
	    }
	}
	
	// If there is no association and we had a "friendly" error message, then create one.
	if(performAssociation)
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
        
        	    ccWrapper.getInternalApiWrapper().applicationApi.associateProtexProject(app.getId(), projectToken);
        	
        	    // Get it
        	    associatedProject = ccWrapper.getInternalApiWrapper().applicationApi
        		    .getAssociatedProtexProject(app.getId());
        
        
        	} catch (SdkFault e)
        	{
        	    if (e.getFaultInfo().getErrorCode() == ErrorCode.PROJECT_ALREADY_ASSOCIATED)
        	    {
        		log.info(
        			"[{}] Protex project is already associated to application.",
        			projectName);
        	    }  
        	    else
        	    {
        		throw new CodeCenterImportException(
        			"Associating Protex project failed:" + e.getMessage(),
        			e);
        	    }
        	}
        	    
        	log.info("...success!");
	}
	return associatedProject;
    }

}
