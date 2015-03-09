/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.soleng.ccimporter.config;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

import soleng.framework.core.config.ConfigurationManager;
import soleng.framework.core.config.ConfigConstants.APPLICATION;
import soleng.framework.core.config.server.ServerBean;

/**
 * @author Ari Kamen
 * @date Jun 27, 2014
 * 
 */
public class CCIConfigurationManager extends ConfigurationManager {
	private final static Logger log = LoggerFactory
			.getLogger(CCIConfigurationManager.class.getName());

	private String[] cmdLineArgs = null;
	private String protexServerName = "";
	private String appVersion = "";
	private String workflow = "";
	private String owner = "";
	private Boolean submit = false;
	private Boolean validate = false;
	private Boolean performDelete = false;
	private Boolean performAdd = true;
	private Boolean performSmartValidate = false;
	private String version = "undefined";
	private Boolean ignoreAssociations = false;
	private boolean attemptToFixInvalidAssociation = false;
	private String appAdjusterClassname = null;
	private boolean reValidateAfterBomChange = false;
	private boolean appAdjusterOnlyIfBomEdits = false;
	private Pattern protexProjectNameFilterPattern = null;
	private int numThreads = 1;
	

	// TODO: Temporary workarounds for DB access
	private String hostName = null;
	private String timeZone = null;
	private String dbString = null;

	private Boolean runReport = false;

	private List<CCIProject> projectList = new ArrayList<CCIProject>();

	public CCIConfigurationManager() {
		// for command line
		super();
	}

	public CCIConfigurationManager(String fileLocation, APPLICATION appType) {
		super(fileLocation, appType);
	}
	
	public CCIConfigurationManager(Properties props, APPLICATION appType) {
		super(props, appType);
	}

	protected void initConfigFile() {
		try {
			String dynamicVersion = getClass().getPackage()
					.getImplementationVersion();
			if (dynamicVersion != null)
				version = dynamicVersion;
		} catch (Throwable t) {
			log.debug("Could not determine version", t);
		}

		List<ServerBean> servers = super.getServerList();

		/**
		 * We use server list to determine whether certain properties are
		 * required or optional.
		 */
		if (servers.size() == 0) {
			// This is only needed in single server mode
			protexServerName = getProperty(CCIConstants.PROTEX_NAME_PROPERTY);

		}

		owner = getProperty(CCIConstants.OWNER_PROPERTY);
		appVersion = getProperty(CCIConstants.VERSION_PROPERTY);
		workflow = getProperty(CCIConstants.WORKFLOW_PROPERTY);
		appAdjusterClassname = getOptionalProperty(CCIConstants.APP_ADJUSTER_CLASSNAME_PROPERTY);

		submit = getOptionalProperty(CCIConstants.SUBMIT_PROPERTY, false,
				Boolean.class);
		validate = getOptionalProperty(
				CCIConstants.VALIDATE_APPLICATION_PROPERTY, false,
				Boolean.class);
		performSmartValidate = getOptionalProperty(
				CCIConstants.VALIDATE_SMART_APPLICATION_PROPERTY, false,
				Boolean.class);
		performAdd = getOptionalProperty(CCIConstants.SUBMIT_PROPERTY, true,
				Boolean.class);
		performDelete = getOptionalProperty(CCIConstants.DELETE_REQUESTS,
				false, Boolean.class);

		ignoreAssociations = getOptionalProperty(
				CCIConstants.IGNORE_ASSOCIATIONS, false, Boolean.class);

		runReport = getOptionalProperty(CCIConstants.RUN_REPORT_PROPERTY,
				false, Boolean.class);

		// TODO: Temporary workaround for the DB access
		this.hostName = getOptionalProperty(CCIConstants.VALIDATE_SMART_HOST_NAME_PROPERTY);
		this.timeZone = getOptionalProperty(CCIConstants.VALIDATE_SMART_TIMEZONE_PROPERTY);
		this.dbString = getOptionalProperty(CCIConstants.VALIDATE_SMART_DB_PROPERTY);
		
		String attemptToFixInvalidAssociationString = getOptionalProperty(CCIConstants.ATTEMPT_TO_FIX_INVALID_ASSOCIATION_PROPERTY);
		if ("true".equalsIgnoreCase(attemptToFixInvalidAssociationString)) {
			attemptToFixInvalidAssociation = true;
		}

		/**
		 * Parse through the user specified list.
		 */
		String potentiaList = getOptionalProperty(CCIConstants.PROJECT_PROPERTY);
		setProjectList(potentiaList);
		
		String reValidateString = super.getOptionalProperty(CCIConstants.RE_VALIDATE_AFTER_CHANGING_BOM_PROPERTY);
		if ("true".equalsIgnoreCase(reValidateString)) {
			this.reValidateAfterBomChange = true;
		}
		
		String appAdjusterOnlyIfBomEditsString = super.getOptionalProperty(CCIConstants.APP_ADJUSTER_ONLY_IF_BOM_EDITS_PROPERTY);
		if ("true".equalsIgnoreCase(appAdjusterOnlyIfBomEditsString)) {
			this.appAdjusterOnlyIfBomEdits = true;
		}
		
		String protexProjectNameFilterString = super.getOptionalProperty(CCIConstants.PROJECT_FILTER_PROPERTY);
		if (protexProjectNameFilterString != null) {
			this.protexProjectNameFilterPattern = Pattern.compile(protexProjectNameFilterString);
		}
		
		String numThreadsString = super.getOptionalProperty(CCIConstants.NUM_THREADS_PROPERTY);
		if (numThreadsString != null) {
			this.numThreads = Integer.parseInt(numThreadsString);
		}
	}

