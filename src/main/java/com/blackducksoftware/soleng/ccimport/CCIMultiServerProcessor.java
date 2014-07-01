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
    public void performSynchronize()
    {
	// TODO Auto-generated method stub

	// For every server bean we have, create a new protexWrapper
	// Grab the projects and process import
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

	getProjects(protexWrapper);
	
	return projectList;
    }
}
