/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.common.ProjectPojo;
import soleng.framework.standard.protex.ProtexServerWrapper;

import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;

public class CCIMultiServerProcessor extends CCIProcessor
{

    private static Logger log = LoggerFactory.getLogger(CCIMultiServerProcessor.class.getName());

    /**
     * @param configManager
     * @param multiserver
     * @throws Exception
     */
    public CCIMultiServerProcessor(CodeCenterConfigManager configManager)
	    throws Exception
    {
	super(configManager);
    }

    /**
     * For every established Protex server bean, perform a process which includes:
     * 1) Grab all the projects
     * 2) Create an association
     */
    @Override
    public void performImport()
    {
	// TODO Auto-generated method stub

	// For every server bean we have, create a ne
    }

    /**
     * Determines the correct project list There are several possibilities based
     * on user specified options.
     * 
     * @return
     * @throws Exception
     */
    private List<String> determineProjectList(ProtexServerWrapper protexWrapper) throws Exception
    {
	log.info("Getting Protex project list");
	List<String> projectList = new ArrayList<String>();

	List<String> userProjectList = codeCenterConfigManager.getProjectList();

	log.info("Importing all projects!");

	try
	{
	    List<ProjectPojo> projects = protexWrapper.getProjects();

	    // TODO: Return the actual pojo.
	    for (ProjectPojo pojo : projects)
	    {
		String projectName = pojo.getProjectName();
		if (codeCenterConfigManager.getAppVersion() != null)
		    projectName = projectName + ","
			    + codeCenterConfigManager.getAppVersion();

		log.info("Adding project: " + projectName);
		projectList.add(projectName);
	    }
	} catch (Exception e)
	{
	    throw new Exception("Unable to get all projects", e);
	}

	return projectList;
    }
}
