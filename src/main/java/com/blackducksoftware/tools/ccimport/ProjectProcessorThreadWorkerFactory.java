package com.blackducksoftware.tools.ccimport;

import java.util.List;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;

public interface ProjectProcessorThreadWorkerFactory {

	public Runnable createProjectProcessorThreadWorker(
			List<CCIProject> partialProjectList,
			List<CCIReportSummary> synchronizedThreadsReportSummaryList);

}