/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import java.lang.reflect.Method;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.connector.protex.ProtexServerWrapper;
import soleng.framework.core.config.ConfigConstants.APPLICATION;
import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimport.report.CCIReportGenerator;
import com.blackducksoftware.soleng.ccimporter.config.CCIConstants;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class CCIMultiServerProcessor extends CCIProcessor
{

    private static Logger log = LoggerFactory.getLogger(CCIMultiServerProcessor.class.getName());

    private ProtexConfigManager protexConfig = null;
    private Object appAdjusterObject;
    private Method appAdjusterMethod;
    
    /**
     * @param configManager
     * @param protexConfigManager 
     * @param multiserver
     * @throws Exception
     */
    public CCIMultiServerProcessor(CodeCenterConfigManager configManager, ProtexConfigManager protexConfigManager,
    		CodeCenterServerWrapper codeCenterServerWrapper,
    		Object appAdjusterObject, Method appAdjusterMethod)
	    throws Exception
    {
	super(configManager, codeCenterServerWrapper);
	
	this.protexConfig = protexConfigManager;
	this.appAdjusterObject = appAdjusterObject;
	this.appAdjusterMethod = appAdjusterMethod;
	log.info("Using Protex URL [{}]", protexConfig.getServerBean().getServerName());
    }

    /**
     * For every established Protex server bean, perform a process which includes:
     * 1) Grab all the projects
     * 2) Create an association
     * @throws CodeCenterImportException 
     */
    @Override
    public void performSynchronize() throws CodeCenterImportException
    {
	// First thing we do, is blank out any project lists that the user has specified
	List<CCIProject> userProjectList = codeCenterConfigManager.getProjectList();
	List<ServerBean> protexServers = codeCenterConfigManager.getServerListByApplication(APPLICATION.PROTEX);
	CodeCenterProjectSynchronizer synchronizer = new CodeCenterProjectSynchronizer(
		codeCenterWrapper, codeCenterConfigManager, appAdjusterObject, appAdjusterMethod);
	
	if(userProjectList.size() > 0)
	{
	    log.warn("Project list detected, ignoring in multi-server mode!");
	    userProjectList.clear();
	}

	// For every server bean we have, create a new protexWrapper
	// Use the supplied alias to set the:
	// - Protex Server Name / Alias
	// - Owner
	// Grab the projects and process import
	for(ServerBean protexServer : protexServers)
	{
	    log.info("Performing synchronization against:" + protexServer);
	    String protexAlias = protexServer.getAlias();

	    if (protexAlias.isEmpty())
		throw new CodeCenterImportException(
			"Protex alias cannot be empty!");
	    else
	    {
		log.info("Setting {} to {}",
			CCIConstants.PROTEX_SERVER_URL_PROPERTY, protexAlias);
		codeCenterConfigManager.setProtexServerName(protexAlias);
	    }

	    ProtexServerWrapper wrapper = null;
	    try
	    {
		wrapper = new ProtexServerWrapper(protexServer, protexConfig,
			true);
	    } catch (Exception e)
	    {
		throw new CodeCenterImportException(
			"Unable to establish connection against: "
				+ protexServer);
	    }
	    
	    List<CCIProject> projectList = getAllProjects(wrapper);
	    CCISingleServerProcessor.setLastAnalyzedDates(wrapper, projectList);
	    synchronizer.synchronize(projectList);
	}
    }

	@Override
	public void runReport() throws CodeCenterImportException {
		log.error("Not implemented");		
	}
    
    /**
     * This will get all the projects for that particular wrapper
     * The underlying call will always collect all, if the project is empty.
     * @param protexWrapper
     * @return
     * @throws CodeCenterImportException
     */
    private List<CCIProject> getAllProjects(ProtexServerWrapper protexWrapper) throws CodeCenterImportException
    {
	return getProjects(protexWrapper).getList();
	
    }
    
    // Used by unit tests
 	public CCIReportGenerator getReportGen() {
 		throw new UnsupportedOperationException("getReportGen() not implemented for CCIMultiServerProcessor");
 	}

}
