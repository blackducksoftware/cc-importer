/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.tools.ccimport;

import java.lang.reflect.Method;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;


public class ProjectProcessorThreadWorker implements Runnable {
	private static Logger logger = LoggerFactory.getLogger(ProjectProcessorThreadWorker.class.getName());

	private CodeCenterServerWrapper codeCenterServerWrapper;
	private ProtexServerWrapper protexServerWrapper;
	private List<CCIProject> partialProjectList;
	private List<CCIReportSummary> reportSummaryList;
	private CodeCenterConfigManager codeCenterConfigManager;
	private Object appAdjusterObject;
	private Method appAdjusterMethod;

	public ProjectProcessorThreadWorker(CodeCenterServerWrapper codeCenterWrapper, ProtexServerWrapper protexWrapper,
			CodeCenterConfigManager codeCenterConfigManager,
			List<CCIProject> partialProjectList, List<CCIReportSummary> reportSummaryList,
			Object appAdjusterObject, Method appAdjusterMethod) {
		this.codeCenterServerWrapper = codeCenterWrapper;
		this.protexServerWrapper = protexWrapper;
		this.codeCenterConfigManager = codeCenterConfigManager;
		this.partialProjectList = partialProjectList;
		this.reportSummaryList = reportSummaryList;
		this.appAdjusterObject = appAdjusterObject;
		this.appAdjusterMethod = appAdjusterMethod;
	}

	public void run() {
		logger.debug("run() called");
		try {
			CodeCenterProjectSynchronizer synchronizer = new CodeCenterProjectSynchronizer(
					codeCenterServerWrapper, protexServerWrapper, codeCenterConfigManager,
					appAdjusterObject, appAdjusterMethod);
			synchronizer.synchronize(partialProjectList);
			synchronized(reportSummaryList) {
				if (reportSummaryList.size() == 0)
					reportSummaryList.add(synchronizer.getReportSummary());
				else
					reportSummaryList.get(0).addReportSummary(synchronizer.getReportSummary());
			}
			
		} catch (Exception e) {
			logger.error(e.getMessage());
			throw new UnsupportedOperationException(e.getMessage());
		}
	}

}
