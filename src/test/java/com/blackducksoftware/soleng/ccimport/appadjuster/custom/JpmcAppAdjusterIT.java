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
import com.blackducksoftware.soleng.ccimport.appadjuster.custom.JpmcAppAdjuster;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class JpmcAppAdjusterIT {
	private static final long TIME_VALUE_OF_JAN1_2000 = 946702800000L;
	private static final String CUSTOM_ATTR_NAME = "Sample Textfield";
	
	private static final String CC_URL = "http://cc-integration.blackducksoftware.com/";
	public static final String SUPERUSER_USERNAME = "super";
	public static final String SUPERUSER_PASSWORD = "super";
	
	private static final String SEALID1_ATTR_VALUE = "123";
	private static final String APP_NAME_STRING = "some application";
	private static final String WORK_STREAM = "PROD";
	private static String APPLICATION1_NAME = SEALID1_ATTR_VALUE + "-" + APP_NAME_STRING + "-" + WORK_STREAM + "-current";
	
	private static final String SEALID2_ATTR_VALUE = "123456";
	private static String APPLICATION2_NAME = SEALID2_ATTR_VALUE + "-" + APP_NAME_STRING + "-" + WORK_STREAM + "-current";
	
	private static String APPLICATION_VERSION = "v123";
	private static String USER1_USERNAME = "JUnit_ccimporter_report_user3";
	private static String USER2_USERNAME = "JUnit_ccimporter_report_user3a";
	private static String USER_PASSWORD = "password";
	private static final String USER_ROLE1 = "Application Developer";

    @BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		String configPath = "src/test/resources/jpmc_sealid.properties";

        CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
    	ServerBean bean = new ServerBean();
		bean.setServerName(CC_URL);
		bean.setUserName(SUPERUSER_USERNAME);
		bean.setPassword(SUPERUSER_PASSWORD);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(bean, ccConfigManager);
		CodeCenterServerProxyV6_6_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
    	
		TestUtils.removeApplication(cc, APPLICATION1_NAME, APPLICATION_VERSION);
		TestUtils.removeApplication(cc, APPLICATION2_NAME, APPLICATION_VERSION);
		TestUtils.removeUserFromCc(cc, USER1_USERNAME);
		TestUtils.removeUserFromCc(cc, USER2_USERNAME);
	}
	

    @Test
    public void testSealId() throws Exception
    {
    	String configPath = "src/test/resources/jpmc_sealid.properties";

        CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
    	ServerBean bean = new ServerBean();
		bean.setServerName(CC_URL);
		bean.setUserName(SUPERUSER_USERNAME);
		bean.setPassword(SUPERUSER_PASSWORD);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(bean, ccConfigManager);
		CodeCenterServerProxyV6_6_0 cc = ccWrapper.getInternalApiWrapper().getProxy();

    	TestUtils.createUser(cc, USER1_USERNAME, USER_PASSWORD);
    	ApplicationIdToken appIdToken = TestUtils.createApplication(cc, APPLICATION1_NAME, APPLICATION_VERSION, USER1_USERNAME, USER_ROLE1);
    	Application app = TestUtils.getApplication(cc, appIdToken);
		
		CCIProject project = new CCIProject();
		project.setProjectName(APPLICATION1_NAME);
		Date testDateValue = new Date();
		project.setAnalyzedDateValue(testDateValue);

		JpmcAppAdjuster appAdjuster = new JpmcAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		appAdjuster.init(ccWrapper, ccConfigManager, tz);
		appAdjuster.adjustApp(app, project);
		
		boolean foundSealIdAttr = false;
		System.out.println("==============\nGetting attr values:");
		Application app2 = cc.getApplicationApi().getApplication(appIdToken); // Not sure why we have to get it again... weird
		List<AttributeValue> attrValues = app2.getAttributeValues();
		for (AttributeValue attrValue : attrValues) {
			AttributeIdToken a = (AttributeIdToken) attrValue.getAttributeId();
			String curAttrName = cc.getAttributeApi().getAttribute(a).getName();
			String curAttrValue = attrValue.getValues().get(0);
			System.out.println("attr name: " + curAttrName + 
				"; value: " + curAttrValue);
			
			if (CUSTOM_ATTR_NAME.equals(curAttrName)) {
				foundSealIdAttr = true;
				assertEquals(SEALID1_ATTR_VALUE, curAttrValue);
			}
		}
		assertTrue(foundSealIdAttr);
    }
    
    @Test
    public void testAnalyzedDate() throws Exception
    {
    	String configPath = "src/test/resources/jpmc_analyzeddate.properties";
    	
    	CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
    	ServerBean bean = new ServerBean();
		bean.setServerName(CC_URL);
		bean.setUserName(SUPERUSER_USERNAME);
		bean.setPassword(SUPERUSER_PASSWORD);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(bean, ccConfigManager);
		CodeCenterServerProxyV6_6_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
    	
    	TestUtils.createUser(cc, USER2_USERNAME, USER_PASSWORD);
    	ApplicationIdToken appIdToken = TestUtils.createApplication(cc, APPLICATION2_NAME, APPLICATION_VERSION, USER2_USERNAME, USER_ROLE1);
    	Application app = TestUtils.getApplication(cc, appIdToken);
		
		CCIProject project = new CCIProject();
		project.setProjectName(APPLICATION2_NAME);
		Date testDateValue = new Date(TIME_VALUE_OF_JAN1_2000);
		project.setAnalyzedDateValue(testDateValue);

		JpmcAppAdjuster appAdjuster = new JpmcAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		appAdjuster.init(ccWrapper, ccConfigManager, tz);
		appAdjuster.adjustApp(app, project);
		
		boolean foundSealIdAttr = false;
		System.out.println("==============\nGetting attr values:");
		Application app2 = cc.getApplicationApi().getApplication(appIdToken); // Not sure why we have to get it again... weird
		List<AttributeValue> attrValues = app2.getAttributeValues();
		for (AttributeValue attrValue : attrValues) {
			AttributeIdToken a = (AttributeIdToken) attrValue.getAttributeId();
			String curAttrName = cc.getAttributeApi().getAttribute(a).getName();
			String curAttrValue = attrValue.getValues().get(0);
			System.out.println("attr name: " + curAttrName + 
				"; value: " + curAttrValue);
			
			if (CUSTOM_ATTR_NAME.equals(curAttrName)) {
				foundSealIdAttr = true;
				assertEquals("01-01-2000", curAttrValue);
			}
		}
		assertTrue(foundSealIdAttr);
    }

}
