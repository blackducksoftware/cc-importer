package com.blackducksoftware.tools.ccimport;

import static org.junit.Assert.*;

import org.junit.Test;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;

public class CCIReportSummaryTest {

    @Test
    public void test() {
	CCIReportSummary sum1 = new CCIReportSummary();
	sum1.setTotalCCApplications(3);
	sum1.addTotalValidationsFailed();
	sum1.addToFailedImportList("failed import 1");
	sum1.addToFailedValidationList("failed validation 1");
	assertEquals(new Integer(3), sum1.getTotalCCApplications());
	assertEquals("failed import 1", sum1.getFailedImportList().get(0));
	assertEquals("failed validation 1",
		sum1.getFailedValidationList().get(0));

	CCIReportSummary sum2 = new CCIReportSummary();
	sum2.setTotalCCApplications(5);
	sum2.addTotalValidationsFailed();
	sum2.addTotalValidationsFailed();
	assertEquals(0, sum2.getFailedImportList().size());
	assertEquals(0, sum2.getFailedValidationList().size());

	sum2.addReportSummary(sum1);

	assertEquals(new Integer(8), sum2.getTotalCCApplications());
	assertEquals(new Integer(3), sum2.getTotalValidationsFailed());

	assertEquals("failed import 1", sum2.getFailedImportList().get(0));
	assertEquals("failed validation 1",
		sum2.getFailedValidationList().get(0));
    }

}
