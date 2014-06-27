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
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.StringConstants;

public class CodeCenterProjectImporter {
	private static Logger log = LoggerFactory.getLogger(CodeCenterProjectImporter.class.getName());

	static ApplicationApi applicationApi = null;
	static RequestApi requestApi;
	static ColaApi colaApi;
	static ProjectApi projectApi;

	List<Project> projectsToImport;

	protected static final String CC_IMPORTER_VERSION = "0.7";

	protected static final String OPT_PROJECT_LIST = "-getProjectList";
	protected static final String OPT_IMPORT_LIST = "-importProjectList";
	protected static final String OPT_IMPORT_ALL = "-importAllProjects";

	/*
	 * Configuration values
	 */
	static String pServer = "";
	static String pUsername = "";
	static String pPassword = "";
	static String ccServer = "";
	static String ccUsername = "";
	static String ccPassword = "";
	static String protexServerName = "";
	static String appVersion = "";
	static String workflow = "";
	static String owner = "";
	static String submitString = "";
	static boolean submit = false;
	static String project = "";

	static String description = "Application created by the Code Center Project Importer v" + CC_IMPORTER_VERSION;

	/*
	 * API references
	 */
	static Long connectionTimeout = 120 * 1000L;
	static ProtexServerProxyV6_3 myProtexServer = null;
	static CodeCenterServerProxyV6_6_0 myCodeCenterServer = null;

	

	public List<Project> getProjectsToImport() {
		return projectsToImport;
	}

	public void setProjectsToImport(List<Project> projectsToImport) {
		this.projectsToImport = projectsToImport;
	}

	public static void initProtexApis() {
		if (myProtexServer == null)
			try {
				myProtexServer = new ProtexServerProxyV6_3(pServer, pUsername, pPassword, connectionTimeout);
			} catch (Exception e) {
				log.error("An error occurred while attemting to authenticate user with the Protex server\r\n{}", e.toString());
				System.exit(-1);
			}

		if (myProtexServer != null) {
			try {
				projectApi = myProtexServer.getProjectApi();
			} catch (Exception e) {
				log.error("Unable to register the Protex projectApi\r\n{}", e.toString());
				log.info("Exiting application...");
				System.exit(-1);
			}
		}
	}

	public static void initCodeCenterApis() {
		try {
			myCodeCenterServer = new CodeCenterServerProxyV6_6_0(ccServer, ccUsername, ccPassword, connectionTimeout);
		} catch (Exception e) {
			log.error("An error occurred while attemting to authenticate user with the Code Center server\r\n{}", e.toString());
			log.info("Exiting application...");
			System.exit(-1);
		}

		if (myCodeCenterServer != null) {
			try {
				applicationApi = myCodeCenterServer.getApplicationApi(0L);
			} catch (Exception e) {
				log.error("Unable to register the Code Center applicationApi\r\n{}", e.toString());
				log.info("Exiting application...");
				System.exit(-1);
			}

			try {
				colaApi = myCodeCenterServer.getColaApi(0L);
			} catch (Exception e) {
				log.error("Unable to register the Code Center colaApi\r\n{}", e.toString());
				log.info("Exiting application...");
				System.exit(-1);
			}

			try {
				requestApi = myCodeCenterServer.getRequestApi(0L);
			} catch (Exception e) {
				log.error("Unable to register the Code Center requestApi\r\n{}", e.toString());
				log.info("Exiting application...");
				System.exit(-1);
			}
		}
	}

