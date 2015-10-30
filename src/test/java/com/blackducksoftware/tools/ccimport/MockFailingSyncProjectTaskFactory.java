package com.blackducksoftware.tools.ccimport;

import java.util.concurrent.Callable;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportNamedException;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;

public class MockFailingSyncProjectTaskFactory implements
	SyncProjectTaskFactory {

    private class MockFailingSyncProjectTask implements
	    Callable<CCIReportSummary> {
	private final CCIProject project;

	public MockFailingSyncProjectTask(CCIProject project) {
	    this.project = project;
	}

	@Override
	public CCIReportSummary call() throws CodeCenterImportException {
	    System.out
		    .println("MockFailingSyncProjectTask.call() called from thread "
			    + Thread.currentThread().getName());
	    throw new CodeCenterImportNamedException(project.getProjectName(),
		    "mock exception for project " + project.getProjectName());
	}
    }

    @Override
    public Callable<CCIReportSummary> createTask(CCIProject project) {
	return new MockFailingSyncProjectTask(project);
    }

}
