package com.blackducksoftware.tools.ccimport;

import java.util.concurrent.Callable;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;

public interface SyncProjectTaskFactory {
    Callable<CCIReportSummary> createTask(CCIProject project);

}
