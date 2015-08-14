/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.tools.ccimport;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.project.Project;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.report.CCIReportGenerator;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.ccimporter.model.CCIProjectList;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;
import com.blackducksoftware.tools.commonframework.core.multithreading.ListDistributor;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;

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
  
    private CCIReportGenerator reportGen = null;
    private int numThreads;
    private boolean threadExceptionThrown=false;
    private boolean threadWaitInterrupted=false;
    private String threadExceptionMessages="";
    private ProjectProcessorThreadWorkerFactory threadFactory;
    private ProtexServerWrapper protexServerWrapper;
    /**
     * @param ccConfigManager
     * @param protexConfigManager
     * @throws Exception
     */
	public CCISingleServerProcessor(CodeCenterConfigManager ccConfigManager,
			ProtexConfigManager protexConfigManager,
			CodeCenterServerWrapper codeCenterServerWrapper) throws Exception {
		
		super(ccConfigManager, codeCenterServerWrapper);
		protexServerWrapper = createProtexServerWrapper(protexConfigManager);
		Object appAdjusterObject = CCIProjectImporterHarness
				.getAppAdjusterObject(ccConfigManager);
		Method appAdjusterMethod = CCIProjectImporterHarness
				.getAppAdjusterMethod(codeCenterServerWrapper,
						protexServerWrapper, ccConfigManager, appAdjusterObject);

		// Construct the factory that the processor will use to create
		// the objects (run multi-threaded) to handle each subset of the project
		// list
		threadFactory = new ProjectProcessorThreadWorkerFactoryImpl(
				codeCenterServerWrapper, protexServerWrapper, ccConfigManager,
				appAdjusterObject, appAdjusterMethod);

		init(ccConfigManager, protexConfigManager, protexServerWrapper);
	}

	public CCISingleServerProcessor(CodeCenterConfigManager ccConfigManager,
			ProtexConfigManager protexConfigManager,
			CodeCenterServerWrapper codeCenterServerWrapper,
			ProjectProcessorThreadWorkerFactory threadFactory) throws Exception {
		
		super(ccConfigManager, codeCenterServerWrapper);
		protexServerWrapper = createProtexServerWrapper(protexConfigManager);
		this.threadFactory = threadFactory;

		init(ccConfigManager, protexConfigManager, protexServerWrapper);
	}

	private void init(CodeCenterConfigManager ccConfigManager,
			ProtexConfigManager protexConfigManager,
			ProtexServerWrapper protexServerWrapper) {
		this.protexServerWrapper = protexServerWrapper;
		numThreads = ccConfigManager.getNumThreads();

		// There will only be one in the single instance
		ServerBean protexBean = protexConfigManager.getServerBean();

		log.info("Using Protex URL [{}]", protexBean.getServerName());
	}

	@Override
	public void performSynchronize() throws CodeCenterImportException {

		List<CCIProject> projectList = getProjects().getList();

		log.info("Processing {} projects for synchronization", projectList);
		if (projectList.size() == 0) {
			throw new CodeCenterImportException("No valid projects were specified.");
		}

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

    private CCIProjectList getProjects() throws CodeCenterImportException
    {
	return getProjects(protexServerWrapper);
    }

	@Override
	public void runReport() throws CodeCenterImportException 
	{
		reportGen = new CCIReportGenerator(codeCenterWrapper, protexServerWrapper);
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
	private static ProtexServerWrapper createProtexServerWrapper(
			ProtexConfigManager configManager) throws Exception {
		ProtexServerWrapper protexWrapper;
		try {
			// Always just one code center
			ServerBean ccBean = configManager.getServerBean();
			if (ccBean == null)
				throw new Exception("No valid Protex server configurations found");

			log.info("Using Protex URL [{}]", ccBean.getServerName());

			protexWrapper = new ProtexServerWrapper(ccBean,
					configManager, true);

		} catch (Exception e) {
			throw new Exception("Unable to establish Protex connection: "
					+ e.getMessage());
		}
		return protexWrapper;
	}
}
