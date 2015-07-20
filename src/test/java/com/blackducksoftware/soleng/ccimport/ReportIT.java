package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import soleng.framework.connector.protex.ProtexServerWrapper;
import soleng.framework.core.config.server.ServerBean;

import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV7_0;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;

import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.datatable.DataTable;
import soleng.framework.standard.datatable.Record;
import soleng.framework.standard.datatable.writer.DataSetWriter;
import soleng.framework.standard.datatable.writer.DataSetWriterStdOut;

public class ReportIT
{
	private static String APPLICATION0_NAME = "ccImporterReportTestApp1";
	private static String APPLICATION_VERSION = "v123";
	private static String USER1_USERNAME = "JUnit_ccimporter_report_user1";
	private static String USER1_PASSWORD = "password";
	private static CodeCenterServerProxyV7_0 cc;
	
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    // Local config files
    CodeCenterConfigManager ccConfigManager = null;
    ProtexConfigManager protexConfigManager = null;

    @BeforeClass
	public static void setUpBeforeClass() throws Exception {
		cc = new CodeCenterServerProxyV7_0(
				TestServerConfig.getCcServerName(), TestServerConfig.getCcUsername(), 
				TestServerConfig.getCcPassword(), TestUtils.CONNECTION_TIMEOUT);
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		
		TestUtils.removeApplication(cc, APPLICATION0_NAME, APPLICATION_VERSION);
		TestUtils.removeUserFromCc(cc, USER1_USERNAME);

	}
	
	/**
	 * Test report output for one scenario
	 * TODO: Would be nice if this were more comprehensive. Only tests one scenario out of several
	 * @throws Exception
	 */
    @Test
    public void testReport() throws Exception
    {
    	TestUtils.createUser(cc, USER1_USERNAME, USER1_PASSWORD);
    	TestUtils.createApplication(cc, APPLICATION0_NAME, APPLICATION_VERSION, USER1_USERNAME, 
    			TestServerConfig.getCcUserRole2(),
    			TestServerConfig.getCcCustomAttributeTextfield(), "test");
    	
		String configPath = "src/test/resources/report.properties";
	
		ccConfigManager = new CodeCenterConfigManager(configPath);
		protexConfigManager = new ProtexConfigManager(configPath);
	
		CodeCenterServerWrapper ccsw = new CodeCenterServerWrapper(ccConfigManager.getServerBean(),
				ccConfigManager);
		
		CCIProcessor processor = new CCISingleServerProcessor(ccConfigManager,
			protexConfigManager, ccsw);
		processor.runReport();
	
		DataTable report = processor.getReportGen().getDataTable();
		DataSetWriter writer = new DataSetWriterStdOut();
		writer.write(report);
	
		boolean foundMatch = false;
		for (Record rec : report)
		{
		    if (APPLICATION0_NAME.equals(rec
			    .getStringFieldValue("applicationName")))
		    {
		    	assertEquals(APPLICATION_VERSION, rec.getStringFieldValue("applicationVersion"));
			    assertEquals("Error", rec.getStringFieldValue("status"));
			    assertEquals("Yes", rec.getStringFieldValue("foundInCc"));
			    assertEquals("No", rec.getStringFieldValue("foundInProtex"));
				assertEquals("N/A", rec.getStringFieldValue("compListsMatch"));
				assertEquals("", rec.getStringFieldValue("compLists"));
				foundMatch = true;
				break;
		    }
		}
		assertTrue(foundMatch);
		System.out.println("testReport() Done.");
    }
}