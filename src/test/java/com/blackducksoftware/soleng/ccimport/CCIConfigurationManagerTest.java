/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class CCIConfigurationManagerTest {
	private static final String APP_VERSION = "Unspecified";
	private static final String APP_NAME1 = "ccimport IT app";
	private static final String APP_NAME2 = "another app";
	private static final String APP_OWNER = "unitTester@blackducksoftware.com";

	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}

	@Test
	public void test() {
		Properties props = new Properties();
		props.setProperty("protex.server.name", TestServerConfig.getProtexServerName());
		props.setProperty("protex.user.name", TestServerConfig.getProtexUsername());
		props.setProperty("protex.password", TestServerConfig.getProtexPassword());
		props.setProperty("cc.server.name", TestServerConfig.getCcServerName());
		props.setProperty("cc.user.name", TestServerConfig.getCcUsername());
		props.setProperty("cc.password", TestServerConfig.getCcPassword());
		props.setProperty("protex.password.isencrypted", "false");
		props.setProperty("cc.password.isencrypted", "false");
		props.setProperty("cc.protex.name", TestServerConfig.getProtexServerNameInCc());
		props.setProperty("cc.default.app.version", APP_VERSION);
		props.setProperty("cc.workflow", TestServerConfig.getCcWorkflow());
		props.setProperty("cc.owner", TestServerConfig.getCcUsername2());
		props.setProperty("protex.project.list", APP_NAME1);
		props.setProperty("validate.application", "true");
		props.setProperty("cc.submit.request", "true");
		props.setProperty("validate.requests.delete", "true");
		props.setProperty("revalidate.after.changing.bom", "true");
		props.setProperty("app.adjuster.only.if.bomedits", "true");
		props.setProperty("protex.project.name.filter", ".*-CURRENT$");
		
		CodeCenterConfigManager ccConfig = new CodeCenterConfigManager(props);
		
		assertTrue(ccConfig.isReValidateAfterBomChange());
		assertTrue(ccConfig.isAppAdjusterOnlyIfBomEdits());
		
		ProtexConfigManager pConfig = new ProtexConfigManager(props);
		
		assertTrue(Pattern.compile(".*-CURRENT$").toString().equals(pConfig.getProtexProjectNameFilterPattern().toString()));
	}
	
	@Test
	public void testAddDeleteSubmit() {
		Properties props = new Properties();
		props.setProperty("protex.server.name", TestServerConfig.getProtexServerName());
		props.setProperty("protex.user.name", TestServerConfig.getProtexUsername());
		props.setProperty("protex.password", TestServerConfig.getProtexPassword());
		props.setProperty("cc.server.name", TestServerConfig.getCcServerName());
		props.setProperty("cc.user.name", TestServerConfig.getCcUsername());
		props.setProperty("cc.password", TestServerConfig.getCcPassword());
		props.setProperty("protex.password.isencrypted", "false");
		props.setProperty("cc.password.isencrypted", "false");
		props.setProperty("cc.protex.name", TestServerConfig.getProtexServerNameInCc());
		props.setProperty("cc.default.app.version", APP_VERSION);
		props.setProperty("cc.workflow", TestServerConfig.getCcWorkflow());
		props.setProperty("cc.owner", TestServerConfig.getCcUsername2());
		props.setProperty("protex.project.list", APP_NAME1);
		props.setProperty("validate.application", "true");
		
		props.setProperty("cc.submit.request", "false");
		props.setProperty("validate.requests.add", "false");
		props.setProperty("validate.requests.delete", "false");
		CodeCenterConfigManager ccConfig = new CodeCenterConfigManager(props);
		assertFalse(ccConfig.isPerformAdd());
		assertFalse(ccConfig.isPerformDelete());
		assertFalse(ccConfig.isSubmit());
		
		props.setProperty("validate.requests.add", "true");
		props.setProperty("validate.requests.delete", "true");
		props.setProperty("cc.submit.request", "true");
		ccConfig = new CodeCenterConfigManager(props);
		assertTrue(ccConfig.isPerformAdd());
		assertTrue(ccConfig.isPerformDelete());
		assertTrue(ccConfig.isSubmit());
		
		props.setProperty("validate.requests.add", "true");
		props.setProperty("validate.requests.delete", "true");
		props.setProperty("cc.submit.request", "false");
		ccConfig = new CodeCenterConfigManager(props);
		assertTrue(ccConfig.isPerformAdd());
		assertTrue(ccConfig.isPerformDelete());
		assertFalse(ccConfig.isSubmit());
		
		props.setProperty("validate.requests.add", "false");
		props.setProperty("validate.requests.delete", "true");
		props.setProperty("cc.submit.request", "true");
		ccConfig = new CodeCenterConfigManager(props);
		assertFalse(ccConfig.isPerformAdd());
		assertTrue(ccConfig.isPerformDelete());
		assertTrue(ccConfig.isSubmit());
	}
	
	@Test
	public void testSpaceAfterComma() {
		Properties props = new Properties();
		props.setProperty("protex.server.name", TestServerConfig.getProtexServerName());
		props.setProperty("protex.user.name", TestServerConfig.getProtexUsername());
		props.setProperty("protex.password", TestServerConfig.getProtexPassword());
		props.setProperty("cc.server.name", TestServerConfig.getCcServerName());
		props.setProperty("cc.user.name", TestServerConfig.getCcUsername());
		props.setProperty("cc.password", TestServerConfig.getCcPassword());
		props.setProperty("protex.password.isencrypted", "false");
		props.setProperty("cc.password.isencrypted", "false");
		props.setProperty("cc.protex.name", TestServerConfig.getProtexServerNameInCc());
		props.setProperty("cc.default.app.version", APP_VERSION);
		props.setProperty("cc.workflow", TestServerConfig.getCcWorkflow());
		props.setProperty("cc.owner", TestServerConfig.getCcUsername2());
		props.setProperty("protex.project.list", APP_NAME1 + ", " + APP_NAME2);
		props.setProperty("validate.application", "true");
		props.setProperty("cc.submit.request", "true");
		props.setProperty("validate.requests.delete", "true");
		props.setProperty("revalidate.after.changing.bom", "true");
		props.setProperty("app.adjuster.only.if.bomedits", "true");
		props.setProperty("protex.project.name.filter", ".*-CURRENT$");
		
		CodeCenterConfigManager ccConfig = new CodeCenterConfigManager(props);
		
		List<CCIProject> projects = ccConfig.getProjectList();
		
		// Make sure 2nd app is in list, and without leading space
		boolean foundSecondApp = false;
		for (CCIProject project : projects) {
			if (project.getProjectName().contains("another")) {
				assertEquals("another app", project.getProjectName());
				foundSecondApp = true;
			}
		}
		assertTrue(foundSecondApp);
		
	}

}
