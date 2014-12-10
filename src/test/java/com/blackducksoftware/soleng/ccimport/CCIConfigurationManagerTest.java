/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.*;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;

public class CCIConfigurationManagerTest {
	private static final String APP_VERSION = "Unspecified";
	private static final String APP_NAME = "ccimport IT app";
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
		props.setProperty("protex.project.list", APP_NAME);
		props.setProperty("validate.application", "true");
		props.setProperty("cc.submit.request", "true");
		props.setProperty("validate.requests.delete", "true");
		props.setProperty("validate.max.validations", "123");
		props.setProperty("app.adjuster.only.if.bomedits", "true");
		props.setProperty("protex.project.name.filter", ".*-CURRENT$");
		
		CodeCenterConfigManager ccConfig = new CodeCenterConfigManager(props);
		
		assertEquals(123, ccConfig.getMaxValidations());
		assertTrue(ccConfig.isAppAdjusterOnlyIfBomEdits());
		
		ProtexConfigManager pConfig = new ProtexConfigManager(props);
		
		assertEquals(".*-CURRENT$", pConfig.getProtexProjectNameFilter());
	}

}
