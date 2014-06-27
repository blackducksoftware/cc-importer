/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.soleng.ccimport;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.common.ProjectPojo;
import soleng.framework.standard.protex.ProtexServerWrapper;

import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;

/**
 * 
 *  @author Ari Kamen
 *  @date Jun 27, 2014
 *
 */
public class CCISingleServerProcessor extends CCIProcessor {

    private static Logger log = LoggerFactory.getLogger(CCISingleServerProcessor.class.getName());

    private ProtexServerWrapper protexWrapper = null;
    
    /**
     * @param configManager
     * @param protexConfigManager 
     * @throws Exception
     */
    public CCISingleServerProcessor(CodeCenterConfigManager configManager, 
	    ProtexConfigManager protexConfigManager)
	    throws Exception
    {
	super(configManager);
	
	// Set up the local Protex config.
	protexWrapper = new ProtexServerWrapper(protexConfigManager.getServerBean(), protexConfigManager, true);
	
    }

    /* (non-Javadoc)
     * @see com.blackducksoftware.soleng.ccimport.CCIProcessor#performImport()
     */
    @Override
    public void performImport()
    {

	CodeCenterProjectImporter importer = new CodeCenterProjectImporter();
	
	try{
		List<String> projectList = getProjects();
		
		log.info("Processing {} projects", projectList);
		importer.processImport(projectList);
	} catch (Exception e)
	{
		log.error("Unable to perform import", e);
	}
	
    }
   
    private List<String> getProjects()
    {
	log.info("Getting Protex project list");
	List<String> projectList = new ArrayList<String>();

	List<String> userProjectList = codeCenterConfigManager.getProjectList();
	log.info("Importing your specified projects");
	for(String projectName : userProjectList)
	{
		try{
			// TODO: This is almost a duplicate of the above, refactor.
			ProjectPojo project = protexWrapper.getProjectByName(projectName);					
			if(codeCenterConfigManager.getAppVersion() != null)
				projectName = project.getProjectName() + "," + codeCenterConfigManager.getAppVersion();
			
			log.info("Adding project: " + projectName);
			projectList.add(projectName);
		} catch (Exception e)
		{
			log.error("Unable to determine project with name: " + projectName);
		}
	}
	
	return projectList;
    }
}
