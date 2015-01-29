/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.soleng.ccimport.report.CCIReportSummary;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;


public class ProjectProcessorThread implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(ProjectProcessorThread.class.getName());

	private CodeCenterServerWrapper codeCenterServerWrapper;
	private List<CCIProject> partialProjectList;
	private List<CCIReportSummary> reportSummaryList;
	private CodeCenterConfigManager codeCenterConfigManager;

	public ProjectProcessorThread(CodeCenterServerWrapper codeCenterWrapper, CodeCenterConfigManager codeCenterConfigManager,
			List<CCIProject> partialProjectList, List<CCIReportSummary> reportSummaryList) {
		this.codeCenterServerWrapper = codeCenterWrapper;
		this.codeCenterConfigManager = codeCenterConfigManager;
		this.partialProjectList = partialProjectList;
		this.reportSummaryList = reportSummaryList;
	}

	public void run() {
		logger.debug("run() called");
		try {
			CodeCenterProjectSynchronizer synchronizer = new CodeCenterProjectSynchronizer(
					codeCenterServerWrapper, codeCenterConfigManager);
			synchronizer.synchronize(partialProjectList);
			reportSummaryList.add(synchronizer.getReportSummary());
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new UnsupportedOperationException(e.getMessage());
		}
	}

}
