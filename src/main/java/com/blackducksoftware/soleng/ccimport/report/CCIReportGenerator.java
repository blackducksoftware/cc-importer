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
import soleng.framework.standard.datatable.DataTable;
import soleng.framework.standard.datatable.FieldDef;
import soleng.framework.standard.datatable.FieldType;
import soleng.framework.standard.datatable.Record;
import soleng.framework.standard.datatable.RecordDef;
import soleng.framework.standard.datatable.writer.DataSetWriter;
import soleng.framework.standard.datatable.writer.DataSetWriterExcel;
import soleng.framework.standard.datatable.writer.DataSetWriterStdOut;

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

	private final static int EXCEL_CELL_MAX_CHARS = 32767;
	
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
	
	private void generateReportInternal(CCIProjectList projectList) throws Exception
	{
		createDataTable();
		
		processInvalidUserSpecifiedProjects(projectList);
		HashMap<String, CCIProject> leftOvers = processValidProjects(projectList);
		processLeftOverProjects(leftOvers);
		
		DataSetWriter writerExcel = new DataSetWriterExcel("sync_report.xlsx");
//		DataSetWriter writerStdOut = new DataSetWriterStdOut();
//		writerStdOut.write(dataTable);
		writerExcel.write(dataTable);
	}
	
	private void createDataTable() {
		// Create the RecordDef
		List<FieldDef> fields = new ArrayList<FieldDef>();
		fields.add(new FieldDef("applicationName", FieldType.STRING, "Application Name"));
		fields.add(new FieldDef("applicationVersion", FieldType.STRING, "Application Version"));
		fields.add(new FieldDef("status", FieldType.STRING, "Status"));
		fields.add(new FieldDef("foundInCc", FieldType.STRING, "Found in CC"));
		fields.add(new FieldDef("foundInProtex", FieldType.STRING, "Found in Protex"));
		fields.add(new FieldDef("compListsMatch", FieldType.STRING, "Component Lists Match"));
		fields.add(new FieldDef("compLists", FieldType.STRING, "Component Lists"));
		recordDef = new RecordDef(fields);
		
		// Create a new/empty DataSet that uses that RecordDef
		dataTable = new DataTable(recordDef);
	}
	
	private void processInvalidUserSpecifiedProjects(CCIProjectList projectList) throws Exception {
		for (CCIProject project : projectList.getInvalidList()) {
			Record record = new Record(recordDef);
			record.setFieldValue("applicationName", project.getProjectName());
			record.setFieldValue("applicationVersion", project.getProjectVersion());
			record.setFieldValue("status", "Error");
			record.setFieldValue("foundInCc", "N/A");
			record.setFieldValue("foundInProtex", "No");
			record.setFieldValue("compListsMatch", "N/A");
			record.setFieldValue("compLists", "");
			dataTable.add(record);
		}
	}
	
	// TODO: This method is too big / ugly... need to refactor it
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
		int appCount = apps.size();
		int appIndex=0;
		for(Application ccApp : apps)
		{
			log.info("Processing application " + ++appIndex + " of " + appCount + "...");
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
			record.setFieldValue("status", "Error");
			record.setFieldValue("foundInProtex", "No");
			record.setFieldValue("compListsMatch", "N/A");
			record.setFieldValue("compLists", "");
			
			// We need this check for this scenario:
			// User has specified a list of project names to report on
			// One of those names appears twice in Code Center (two applications / different versions)
			// In this scenario: when we get to the second app, because we deleted it from protexProjectMap,
			// it looks like the user just didn't specify it (but they did)
			if (processedAppnames.containsKey(appName)) {
				log.warn("[" + appName + "] exists multiple times (multiple versions); in Code Center");
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
				log.warn("[" + appName + ":" + appVersion + "] no association found in CC (cause: " + e.getMessage() + ")");
				record.setFieldValue("foundInProtex", "No");
				record.setFieldValue("compListsMatch", "N/A");
				dataTable.add(record);
				continue;
			} catch (Exception e2) {
				log.warn("Error: " + e2.getMessage());
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
		    	
		    	String compDiffString;
		    	try {
			    	if ((compDiffString = getComponentListDiffString(protexProject, ccApp, EXCEL_CELL_MAX_CHARS)) == null) {
			    		log.info("[" + appName + ":" + appVersion + "] Code Center / Protex Component lists are identical");
			    		
						record.setFieldValue("compListsMatch", "Yes");
						record.setFieldValue("status", "");
			    	} else {
			    		log.warn("[" + appName + ":" + appVersion + "] Code Center / Protex Component lists are different");
						record.setFieldValue("compListsMatch", "No");
						record.setFieldValue("compLists", compDiffString);
			    	}
		    	} catch (Exception e) {
		    		log.warn("[" + appName + ":" + appVersion + "] Error reading components from Code Center / Protex", e);
		    		record.setFieldValue("compListsMatch", "No");
		    	}
		    	
		    } else {
		    	if (projectListIsUserSpecifiedSubset) {
		    		log.warn("[" + appName + ":" + appVersion + "] exists in Code Center but does not exist in protexProjectMap");
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
	
	public DataTable getDataTable() {
		return dataTable;
	}

	private void processLeftOverProjects(HashMap<String, CCIProject> leftOvers) throws Exception {
		for (String key : leftOvers.keySet()) {
			CCIProject project = leftOvers.get(key);
			
			Record record = new Record(recordDef);
			record.setFieldValue("applicationName", project.getProjectName());
			record.setFieldValue("applicationVersion", project.getProjectVersion());
			record.setFieldValue("status", "Error");
			record.setFieldValue("foundInCc", "No");
			record.setFieldValue("foundInProtex", "Yes");
			record.setFieldValue("compListsMatch", "N/A");
			record.setFieldValue("compLists", "");
			dataTable.add(record);
		}
	}
	
	/**
	 * returns null if the component lists are identical, a string with both lists if not.
	 * @param protexProject
	 * @param ccApp
	 * @param maxLen
	 * @return
	 * @throws Exception
	 */
	private String getComponentListDiffString(CCIProject protexProject, Application ccApp, int maxLen) throws Exception {
		
		if (maxLen < 5) {
			throw new IllegalArgumentException("maxLen must be at least 5");
		}
		
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

		log.debug("\tCode Center Component list: " + ccComponentCollector);
		log.debug("\tProtex      Component list: " + protexComponentCollector);
		log.info("Comparing Protex component list (\"this\") to Code Center component list (\"other\")");
    	String diffString = protexComponentCollector.getDiffString(ccComponentCollector);

    	// TODO obsolete
//    	if (diffString != null) {
//    		StringBuilder builder = new StringBuilder();
//    		builder.append("Code Center Component list: ");
//    		builder.append(ccComponentCollector.toString());
//    		builder.append("\nProtex Component list: ");
//    		builder.append(protexComponentCollector.toString());
//    		if (builder.length() > maxLen) {
//    			diffString = builder.substring(0, maxLen-5) + "...";
//    		} else {
//    			diffString = builder.toString();
//    		}
//    	}
    	return diffString; // null if lists are identical
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
