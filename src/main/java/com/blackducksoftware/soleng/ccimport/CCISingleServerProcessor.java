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

import soleng.framework.core.config.ConfigConstants.APPLICATION;
import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.common.ProjectPojo;
import soleng.framework.standard.protex.ProtexServerWrapper;

import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimport.report.CCIReportGenerator;
import com.blackducksoftware.soleng.ccimport.report.CCIReportSummary;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;
import com.blackducksoftware.soleng.ccimporter.model.CCIProjectList;

/**
 * 
 * @author Ari Kamen
 * @date Jun 27, 2014
 * 
 */
public class CCISingleServerProcessor extends CCIProcessor
{

    private static Logger log = LoggerFactory
	    .getLogger(CCISingleServerProcessor.class.getName());
  
    private ProtexServerWrapper protexWrapper = null;
    private CCIReportGenerator reportGen = null;

    /**
     * @param configManager
     * @param protexConfigManager
     * @throws Exception
     */
    public CCISingleServerProcessor(CodeCenterConfigManager configManager,
	    ProtexConfigManager protexConfigManager) throws Exception
    {
	super(configManager);

	// There will only be one in the single instance
	ServerBean protexBean = protexConfigManager.getServerBean();

	// Set up the local Protex config.
	protexWrapper = new ProtexServerWrapper(protexBean,
		protexConfigManager, true);

    }

    @Override
    public void performSynchronize() throws CodeCenterImportException
    {

	CodeCenterProjectSynchronizer synchronizer = new CodeCenterProjectSynchronizer(
		codeCenterWrapper, codeCenterConfigManager);

	List<CCIProject> projectList = getProjects().getList();
	log.info("Processing {} projects for synchronization", projectList);

	synchronizer.synchronize(projectList);
	reportSummaryList.add(synchronizer.getReportSummary());

    }

    private CCIProjectList getProjects() throws CodeCenterImportException
    {
	return getProjects(protexWrapper);
    }

	@Override
	public void runReport() throws CodeCenterImportException 
	{
		reportGen = new CCIReportGenerator(codeCenterWrapper, protexWrapper);
		CCIProjectList projectList = getProjects();
		
		log.info("Processing {} projects for reporting", projectList);
		reportGen.generateReport(projectList);
	}

	// Used by unit tests
	public CCIReportGenerator getReportGen() {
		return reportGen;
	}
	
}
