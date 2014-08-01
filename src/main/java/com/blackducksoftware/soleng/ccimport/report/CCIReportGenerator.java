package com.blackducksoftware.soleng.ccimport.report;

import java.util.HashMap;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.core.exception.CommonFrameworkException;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.protex.ProtexProjectPojo;
import soleng.framework.standard.protex.ProtexServerWrapper;

import com.blackducksoftware.sdk.codecenter.administration.data.ProtexServer;
import com.blackducksoftware.sdk.codecenter.administration.data.ServerNameOrIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

/**
 * Class responsible for the report generation portion of the CCI.
 * 
 * 
 * @author akamen
 *
 */
public class CCIReportGenerator {

	private static Logger log = LoggerFactory
			    .getLogger(CCIReportGenerator.class.getName());

	private CodeCenterServerWrapper codeCenterWrapper = null;
	private ProtexServerWrapper protexWrapper = null;
	private CCIReportSummary reportSummary = new CCIReportSummary();
	
	public CCIReportGenerator(CodeCenterServerWrapper codeCenterWrapper, ProtexServerWrapper protexWrapper)
	{
		this.codeCenterWrapper = codeCenterWrapper;
		this.protexWrapper = protexWrapper;
	}

	public void generateReport(List<CCIProject> projectList) throws CodeCenterImportException 
	{
		// We already have all the projects, time to get all the applications.
		// Request all applications that belong to the user
		List<Application> apps = getAllApplications();
		HashMap<String, String> projectApplicationMap = new HashMap<String, String>();
		
		// Go through each application and find corresponding Protex project
		for(Application app : apps)
		{
			String appName = app.getName();
			String applicationId = app.getId().getId();
		
			com.blackducksoftware.sdk.codecenter.application.data.Project associatedProject = null;
			try
			{
			    associatedProject = this.codeCenterWrapper.getInternalApiWrapper().applicationApi
				    .getAssociatedProtexProject(app.getId());
			    
			    String associatedProtexId = associatedProject.getId().getId();
			    log.info("[{}] application's association found", appName);
			    
			    try{
			    ServerNameOrIdToken serverNameToken = associatedProject.getId().getServerId();	    
			    ProtexServer server = codeCenterWrapper.getInternalApiWrapper().settingsApi.getServerDetails(serverNameToken);
			   
			    } catch (SdkFault f)
			    {
			    	log.error("Unable to get Server information: " + f.getMessage());
			    }
			    

			    projectApplicationMap.put(applicationId, associatedProtexId);
			    
			    try {
					ProtexProjectPojo pp = (ProtexProjectPojo) protexWrapper.getProjectByID(associatedProtexId);
				} catch (CommonFrameworkException e) {
					log.warn(e.getMessage());
				}
			    
			} catch (SdkFault e)
			{
				log.error("No association for {} found, cause {}",appName, e.getFaultInfo().getErrorCode().toString());
			}
		}
		
		// Set summary basics
		reportSummary.setTotalProtexProjects(projectList.size());
		reportSummary.setTotalCCApplications(apps.size());
		
		
		
		log.info("Summary so far: " + reportSummary.toString());
	}

	private HashMap<String, Application> buildApplicationMap(
			List<Application> apps) throws CodeCenterImportException{
		HashMap<String, Application> map = new HashMap<String, Application>();
		
		try
		{
			for(Application app: apps)
			{
				String appName = app.getName();
				map.put(appName, app);
			}
		} catch (Exception e)
		{
			throw new CodeCenterImportException("Unable to construct Code Center application map", e);
		}
		
		
		return map;
	}

	private List<Application> getAllApplications() throws CodeCenterImportException 
	{
		ApplicationPageFilter apf = new ApplicationPageFilter();
		apf.setFirstRowIndex(0);
		apf.setLastRowIndex(Integer.MAX_VALUE);
		List<Application> apps = null;
		
		try{
			log.info("Getting Code Center applications...");
			apps = codeCenterWrapper.getInternalApiWrapper().applicationApi.searchApplications("", apf);
			log.info("Returned {} applications.", apps.size());
		} catch (Exception ccie)
		{
			throw new CodeCenterImportException("Error getting applications", ccie);
		}
		
		return apps;
	}
}
