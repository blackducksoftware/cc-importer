/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.soleng.ccimport;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.core.config.ConfigConstants.APPLICATION;
import soleng.framework.core.config.server.ServerBean;
import soleng.framework.core.exception.CommonFrameworkException;
import soleng.framework.core.multithreading.ListDistributor;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.common.ProjectPojo;
import soleng.framework.standard.protex.ProtexServerWrapper;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.project.Project;
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
    private int numThreads;
    private boolean threadExceptionThrown=false;
    private boolean threadWaitInterrupted=false;
    private String threadExceptionMessages="";
    private ProjectProcessorThreadWorkerFactory threadFactory;

    /**
     * @param configManager
     * @param protexConfigManager
     * @throws Exception
     */
    public CCISingleServerProcessor(CodeCenterConfigManager configManager,
	    ProtexConfigManager protexConfigManager, CodeCenterServerWrapper codeCenterServerWrapper,
	    ProjectProcessorThreadWorkerFactory threadFactory) throws Exception
    {
	super(configManager, codeCenterServerWrapper);
	
	numThreads = configManager.getNumThreads();

	// There will only be one in the single instance
	ServerBean protexBean = protexConfigManager.getServerBean();

	log.info("Using Protex URL [{}]", protexBean.getServerName());
	
	// Set up the local Protex config.
	protexWrapper = new ProtexServerWrapper(protexBean,
		protexConfigManager, true);

	this.threadFactory = threadFactory;
    }

	@Override
	public void performSynchronize() throws CodeCenterImportException {

		List<CCIProject> projectList = getProjects().getList();
		setLastAnalyzedDates(protexWrapper, projectList);

		log.info("Processing {} projects for synchronization", projectList);

		ListDistributor distrib = new ListDistributor(numThreads,
				projectList.size());

		// Create a temporary list of summary reports, one for each thread
		List<CCIReportSummary> threadsReportSummaryList = new ArrayList<CCIReportSummary>();
		List<CCIReportSummary> synchronizedThreadsReportSummaryList = Collections.synchronizedList(threadsReportSummaryList);
		
		// Launch a bunch of threads to process apps
		List<Thread> startedThreads = new ArrayList<Thread>(
				distrib.getNumThreads());
		for (int i = 0; i < distrib.getNumThreads(); i++) {
			List<CCIProject> partialProjectList = projectList.subList(
					distrib.getFromListIndex(i), distrib.getToListIndex(i));

			Runnable threadWorker = 
					threadFactory.createProjectProcessorThreadWorker(partialProjectList, synchronizedThreadsReportSummaryList);

			Thread thread = new Thread(threadWorker, "ProjectProcessorThread" + i);
			thread.setUncaughtExceptionHandler(new WorkerThreadExceptionHandler());
			log.info("Starting thread " + thread.getName());
			thread.start();
			startedThreads.add(thread);
		}

		// Now wait for all threads to finish
		for (Thread startedThread : startedThreads) {
			log.info("Waiting for thread " + startedThread.getName());
			try {
				startedThread.join();
			} catch (InterruptedException e) {
				this.threadWaitInterrupted=true;
			}
		}
		
		log.info("Consolidated summary:\n" + synchronizedThreadsReportSummaryList.toString());
		
		if (threadExceptionThrown) {
			log.error("Exception(s) thrown from worker thread(s): " + threadExceptionMessages);
		} else if (threadWaitInterrupted) {
			log.error("Interrupted while waiting for worker threads to finish");
		} else {
			log.info("All threads finished.");
		}

		reportSummaryList = threadsReportSummaryList;
	}
	
	public String getThreadExceptionMessages() {
		return this.threadExceptionMessages;
	}
    
    public static void setLastAnalyzedDates(ProtexServerWrapper protexWrapper, List<CCIProject> projectList) throws CodeCenterImportException {
    	for (CCIProject project : projectList) {
    		Project sdkProject = null;
    		try {
    			sdkProject = protexWrapper.getInternalApiWrapper().getProjectApi().getProjectById(project.getProjectKey());
    		} catch (SdkFault e) {
    			throw new CodeCenterImportException("Error getting project: " + project.getProjectName() + " in order to get lastAnalyzedDate: " + e.getMessage());
    		}
    		project.setAnalyzedDateValue(sdkProject.getLastAnalyzedDate());
    	}
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
	
	private class WorkerThreadExceptionHandler implements Thread.UncaughtExceptionHandler {
		public WorkerThreadExceptionHandler() {
			log.debug("WorkerThreadExceptionHandler constructed");
		}
		public void uncaughtException(Thread t, Throwable e) {
			String msg = "Thread " + t.getName() + " failed: " + e.getMessage();
			log.error(msg, e);
			threadExceptionThrown = true;
			synchronized(this) {
				threadExceptionMessages += ";" + msg;
			}
		}
	}
	
}
