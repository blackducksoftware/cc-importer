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

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.core.config.ConfigurationManager;
import soleng.framework.core.config.ConfigConstants.APPLICATION;
import soleng.framework.core.config.server.ServerBean;

/**
 * @author Ari Kamen
 * @date Jun 27, 2014
 * 
 */
public class CCIConfigurationManager extends ConfigurationManager
{
    private final static Logger log = LoggerFactory
	    .getLogger(CCIConfigurationManager.class.getName());

    private String protexServerName = "";
    private String appVersion = "";
    private String workflow = "";
    private String owner = "";
    private Boolean submit = false;
    private List<String> projectList = new ArrayList<String>();

    public CCIConfigurationManager()
    {
	// for command line
	super();
    }
    
    public CCIConfigurationManager(String fileLocation, APPLICATION appType)
    {
	super(fileLocation, appType);
    }
    
    protected void initConfigFile()
    {

	protexServerName = getProperty(CCIConstants.PROTEX_PROPERTY);
	appVersion = getProperty(CCIConstants.VERSION_PROPERTY);
	workflow = getProperty(CCIConstants.WORKFLOW_PROPERTY);
	owner = getProperty(CCIConstants.OWNER_PROPERTY);
	submit = getOptionalProperty(CCIConstants.SUBMIT_PROPERTY, false,
		Boolean.class);

	String potentiaList = getOptionalProperty(CCIConstants.PROJECT_PROPERTY);
	projectList = Arrays.asList(StringUtils.split(potentiaList, ","));

    }

    protected void initCommandLine(String[] args)
    {

	String projectOption = null;
	for (int a = 0; a < args.length; a++)
	{

	    String pServer = null;
	    String pUsername = null;
	    String pPassword = null;

	    if (args[a].equals(StringConstants.PROTEX_SERVER))
		if (args.length > a + 1 && !args[a + 1].startsWith("-"))
		    pServer = args[a + 1];
	    if (args[a].equals(StringConstants.PROTEX_USERNAME))
		if (args.length > a + 1 && !args[a + 1].startsWith("-"))
		    pUsername = args[a + 1];
	    if (args[a].equals(StringConstants.PROTEX_USERNAME))
		if (args.length > a + 1 && !args[a + 1].startsWith("-"))
		    pPassword = args[a + 1];

	    String ccServer = null;
	    String ccUsername = null;
	    String ccPassword = null;

	    if (args[a].equals(StringConstants.CODE_CENTER_SERVER))
		if (args.length > a + 1 && !args[a + 1].startsWith("-"))
		    ccServer = args[a + 1];
	    if (args[a].equals(StringConstants.CODE_CENTER_USERNAME))
		if (args.length > a + 1 && !args[a + 1].startsWith("-"))
		    ccUsername = args[a + 1];
	    if (args[a].equals(StringConstants.CODE_CENTER_PASSWORD))
		if (args.length > a + 1 && !args[a + 1].startsWith("-"))
		    ccPassword = args[a + 1];

	    ServerBean protexBean = new ServerBean(pServer, pUsername,
		    pPassword, APPLICATION.PROTEX);
	    ServerBean ccServerBean = new ServerBean(ccServer, ccUsername,
		    ccPassword, APPLICATION.CODECENTER);

	    /**
	     * We are going to add two server beans in the case of command line
	     * 
	     */
	    super.addServerBean(protexBean);
	    super.addServerBean(ccServerBean);

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
		    projectList = Arrays.asList(StringUtils.split(args[a + 1]));
	}

	if (projectOption == null)
	{
	    log.error("Missing project import options");
	    usage();
	    System.exit(-1);
	}

	boolean valid = true;

	if (protexServerName.isEmpty())
	{
	    log.info("Missing protex server name configuration");
	    valid = false;
	}
	if (appVersion.isEmpty())
	{
	    log.info("Missing default application version configuration");
	    valid = false;
	}
	if (workflow.isEmpty())
	{
	    log.info("Missing workflow configuration");
	    valid = false;
	}
	if (owner.isEmpty())
	{
	    log.info("Missing application owner configuration");
	    valid = false;
	}

	if (projectList.isEmpty())
	{
	    log.info("Missing project configuration");
	    valid = false;
	}

	if (!valid)
	{
	    log.error("Missing configuration details.");
	    usage();
	    System.exit(-1);
	}
    }

    /**
     * Name of the server as specified in the CC admin
     * 
     * @return
     */
    public String getProtexServerName()
    {
	return protexServerName;
    }

    public String getAppVersion()
    {
	return appVersion;
    }

    public String getWorkflow()
    {
	return workflow;
    }

    public String getOwner()
    {
	return owner;
    }

    public Boolean getSubmit()
    {
	return submit;
    }

    public List<String> getProjectList()
    {
	return projectList;
    }

    public static void usage()
    {
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
	System.out.println(StringConstants.PROJECT + ": Project list.");
    }
}