	/**
	 * Set project list from a comma separated list of project names. 
	 * Any project can be followed by a semicolon plus the version:
	 * "project1;version1,project2,project3"
	 * @param projectListString
	 */
	public void setProjectList(String projectListString) {
		if (projectListString == null) {
			projectList = new ArrayList<CCIProject>(0);
		} else {
			projectList = buildProjectList(Arrays.asList(StringUtils.split(
				projectListString, ",")));
		}
	}

	/**
	 * Builds a user specified list of projects. At the very least, this will
	 * contain a list of project objects with names In some cases it will
	 * contain a version too
	 * 
	 * @param asList
	 * @return
	 */
	private List<CCIProject> buildProjectList(List<String> asList) {
		List<CCIProject> projectList = new ArrayList<CCIProject>();

		for (String userSuppliedProjectRaw : asList) {
			String userSuppliedProject = userSuppliedProjectRaw.trim();
			CCIProject project = new CCIProject();
			// First we attempt to tokenize the user supplied list to check for
			// versions
			String[] projAndVersion = userSuppliedProject.split(";");
			if (projAndVersion.length == 1) {
				// Just project
				// Set the name and the user specified version
				project.setProjectName(projAndVersion[0]);
				project.setProjectVersion(getAppVersion());

			} else if (projAndVersion.length == 2) {
				// Project and version
				project.setProjectName(projAndVersion[0].trim());
				project.setProjectVersion(projAndVersion[1].trim());
			} else {
				// No idea, say something.
				log.warn(
						"User specified element '{}' not understood, expecting comma separated <project name>;<version",
						userSuppliedProject);
			}
			projectList.add(project);
		}

		return projectList;
	}

	protected void initCommandLine(String[] args, APPLICATION type) {
		this.cmdLineArgs = args.clone();
		String pServer = null;
		String pUsername = null;
		String pPassword = null;
		String ccServer = null;
		String ccUsername = null;
		String ccPassword = null;

		for (int a = 0; a < args.length; a++) {
			if (args[a].equals(StringConstants.PROTEX_SERVER))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					pServer = args[a + 1];
			if (args[a].equals(StringConstants.PROTEX_USERNAME))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					pUsername = args[a + 1];
			if (args[a].equals(StringConstants.PROTEX_PASSWORD))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					pPassword = args[a + 1];
			if (args[a].equals(StringConstants.CODE_CENTER_SERVER))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					ccServer = args[a + 1];
			if (args[a].equals(StringConstants.CODE_CENTER_USERNAME))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					ccUsername = args[a + 1];
			if (args[a].equals(StringConstants.CODE_CENTER_PASSWORD))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					ccPassword = args[a + 1];

			if (args[a].equals(StringConstants.PROTEX_NAME))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					protexServerName = args[a + 1];
			if (args[a].equals(StringConstants.DEFAULT_APP_VERSION))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					appVersion = args[a + 1];
			if (args[a].equals(StringConstants.WORKFLOW))
				if (args.length > a + 1 && !args[a + 1].contains("--"))
					workflow = args[a + 1];
			if (args[a].equals(StringConstants.OWNER))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					owner = args[a + 1];
			if (args[a].equals(StringConstants.SUBMIT))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					submit = new Boolean(args[a + 1]);
			if (args[a].equals(StringConstants.PROJECT))
				if (args.length > a + 1 && !args[a + 1].startsWith("-"))
					projectList = buildProjectList(Arrays.asList(StringUtils
							.split(args[a + 1])));
		}

		boolean valid = true;

		if (protexServerName.isEmpty()) {
			log.info("Missing protex server name configuration");
			valid = false;
		}
		if (appVersion.isEmpty()) {
			log.info("Missing default application version configuration");
			valid = false;
		}
		if (workflow.isEmpty()) {
			log.info("Missing workflow configuration");
			valid = false;
		}
		if (owner.isEmpty()) {
			log.info("Missing application owner configuration");
			valid = false;
		}

		if (!valid) {
			log.error("Missing configuration details.");
			usage();
			System.exit(-1);
		}

		/**
		 * Set up the server beans from the user specified configuration
		 */
		ServerBean protexBean = new ServerBean(pServer, pUsername, pPassword,
				APPLICATION.PROTEX);
		ServerBean ccServerBean = new ServerBean(ccServer, ccUsername,
				ccPassword, APPLICATION.CODECENTER);

		/**
		 * We are going to set that bean which is of interest to us.
		 * 
		 */
		if (type == APPLICATION.PROTEX)
			super.addServerBean(protexBean);
		else if (type == APPLICATION.CODECENTER)
			super.addServerBean(ccServerBean);
	}

