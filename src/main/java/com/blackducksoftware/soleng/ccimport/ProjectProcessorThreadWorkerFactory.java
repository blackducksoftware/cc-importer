package com.blackducksoftware.soleng.ccimport;

import java.util.List;

import com.blackducksoftware.soleng.ccimport.report.CCIReportSummary;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public interface ProjectProcessorThreadWorkerFactory {

	public Runnable createProjectProcessorThreadWorker(
			List<CCIProject> partialProjectList,
			List<CCIReportSummary> synchronizedThreadsReportSummaryList);

}