package com.blackducksoftware.tools.ccimport;

import java.lang.reflect.Method;
import java.util.List;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;

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
	 * @see com.blackducksoftware.tools.ccimport.ProjectProcessorThreadWorkerFactory#createProjectProcessorThreadWorker(java.util.List, java.util.List)
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
