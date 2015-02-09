/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeIdToken;
import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV6_6_0;
import com.blackducksoftware.sdk.codecenter.common.data.AttributeValue;
import com.blackducksoftware.soleng.ccimport.TestUtils;
import com.blackducksoftware.soleng.ccimport.appadjuster.custom.NumericPrefixedAppAdjuster;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIApplication;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class NumericPrefixAppAdjusterAppEditUrlIT {
	private static final String APPEDIT_URL = "http://localhost:8080/AppEdit/editappdetails";
	private static final String CONFIG_FILE = "src/test/resources/numprefixed_appediturl.properties";
	private static final long TIME_VALUE_OF_JAN1_2000 = 946702800000L;
	private static final String CUSTOM_ATTR_NAME = "Application Editor";
	
	private static final String CC_URL = "http://cc-integration.blackducksoftware.com/";
	public static final String SUPERUSER_USERNAME = "super";
	public static final String SUPERUSER_PASSWORD = "super";
	
	private static final String NUMPREFIX1_ATTR_VALUE = "200";
	private static final String APP_NAME_STRING = "appediturl test app";
	private static final String WORK_STREAM = "PROD";
	private static String APPLICATION1_NAME = NUMPREFIX1_ATTR_VALUE + "-" + APP_NAME_STRING + "-" + WORK_STREAM + "-CURRENT";
	
	private static String APPLICATION_VERSION = "v123";
	private static String OWNER = "unitTester@blackducksoftware.com";
	private static String USER_PASSWORD = "password";
	private static final String USER_ROLE1 = "Application Developer";

    @BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		String configPath = CONFIG_FILE;

        CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
    	ServerBean bean = new ServerBean();
		bean.setServerName(CC_URL);
		bean.setUserName(SUPERUSER_USERNAME);
		bean.setPassword(SUPERUSER_PASSWORD);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(bean, ccConfigManager);
		CodeCenterServerProxyV6_6_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
    	
		TestUtils.removeApplication(cc, APPLICATION1_NAME, APPLICATION_VERSION);
	}
	

    @Test
    public void testSettingAppEditUrl() throws Exception
    {
    	String configPath = CONFIG_FILE;

        CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
    	ServerBean bean = new ServerBean();
		bean.setServerName(CC_URL);
		bean.setUserName(SUPERUSER_USERNAME);
		bean.setPassword(SUPERUSER_PASSWORD);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(bean, ccConfigManager);
		CodeCenterServerProxyV6_6_0 cc = ccWrapper.getInternalApiWrapper().getProxy();

    	ApplicationIdToken appIdToken = TestUtils.createApplication(cc, APPLICATION1_NAME, APPLICATION_VERSION, OWNER, USER_ROLE1);
    	Application app = TestUtils.getApplication(cc, appIdToken);
    	CCIApplication cciApp = new CCIApplication(app, false);
		
		CCIProject project = new CCIProject();
		project.setProjectName(APPLICATION1_NAME);
		Date testDateValue = new Date();
		project.setAnalyzedDateValue(testDateValue);

		NumericPrefixedAppAdjuster appAdjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		appAdjuster.init(ccWrapper, ccConfigManager, tz);
		cciApp.setJustCreated(false);
		appAdjuster.adjustApp(cciApp, project);
		
		boolean foundAppEditUrlAttr = false;
		Application app2 = cc.getApplicationApi().getApplication(appIdToken); // Not sure why we have to get it again... weird
		List<AttributeValue> attrValues = app2.getAttributeValues();
		for (AttributeValue attrValue : attrValues) {
			AttributeIdToken a = (AttributeIdToken) attrValue.getAttributeId();
			String curAttrName = cc.getAttributeApi().getAttribute(a).getName();
			String curAttrValue = attrValue.getValues().get(0);
			System.out.println("attr name: " + curAttrName + 
				"; value: " + curAttrValue);
			
			if (CUSTOM_ATTR_NAME.equals(curAttrName)) {
				foundAppEditUrlAttr = true;
				assertEquals(APPEDIT_URL + "?appId=" + app.getId().getId(), curAttrValue);
			}
		}
		assertTrue(foundAppEditUrlAttr);
    }
    
}
