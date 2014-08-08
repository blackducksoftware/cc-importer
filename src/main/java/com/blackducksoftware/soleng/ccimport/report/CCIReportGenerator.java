package com.blackducksoftware.soleng.ccimport.report;

import java.util.ArrayList;
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
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;
import com.blackducksoftware.soleng.ccimporter.model.CCIProjectList;
import com.blackducksoftware.soleng.ccreport.ComponentPojo;
import com.blackducksoftware.soleng.ccreport.datasource.ApplicationDataDao;
import com.blackducksoftware.soleng.ccreport.datasource.CodeCenter6_6_1Dao;
import com.blackducksoftware.soleng.ccreport.datatable.DataTable;
import com.blackducksoftware.soleng.ccreport.datatable.FieldDef;
import com.blackducksoftware.soleng.ccreport.datatable.FieldType;
import com.blackducksoftware.soleng.ccreport.datatable.Record;
import com.blackducksoftware.soleng.ccreport.datatable.RecordDef;
import com.blackducksoftware.soleng.ccreport.datatable.writer.DataSetWriter;
import com.blackducksoftware.soleng.ccreport.datatable.writer.DataSetWriterExcel;
import com.blackducksoftware.soleng.ccreport.datatable.writer.DataSetWriterStdOut;

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
	private DataTable dataTable;
	private RecordDef recordDef;
	
	public CCIReportGenerator(CodeCenterServerWrapper codeCenterWrapper, ProtexServerWrapper protexWrapper)
	{
		this.codeCenterWrapper = codeCenterWrapper;
		this.protexWrapper = protexWrapper;
		
	}
	
	private HashMap<String, CCIProject> getProjectMap(CCIProjectList projectListObject) {
		List<CCIProject> projectList = projectListObject.getList();
		HashMap<String, CCIProject> projectMap = new HashMap<String, CCIProject>(projectList.size());
		for (CCIProject project : projectList) {
			projectMap.put(project.getProjectName(), project);
		}
		return projectMap;
	}

	
	public void generateReport(CCIProjectList projectList) throws CodeCenterImportException
	{
		try {
			generateReportInternal(projectList);
		} catch (Exception e) {
			throw new CodeCenterImportException(e.getMessage());
		}	
	}
	
	// TODO: This method is getting ugly... need to refactor it
	private void generateReportInternal(CCIProjectList projectList) throws Exception
	{
		createDataTable();
		
		processInvalidUserSpecifiedProjects(projectList);
		HashMap<String, CCIProject> leftOvers = processValidProjects(projectList);
		processLeftOverProjects(leftOvers);
		
		DataSetWriter writerExcel = new DataSetWriterExcel("sync_report.xlsx");
		DataSetWriter writerStdOut = new DataSetWriterStdOut();
		writerStdOut.write(dataTable);
		writerExcel.write(dataTable);
	}
	
	private void createDataTable() {
		// Create the RecordDef
		List<FieldDef> fields = new ArrayList<FieldDef>();
		fields.add(new FieldDef("applicationName", FieldType.STRING, "Application Name"));
		fields.add(new FieldDef("applicationVersion", FieldType.STRING, "Application Version"));
		fields.add(new FieldDef("foundInCc", FieldType.STRING, "Found in CC"));
		fields.add(new FieldDef("foundInProtex", FieldType.STRING, "Found in Protex"));
		fields.add(new FieldDef("compListsMatch", FieldType.STRING, "Component Lists Match"));
		recordDef = new RecordDef(fields);
		
		// Create a new/empty DataSet that uses that RecordDef
		dataTable = new DataTable(recordDef);
	}
	
	private void processInvalidUserSpecifiedProjects(CCIProjectList projectList) throws Exception {
		for (CCIProject project : projectList.getInvalidList()) {
			Record record = new Record(recordDef);
			record.setFieldValue("applicationName", project.getProjectName());
			record.setFieldValue("applicationVersion", project.getProjectVersion());
			record.setFieldValue("foundInCc", "N/A");
			record.setFieldValue("foundInProtex", "No");
			record.setFieldValue("compListsMatch", "N/A");
			dataTable.add(record);
		}
	}
	
	private HashMap<String, CCIProject> processValidProjects(CCIProjectList projectList) throws Exception {
		log.info("Project list: " + projectList);
		
		HashMap<String, CCIProject> protexProjectMap = getProjectMap(projectList);
		boolean projectListIsUserSpecifiedSubset = projectList.isUserSpecifiedSubset();
		
		
		// We already have all projects, time to get all the applications.
		// Request all applications that belong to the user
		List<Application> apps=null;
		try {
			apps = getAllApplications();
		} catch (CodeCenterImportException e) {
			log.error("Error getting Code Center applications: " + e.getMessage());
		}
		HashMap<String, String> projectApplicationMap = new HashMap<String, String>();
		
		HashMap<String, String> processedAppnames = new HashMap<String, String>(apps.size());
		// Go through each application and find corresponding Protex project
		for(Application ccApp : apps)
		{
			String appName = ccApp.getName();
			String appVersion = ccApp.getVersion();
			
			if (projectListIsUserSpecifiedSubset && !protexProjectMap.containsKey(appName)) {
				log.info("[" + appName + ":" + appVersion + "] is not in user-specified protex project list; skipping it");
				continue;
			}
			
			Record record = new Record(recordDef);
			record.setFieldValue("applicationName", appName);
			record.setFieldValue("applicationVersion", appVersion);
			record.setFieldValue("foundInCc", "Yes");
			
			// Defaults
			record.setFieldValue("foundInProtex", "No");
			record.setFieldValue("compListsMatch", "N/A");
			
			// We need this check for this scenario:
			// User has specified a list of project names to report on
			// One of those names appears twice in Code Center (two applications / different versions)
			// In this scenario: when we get to the second app, because we deleted it from protexProjectMap,
			// it looks like the user just didn't specify it (but they did)
			if (processedAppnames.containsKey(appName)) {
				log.error("[" + appName + "] exists multiple times (multiple versions); in Code Center");
				record.setFieldValue("foundInProtex", "No");
				record.setFieldValue("compListsMatch", "N/A");
				dataTable.add(record);
				continue;
			}

			processedAppnames.put(appName, appName); // remember that we've processed this app so we can check for duplicates
		
			com.blackducksoftware.sdk.codecenter.application.data.Project associatedProject = null;

			try {
				associatedProject = this.codeCenterWrapper.getInternalApiWrapper().applicationApi
						.getAssociatedProtexProject(ccApp.getId());
			} catch (com.blackducksoftware.sdk.codecenter.fault.SdkFault e) {
				log.error("[" + appName + ":" + appVersion + "] no association found in CC (cause: " + e.getMessage() + ")");
				record.setFieldValue("foundInProtex", "No");
				record.setFieldValue("compListsMatch", "N/A");
				dataTable.add(record);
				continue;
			} catch (Exception e2) {
				log.error("Error: " + e2.getMessage());
				record.setFieldValue("foundInProtex", "No");
				record.setFieldValue("compListsMatch", "N/A");
				dataTable.add(record);
				continue;
			}

		    log.info("[" + appName + ":" + appVersion + "] application's association found in CC");
		    
		    if (protexProjectMap.containsKey(appName)) {
		    	log.info("[" + appName + ":" + appVersion + "] existed in protexProjectMap");
		    	CCIProject protexProject = protexProjectMap.get(appName);
		    	protexProjectMap.remove(appName);
		    	record.setFieldValue("foundInProtex", "Yes");
		    	
		    	try {
			    	if (compareComponentLists(protexProject, ccApp) == 0) {
			    		log.info("[" + appName + ":" + appVersion + "] Code Center / Protex Component lists are identical");
			    		
						record.setFieldValue("compListsMatch", "Yes");
			    	} else {
			    		log.error("[" + appName + ":" + appVersion + "] Code Center / Protex Component lists are different");
						record.setFieldValue("compListsMatch", "No");
			    	}
		    	} catch (Exception e) {
		    		log.error("[" + appName + ":" + appVersion + "] Error reading components from Code Center / Protex", e);
		    		record.setFieldValue("compListsMatch", "No");
		    	}
		    	
		    } else {
		    	if (projectListIsUserSpecifiedSubset) {
		    		log.error("[" + appName + ":" + appVersion + "] exists in Code Center but does not exist in protexProjectMap");
		    		record.setFieldValue("foundInProtex", "No");
		    	}
		    }			    
		    dataTable.add(record);
		}
		
		
		
		// Set summary basics
//		reportSummary.setTotalProtexProjects(projectList.size());
//		reportSummary.setTotalCCApplications(apps.size());
//		log.info("Summary so far: " + reportSummary.toString());
		
		return protexProjectMap;
	}
	
	private void processLeftOverProjects(HashMap<String, CCIProject> leftOvers) throws Exception {
		for (String key : leftOvers.keySet()) {
			CCIProject project = leftOvers.get(key);
			
			Record record = new Record(recordDef);
			record.setFieldValue("applicationName", project.getProjectName());
			record.setFieldValue("applicationVersion", project.getProjectVersion());
			record.setFieldValue("foundInCc", "No");
			record.setFieldValue("foundInProtex", "Yes");
			record.setFieldValue("compListsMatch", "N/A");
			dataTable.add(record);
		}
	}
	
	
	
	private int compareComponentLists(CCIProject protexProject, Application ccApp) throws Exception {
		// Get / compare components
		ComponentCollector protexComponentCollector = new ProtexComponentCollector(protexWrapper,
				protexProject.getProjectKey());
    	
		ComponentCollector ccComponentCollector;
		try {
			ApplicationDataDao ccDao = new CodeCenter6_6_1Dao(codeCenterWrapper);
			ccDao.setSkipNonKbComponents(false);
	    	ccComponentCollector = new CodeCenterComponentCollector(ccDao, ccApp.getId().getId());
		} catch (com.blackducksoftware.sdk.codecenter.fault.SdkFault e) {
			throw new SdkFault(e.getMessage());
		}
		log.info("\tCode Center Component list: " + ccComponentCollector);
		log.info("\tProtex      Component list: " + protexComponentCollector);
    	int compareResult = protexComponentCollector.compareTo(ccComponentCollector);
    	System.out.println("Compare result: " + compareResult);
    	return compareResult;
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
