package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.*;

import org.junit.Test;

import com.blackducksoftware.soleng.ccimport.report.CCIReportSummary;

public class CCIReportSummaryTest {

	@Test
	public void test() {
		CCIReportSummary sum1 = new CCIReportSummary();
		sum1.setTotalCCApplications(3);
		sum1.addTotalValidationsFailed();
		assertEquals(new Integer(3), sum1.getTotalCCApplications());
		
		CCIReportSummary sum2 = new CCIReportSummary();
		sum2.setTotalCCApplications(5);
		sum2.addTotalValidationsFailed();
		sum2.addTotalValidationsFailed();
		
		sum2.addReportSummary(sum1);
		
		assertEquals(new Integer(8), sum2.getTotalCCApplications());
		assertEquals(new Integer(3), sum2.getTotalValidationsFailed());
	}

}
