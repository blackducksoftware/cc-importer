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
		props.setProperty("protex.server.name", "http://se-menger.blackducksoftware.com");
		props.setProperty("protex.user.name", "ccImportUser@blackducksoftware.com");
		props.setProperty("protex.password", "blackduck");
		props.setProperty("cc.server.name", "http://cc-integration/");
		props.setProperty("cc.user.name", "ccImportUser");
		props.setProperty("cc.password", "blackduck");
		props.setProperty("protex.password.isplaintext", "true");
		props.setProperty("cc.password.isplaintext", "true");
		props.setProperty("cc.protex.name", "Menger");
		props.setProperty("cc.default.app.version", APP_VERSION);
		props.setProperty("cc.workflow", "Serial");
		props.setProperty("cc.owner", APP_OWNER);
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
	public void testSpaceAfterComma() {
		Properties props = new Properties();
		props.setProperty("protex.server.name", "http://se-menger.blackducksoftware.com");
		props.setProperty("protex.user.name", "ccImportUser@blackducksoftware.com");
		props.setProperty("protex.password", "blackduck");
		props.setProperty("cc.server.name", "http://cc-integration/");
		props.setProperty("cc.user.name", "ccImportUser");
		props.setProperty("cc.password", "blackduck");
		props.setProperty("protex.password.isplaintext", "true");
		props.setProperty("cc.password.isplaintext", "true");
		props.setProperty("cc.protex.name", "Menger");
		props.setProperty("cc.default.app.version", APP_VERSION);
		props.setProperty("cc.workflow", "Serial");
		props.setProperty("cc.owner", APP_OWNER);
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