	public void processImport(List<String> projectList) {

		for (String projectNameVersion : projectList) {
			String applicationName = null;
			String version = null;

			try {
				applicationName = projectNameVersion.split(",")[0].trim();
				try {
					version = projectNameVersion.split(",")[1].trim();
				} catch (ArrayIndexOutOfBoundsException aiobe) {
					version = appVersion;
				}

				log.info("[{}] Attempting Protex project import. (version: {})", applicationName, version);

				ApplicationIdToken appIdToken = null;
				ApplicationNameVersionToken appNameVersionToken = null;
				Application app = null;

				// Set up the app name and version token
				appNameVersionToken = new ApplicationNameVersionToken();
				appNameVersionToken.setName(applicationName);
				appNameVersionToken.setVersion(version);

				try {
					// Check if Application exists
					app = applicationApi.getApplication(appNameVersionToken);
					log.info("[{}] getApplication SDK call success.", applicationName);
					appIdToken = app.getId();
					log.info("[{}] Exists in Code Center.", applicationName);

				} catch (SdkFault e) {
					if (e.getMessage().contains("No Application found with")) {
						log.info("[{}] Does NOT exist in Code Center. Attempting to create it...", applicationName);
					} else {
						log.info("[{}] Exception occurred when checking if application exists:{}", applicationName, e.getMessage());
						throw new CodeCenterImportException("Error when getting Application:" + e.getMessage(), e);
					}

					try {

						// Setup application to create it
						ApplicationCreate appCreate = new ApplicationCreate();
						appCreate.setName(applicationName);
						appCreate.setVersion(version);
						appCreate.setDescription(description);
						WorkflowNameToken wf = new WorkflowNameToken();
						wf.setName(workflow);
						appCreate.setWorkflowId(wf);
						UserNameToken ownerToken = new UserNameToken();
						ownerToken.setName(owner);
						appCreate.setOwnerId(ownerToken);
						RoleNameToken role = new RoleNameToken();
						role.setName("Application Administrator");
						appCreate.setOwnerRoleId(role);

						// CREATE Application
						appIdToken = applicationApi.createApplication(appCreate);
						log.info("...success!");

					} catch (SdkFault sdke) {
						throw new CodeCenterImportException("Creating Code Center application failed:" + sdke.getMessage(), sdke);
					}
				}

				try {

					// PROTEX PROJECT ASSOCIATION
					log.info("Attempting Protex project association for: " + applicationName + " version: " + appVersion);
					ProjectNameToken projectToken = new ProjectNameToken();
					projectToken.setName(applicationName);
					ServerNameToken protexServerToken = new ServerNameToken();
					protexServerToken.setName(protexServerName);
					projectToken.setServerId(protexServerToken);
			
					applicationApi.associateProtexProject(appIdToken, projectToken);
					log.info("...success!");

				} catch (SdkFault e) {
					if (e.getMessage().endsWith("is already associated")) {
						log.info("[{}] Protex project is already associated to application.", applicationName);
					} else {
						throw new CodeCenterImportException("Associating Protex project failed:" + e.getMessage(), e);
					}
				} catch (Exception sfe) {
					throw new CodeCenterImportException("Associating Protex project failed. Does Protex project exist? " + sfe.getMessage(), sfe);
				}

				log.info("[{}] Retrieving Protex association...", applicationName);

				com.blackducksoftware.sdk.codecenter.application.data.Project project = null;
				try {
					project = applicationApi.getAssociatedProtexProject(appIdToken);
				} catch (SdkFault e1) {
					throw new CodeCenterImportException("Retrieving Protex association failed:" + e1.getMessage(), e1);
				}

				if (project == null) {
					throw new CodeCenterImportException("Associated Protex project is null. The Protex project may already be associated with another project.");
				} else {
					log.info("...success!");
				}

				// VALIDATION
				log.info("[{}] Attempting validation with Protex. This may take some time, depending on the number of components...", applicationName);
				try {
					applicationApi.validate(appIdToken, false, false);
				} catch (SdkFault e) {
					throw new CodeCenterImportException("Could not validate Application with Protex project:" + e.getMessage(), e);
				} catch (Exception sfe) {
					throw new CodeCenterImportException("Error with validation:" + sfe.getMessage(), sfe);
				}

				log.info("...success!");

				List<ProtexRequest> protexOnlyComponents = new ArrayList<ProtexRequest>();

				try {
					protexOnlyComponents = applicationApi.getProtexOnlyComponentsFromLastValidation(appIdToken);
				} catch (SdkFault e) {
					throw new CodeCenterImportException("Error getting Protex only components from validation:" + e.getMessage(), e);
				}

				// REQUESTS
				log.info("[{}] Attempting {} component requests...", applicationName, protexOnlyComponents.size());

				List<RequestIdToken> newRequests = new ArrayList<RequestIdToken>();

				for (ProtexRequest protexRequest : protexOnlyComponents) {

					try {

						RequestCreate request = new RequestCreate();

						// Should this be requested
						request.setSubmit(submit);

						RequestApplicationComponentToken token = new RequestApplicationComponentToken();
						token.setApplicationId(appIdToken);
						token.setComponentId(protexRequest.getComponentId());

						request.setApplicationComponentToken(token);

						newRequests.add(requestApi.createRequest(request));

					} catch (SdkFault e) {
						throw new CodeCenterImportException("Error creating request:" + e.getMessage(), e);
					}

				}
				log.info("...success!");

				log.info("[{}] IMPORT SUCCESSFUL!", applicationName);
				log.info("-----------------------------");
			} catch (CodeCenterImportException ccie) {
				if (ccie.getMessage().contains("already associated with Protex Project")) {
					String text = ccie.getMessage().replace("Associating Protex project failed:The Application ", "The Application [") + "]";
					text = text.replace(" is already associated with Protex Project ", "] is already associated with Protex Project [");

					log.info(text);
					log.info("-----------------------------");
				} else {
					log.error("Skipping application import due to ERROR: {}", ccie.getMessage());
					log.info("[{}] IMPORT FAILED :-(", applicationName);
					log.info("-----------------------------");
				}
			}
		}
		log.info("---- Done ----");

	}

	public List<Project> retrieveProjectList() {

		ProjectPageFilter pageFilter = PageFilterFactory.getAllRows(ProjectColumn.PROJECT_NAME);
		List<Project> projects = null;

		try {

			projects = projectApi.getProjects(pageFilter);

		} catch (com.blackducksoftware.sdk.fault.SdkFault e) {
			log.error("Error getting list of projects", e);
			System.exit(-1);
		}

		return projects;

	}

	public List<String> getProjectList(List<Project> projects) {
		List<String> projectNames = new ArrayList<String>();

		for (Project project : projects) {
			projectNames.add(project.getName().concat(", ".concat(appVersion)));
		}

		return projectNames;
	}

	public List<String> getProjectList(String projectList) {

		List<String> projectNames = new ArrayList<String>();

		for (String projectName : projectList.trim().split(";")) {
			if (!projectName.contains(",")) {
				projectName = projectName.concat(", ".concat(appVersion));
			}
			projectNames.add(projectName.trim());
		}

		log.info("Found {} projects to import into Code Center", projectNames.size());

		return projectNames;

	}

	public static boolean authenticate() {

		return true;
	}

	public Logger getLog() {
		return log;
	}

	public void setLog(Logger log) {
		CodeCenterProjectImporter.log = log;
	}

}
