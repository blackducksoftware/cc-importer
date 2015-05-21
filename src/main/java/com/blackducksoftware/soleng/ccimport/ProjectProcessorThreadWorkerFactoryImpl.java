package com.blackducksoftware.soleng.ccimport;

import java.lang.reflect.Method;
import java.util.List;

import soleng.framework.connector.protex.ProtexServerWrapper;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.soleng.ccimport.report.CCIReportSummary;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class ProjectProcessorThreadWorkerFactoryImpl implements ProjectProcessorThreadWorkerFactory {
	private CodeCenterServerWrapper codeCenterWrapper;
	private ProtexServerWrapper protexServerWrapper;
	private CodeCenterConfigManager codeCenterConfigManager;
	private Object appAdjusterObject;
	private Method appAdjusterMethod;
	
	public ProjectProcessorThreadWorkerFactoryImpl(CodeCenterServerWrapper codeCenterWrapper, ProtexServerWrapper protexServerWrapper,
			CodeCenterConfigManager codeCenterConfigManager,
			Object appAdjusterObject, Method appAdjusterMethod) {
		this.codeCenterConfigManager = codeCenterConfigManager;
		this.protexServerWrapper = protexServerWrapper;
		this.codeCenterWrapper = codeCenterWrapper;
		this.appAdjusterObject = appAdjusterObject;
		this.appAdjusterMethod = appAdjusterMethod;
	}

	/* (non-Javadoc)
	 * @see com.blackducksoftware.soleng.ccimport.ProjectProcessorThreadWorkerFactory#createProjectProcessorThreadWorker(java.util.List, java.util.List)
	 */
	@Override
	public Runnable createProjectProcessorThreadWorker(List<CCIProject> partialProjectList,
			List<CCIReportSummary> synchronizedThreadsReportSummaryList) {
		Runnable threadWorker = new ProjectProcessorThreadWorker(
				codeCenterWrapper, protexServerWrapper, codeCenterConfigManager,
				partialProjectList, synchronizedThreadsReportSummaryList,
				appAdjusterObject, appAdjusterMethod);
		return threadWorker;
	}
}
