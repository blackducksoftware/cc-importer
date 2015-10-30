package com.blackducksoftware.tools.ccimport;

import java.util.concurrent.Callable;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;

public class MockFailingSyncProjectTaskFactory implements
	SyncProjectTaskFactory {

    private class MockTask implements Callable<CCIReportSummary> {
	@Override
	public CCIReportSummary call() throws CodeCenterImportException {
	    System.out.println("MockTask.call() called from thread "
		    + Thread.currentThread().getName());
	    throw new CodeCenterImportException("mock exception");
	}
    }

    @Override
    public Callable<CCIReportSummary> createTask(CCIProject project) {
	return new MockTask();
    }

}
