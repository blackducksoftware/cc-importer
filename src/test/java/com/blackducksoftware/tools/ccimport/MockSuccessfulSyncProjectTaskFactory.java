package com.blackducksoftware.tools.ccimport;

import java.util.concurrent.Callable;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;

public class MockSuccessfulSyncProjectTaskFactory implements SyncProjectTaskFactory {

    private class MockTask implements Callable<CCIReportSummary> {
	@Override
	public CCIReportSummary call() {
	    System.out.println("MockTask.call() called from thread "
		    + Thread.currentThread().getName());
	    CCIReportSummary summary = new CCIReportSummary();
	    summary.setTotalProtexProjects(1);
	    summary.setTotalCCApplications(1);
	    summary.addToTotalValidatesPerfomed();
	    summary.addTotalPotentialAdds(2);
	    summary.addTotalPotentialDeletes(3);
	    summary.addRequestsAdded(2);
	    summary.addRequestsDeleted(3);
	    return summary;
	}
    }

    @Override
    public Callable<CCIReportSummary> createTask(CCIProject project) {
	return new MockTask();
    }

}
