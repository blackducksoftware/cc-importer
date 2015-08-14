/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.tools.ccimport;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.report.CCIReportGenerator;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.ccimporter.model.CCIProjectList;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;

/**
 * Common functionality shared between the single and multi processors.
 * 
 * @author Ari Kamen
 * @date Jun 27, 2014
 * 
 */
public abstract class CCIProcessor
{
    private static Logger log = LoggerFactory.getLogger(CCIProcessor.class
	    .getName());

    protected CodeCenterConfigManager codeCenterConfigManager = null;
    protected CodeCenterServerWrapper codeCenterWrapper = null;


    protected List<CCIReportSummary> reportSummaryList = new ArrayList<CCIReportSummary>();
    
    
    /**
     * Establish the Code Center connection.
     * 
     * @param configManager
     * @throws Exception
     */
    public CCIProcessor(CodeCenterConfigManager configManager, CodeCenterServerWrapper codeCenterServerWrapper) throws Exception
    {
	this.codeCenterConfigManager = configManager;
	this.codeCenterWrapper = codeCenterServerWrapper;
    }

    /**
     * Performs a synchronization between Protex and Code Center
     * This will always include an import step (application verification and association)
     * This may, based on user preference, also include a validation step.
     * @throws CodeCenterImportException 
     */
    public abstract void performSynchronize() throws CodeCenterImportException;

    /**
     * Generates a report denoting which application/project is "out of sync"
     * @throws CodeCenterImportException
     */
	public abstract void runReport() throws CodeCenterImportException;
	
	/**
	 * Used by unit tests to get the report generator (for testing contents of report).
	 */
	public abstract CCIReportGenerator getReportGen();
	
    /**
     * Returns a list of projects based on either: - Supplied user list - All
     * Projects belonging to that user (assuming supplied user list is empty)
     * 
     * @param protexWrapper
     * @return
     * @throws Exception
     */
	/**
     * Returns a list of projects based on either: - Supplied user list - All
     * Projects belonging to that user (assuming supplied user list is empty)
     * 
     * @param protexWrapper
     * @return
     * @throws Exception
     */
	public CCIProjectList getProjects(ProtexServerWrapper protexWrapper)
		    throws CodeCenterImportException
    {
	List<CCIProject> projectList = new ArrayList<CCIProject>();
	CCIProjectList listObject;

	List<CCIProject> userProjectList = codeCenterConfigManager.getProjectList();
	if (userProjectList.size() == 0)
	{
		projectList = getAllProjects(protexWrapper);
		listObject = new CCIProjectList(projectList);
		listObject.setUserSpecifiedSubset(false);
	}
	else
	{
        	log.info("Getting user supplied projects");
        	listObject = new CCIProjectList();
        	listObject.setUserSpecifiedSubset(true);
        	
        	for (CCIProject project : userProjectList)
        	{
        	    try
        	    {
        		// Retrieve the POJO from the SDK (this verifies that the project name is intact)
        		ProjectPojo projectPojo = protexWrapper.getProjectByName(project.getProjectName());
        		log.info("Found project: " + projectPojo.getProjectName());
        		
        		// If project came back, it is "valid", add it to our list and move on.
        		project.setProjectKey(projectPojo.getProjectKey());
        		projectList.add(project);
        	    } catch (Exception e)
        	    {
        		log.error("Unable to find Protex project with name: "
        			+ project);
        		listObject.addInvalidProject(project.getProjectName(), project.getProjectVersion());
        	    }
        	}
	}

	listObject.setList(projectList);
	return listObject;

    }

    /**
     * @param protexWrapper
     * @return
     * @throws Exception
     */
    private List<CCIProject> getAllProjects(ProtexServerWrapper protexWrapper)
	    throws CodeCenterImportException
    {
	log.info("Getting ALL user projects.");
	List<CCIProject> projects = null;
	 
	try
	{
	    projects = protexWrapper.getProjects(CCIProject.class);

	    for (CCIProject pojo : projects)
	    {
		pojo = this.setProjectVersion(pojo);
	    }
	} catch (Exception e)
	{
	    throw new CodeCenterImportException("Unable to get all projects", e);
	}

	return projects;
    }

    /**
     * Verifies project against server and appends version.
     * 
     * @param projectName
     * @param protexWrapper
     * @return
     */
    private CCIProject setProjectVersion(CCIProject project)
    {
	if (codeCenterConfigManager.getAppVersion() != null)
	{
	    if(project.getProjectVersion() == null)
	    {
		log.debug("Setting default version '{}' to project {}", codeCenterConfigManager.getAppVersion(), project.getProjectName());
		project.setProjectVersion(codeCenterConfigManager.getAppVersion());
	    }
	}

	return project;
    }


    protected List<CCIReportSummary> getReportSummaryList()
    {
	return this.reportSummaryList;
    }
    
}
