/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import static org.junit.Assert.*;

import java.util.Date;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import soleng.framework.standard.datatable.DataTable;
import soleng.framework.standard.datatable.Record;
import soleng.framework.standard.datatable.writer.DataSetWriter;
import soleng.framework.standard.datatable.writer.DataSetWriterStdOut;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV6_6_0;
import com.blackducksoftware.soleng.ccimport.CCIProcessor;
import com.blackducksoftware.soleng.ccimport.CCISingleServerProcessor;
import com.blackducksoftware.soleng.ccimport.TestUtils;
import com.blackducksoftware.soleng.ccimport.appadjuster.custom.JpmcAppAdjuster;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class JpmcAppAdjusterIT {

	private static final String CC_URL = "http://cc-integration.blackducksoftware.com/";
	public static final String SUPERUSER_USERNAME = "super";
	public static final String SUPERUSER_PASSWORD = "super";
	private static String APPLICATION0_NAME = "123-some application-PROD-current";
	private static String APPLICATION_VERSION = "v123";
	private static String USER1_USERNAME = "JUnit_ccimporter_report_user3";
	private static String USER1_PASSWORD = "password";
	private static final String USER_ROLE1 = "Application Developer";
	private static CodeCenterServerProxyV6_6_0 cc;
	
    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    CodeCenterConfigManager ccConfigManager = null;
    ProtexConfigManager protexConfigManager = null;

    @BeforeClass
	public static void setUpBeforeClass() throws Exception {
		cc = new CodeCenterServerProxyV6_6_0(
				CC_URL, SUPERUSER_USERNAME, SUPERUSER_PASSWORD, TestUtils.CONNECTION_TIMEOUT);
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
    public void testAdjuster() throws Exception
    {
    	TestUtils.createUser(cc, USER1_USERNAME, USER1_PASSWORD);
    	ApplicationIdToken appIdToken = TestUtils.createApplication(cc, APPLICATION0_NAME, APPLICATION_VERSION, USER1_USERNAME, USER_ROLE1);
    	
		String configPath = "src/test/resources/report.properties";
	
		ccConfigManager = new CodeCenterConfigManager(configPath);
		protexConfigManager = new ProtexConfigManager(configPath);
	
		CCIProcessor processor = new CCISingleServerProcessor(ccConfigManager,
			protexConfigManager);
		
		CCIProject project = new CCIProject();
		project.setProjectName(APPLICATION0_NAME);
		Date testDateValue = new Date();
		project.setAnalyzedDateValue(testDateValue);
		Application app = TestUtils.getApplication(cc, appIdToken);
		
		JpmcAppAdjuster appAdjuster = new JpmcAppAdjuster();
		appAdjuster.init(ccConfigManager);
		appAdjuster.adjustApp(app, project);
    }

}