	/**
	 * Name of the server as specified in the CC admin
	 * 
	 * @return
	 */
	public String getProtexServerName() {
		return protexServerName;
	}

	/**
	 * This is the name of the Protex configuration within Code Center
	 * 
	 * @param protexAlias
	 */
	public void setProtexServerName(String protexAlias) {
		protexServerName = protexAlias;
	}

	public void setOwner(String ownerName) {
		owner = ownerName;
	}

	public String getAppVersion() {
		return appVersion;
	}

	public String getWorkflow() {
		return workflow;
	}

	public String getOwner() {
		return owner;
	}

	public Boolean isSubmit() {
		return submit;
	}

	public Boolean isValidate() {
		return validate;
	}

	public void setValidate(Boolean validate) {
		this.validate = validate;
	}


	public boolean isAttemptToFixInvalidAssociation() {
		return attemptToFixInvalidAssociation;
	}

	public List<CCIProject> getProjectList() {
		return projectList;
	}

	public String[] getCmdLineArgs() {
		return cmdLineArgs;
	}

	public void setCmdLineArgs(String[] cmdLineArgs) {
		this.cmdLineArgs = cmdLineArgs;
	}

	public static void usage() {
		System.out.println();
		System.out.println("USAGE:");
		System.out.println(StringConstants.PROTEX_SERVER
				+ ": Protex server to import project(s) from.");
		System.out.println(StringConstants.PROTEX_USERNAME
				+ ": Protex username.");
		System.out.println(StringConstants.PROTEX_PASSWORD
				+ ": Protex password.");
		System.out.println(StringConstants.CODE_CENTER_SERVER
				+ ": Code Center server to import project(s) to.");
		System.out.println(StringConstants.CODE_CENTER_USERNAME
				+ ": Code Center username.");
		System.out.println(StringConstants.CODE_CENTER_PASSWORD
				+ ": Code Center password.");
		System.out
				.println(StringConstants.PROTEX_NAME
						+ ": The Protex server name specified in the Code Center administration configuration.");
		System.out
				.println(StringConstants.DEFAULT_APP_VERSION
						+ ": Default application version for the application in Code Center.");
		System.out.println(StringConstants.WORKFLOW
				+ ": Default workflow for the application in Code Center.");
		System.out.println(StringConstants.OWNER
				+ ": Default application owner in Code Center.");
		System.out.println(StringConstants.SUBMIT + ": Submit request option.");
		System.out.println(StringConstants.VALIDATE + ": Validate option.");
		System.out
				.println(StringConstants.PROJECT
						+ ": Project list, example:  projectName;version,project2;version2 (Leave blank for ALL projects)");
	}

	/**
	 * Version of the current application (derived from pom.xml)
	 * 
	 * @return
	 */
	public String getVersion() {
		return version;
	}

	public Boolean isRunReport() {
		return runReport;
	}

	public void setRunReport(Boolean runReport) {
		this.runReport = runReport;
	}

	public Boolean isPerformDelete() {
		return performDelete;
	}

	public void setPerformDelete(Boolean performDelete) {
		this.performDelete = performDelete;
	}

	public Boolean isPerformAdd() {
		return performAdd;
	}

	public void setPerformAdd(Boolean performSubmit) {
		this.performAdd = performSubmit;
	}

	public Boolean isPerformSmartValidate() {
		return performSmartValidate;
	}

	public void setPerformSmartValidate(Boolean performSmartValidate) {
		this.performSmartValidate = performSmartValidate;
	}

	public String getHostName() {
		return hostName;
	}

	public void setHostName(String hostName) {
		this.hostName = hostName;
	}

	public String getTimeZone() {
		return timeZone;
	}

	public void setTimeZone(String timeZone) {
		this.timeZone = timeZone;
	}

	public String getDbString() {
		return dbString;
	}

	public void setDbString(String dbString) {
		this.dbString = dbString;
	}

	public Boolean isIgnoreAssociations() {
		return ignoreAssociations;
	}

	public void setIgnoreAssociations(Boolean ignoreAssociations) {
		this.ignoreAssociations = ignoreAssociations;
	}

	public String getAppAdjusterClassname() {
		return appAdjusterClassname;
	}

	public boolean isReValidateAfterBomChange() {
		return reValidateAfterBomChange;
	}

	public boolean isAppAdjusterOnlyIfBomEdits() {
		return appAdjusterOnlyIfBomEdits;
	}

	public Pattern getProtexProjectNameFilterPattern() {
		return protexProjectNameFilterPattern;
	}

	public int getNumThreads() {
		return numThreads;
	}
}
