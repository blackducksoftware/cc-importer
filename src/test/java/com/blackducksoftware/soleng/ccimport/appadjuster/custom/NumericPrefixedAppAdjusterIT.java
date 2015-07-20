/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import static org.junit.Assert.*;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import soleng.framework.connector.protex.ProtexServerWrapper;
import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeIdToken;
import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV7_0;
import com.blackducksoftware.sdk.codecenter.common.data.AttributeValue;
import com.blackducksoftware.soleng.ccimport.ProtexTestUtils;
import com.blackducksoftware.soleng.ccimport.TestServerConfig;
import com.blackducksoftware.soleng.ccimport.TestUtils;
import com.blackducksoftware.soleng.ccimport.appadjuster.custom.NumericPrefixedAppAdjuster;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIApplication;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class NumericPrefixedAppAdjusterIT {
	private static final long TIME_VALUE_OF_JAN1_2000 = 946702800000L;
	
	private static final String NUMPREFIX1_ATTR_VALUE = "123";
	private static final String APP_NAME_STRING = "some application";
	private static final String WORK_STREAM = "PROD";
	private static String APPLICATION1_NAME = NUMPREFIX1_ATTR_VALUE + "-" + APP_NAME_STRING + "-" + WORK_STREAM + "-CURRENT";
	private static String APPLICATION1a_NAME = NUMPREFIX1_ATTR_VALUE + "-" + APP_NAME_STRING + "_A" + "-" + WORK_STREAM + "-CURRENT";
	
	private static final String NUMPREFIX2_ATTR_VALUE = "123456";
	private static String APPLICATION2_NAME = NUMPREFIX2_ATTR_VALUE + "-" + APP_NAME_STRING + "-" + WORK_STREAM + "-CURRENT";
	private static String APPLICATION2a_NAME = NUMPREFIX2_ATTR_VALUE + "-" + APP_NAME_STRING + "_A" + "-" + WORK_STREAM + "-CURRENT";
	private static final String NUMPREFIX3_ATTR_VALUE = "2468";
	private static String APPLICATION3_NAME = NUMPREFIX3_ATTR_VALUE + "-" + APP_NAME_STRING + "-" + WORK_STREAM + "-CURRENT";
	private static String EXPECTED_PROJECT_STATUS_VALUE = "cur";
	
	private static String APPLICATION_VERSION = "v123";
	private static String USER1_USERNAME = "JUnit_ccimporter_report_user3_1";
	private static String USER1a_USERNAME = "JUnit_ccimporter_report_user3_1a";
	private static String USER2_USERNAME = "JUnit_ccimporter_report_user3_2";
	private static String USER2a_USERNAME = "JUnit_ccimporter_report_user3_2a";
	private static String USER3_USERNAME = "JUnit_ccimporter_report_user3_3";
	private static String USER_PASSWORD = "password";

	private static List<String> projectIdsToDelete = new ArrayList<String>();
	
    @BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		String configPath = "src/test/resources/numprefixed_numprefix.properties";

        CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(ccConfigManager.getServerBean(), ccConfigManager);
		CodeCenterServerProxyV7_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
    	
		ProtexServerWrapper protexWrapper = new ProtexServerWrapper(protexConfigManager.getServerBean(), protexConfigManager, false);
		
		TestUtils.removeApplication(cc, APPLICATION1_NAME, APPLICATION_VERSION);
		TestUtils.removeApplication(cc, APPLICATION1a_NAME, APPLICATION_VERSION);
		TestUtils.removeApplication(cc, APPLICATION2_NAME, APPLICATION_VERSION);
		TestUtils.removeApplication(cc, APPLICATION2a_NAME, APPLICATION_VERSION);
		TestUtils.removeApplication(cc, APPLICATION3_NAME, APPLICATION_VERSION);
		TestUtils.removeUserFromCc(cc, USER1_USERNAME);
		TestUtils.removeUserFromCc(cc, USER1a_USERNAME);
		TestUtils.removeUserFromCc(cc, USER2_USERNAME);
		TestUtils.removeUserFromCc(cc, USER2a_USERNAME);
		TestUtils.removeUserFromCc(cc, USER3_USERNAME);
		
		for (String projectId : projectIdsToDelete) {
			ProtexTestUtils.deleteProjectById(protexWrapper, projectId);
		}
	}
	

    @Test
    public void testNumPrefix() throws Exception
    {
    	String configPath = "src/test/resources/numprefixed_numprefix.properties";

        CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(ccConfigManager.getServerBean(), ccConfigManager);
		CodeCenterServerProxyV7_0 cc = ccWrapper.getInternalApiWrapper().getProxy();

		ProtexServerWrapper protexWrapper = new ProtexServerWrapper(protexConfigManager.getServerBean(), protexConfigManager, false);
		String projectId = ProtexTestUtils.createProject(protexWrapper, protexConfigManager, APPLICATION1_NAME, "src/test/resources/source");
		projectIdsToDelete.add(projectId);
		
    	TestUtils.createUser(cc, USER1_USERNAME, USER_PASSWORD);
    	ApplicationIdToken appIdToken = TestUtils.createApplication(cc, APPLICATION1_NAME, APPLICATION_VERSION, USER1_USERNAME, TestServerConfig.getCcUserRole2(),
    			TestServerConfig.getCcCustomAttributeTextfield(), "test");
    	Application app = TestUtils.getApplication(cc, appIdToken);
    	CCIApplication cciApp = new CCIApplication(app, false);
		
		CCIProject project = new CCIProject();
		project.setProjectName(APPLICATION1_NAME);
		Date testDateValue = new Date();

		NumericPrefixedAppAdjuster appAdjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		appAdjuster.init(ccWrapper, protexWrapper, ccConfigManager, tz);
		cciApp.setJustCreated(true);
		appAdjuster.adjustApp(cciApp, project);
		
		boolean foundNumPrefixAttr = false;
		Application app2 = cc.getApplicationApi().getApplication(appIdToken);
		List<AttributeValue> attrValues = app2.getAttributeValues();
		for (AttributeValue attrValue : attrValues) {
			AttributeIdToken a = (AttributeIdToken) attrValue.getAttributeId();
			String curAttrName = cc.getAttributeApi().getAttribute(a).getName();
			String curAttrValue = attrValue.getValues().get(0);
			System.out.println("attr name: " + curAttrName + 
				"; value: " + curAttrValue);
			
			if (TestServerConfig.getCcCustomAttributeTextfield().equals(curAttrName)) {
				foundNumPrefixAttr = true;
				assertEquals(NUMPREFIX1_ATTR_VALUE, curAttrValue);
			}
		}
		assertTrue(foundNumPrefixAttr);
    }
    @Test
    public void testNumPrefixOldApp() throws Exception
    {
    	String configPath = "src/test/resources/numprefixed_numprefix.properties";

        CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(ccConfigManager.getServerBean(), ccConfigManager);
		CodeCenterServerProxyV7_0 cc = ccWrapper.getInternalApiWrapper().getProxy();

		ProtexServerWrapper protexWrapper = new ProtexServerWrapper(protexConfigManager.getServerBean(), protexConfigManager, false);
		String projectId = ProtexTestUtils.createProject(protexWrapper, protexConfigManager, APPLICATION1a_NAME, "src/test/resources/source");
		projectIdsToDelete.add(projectId);
		
    	TestUtils.createUser(cc, USER1a_USERNAME, USER_PASSWORD);
    	ApplicationIdToken appIdToken = TestUtils.createApplication(cc, APPLICATION1a_NAME, APPLICATION_VERSION, USER1a_USERNAME, TestServerConfig.getCcUserRole2(),
    			TestServerConfig.getCcCustomAttributeTextfield2(), "test");
    	Application app = TestUtils.getApplication(cc, appIdToken);
    	CCIApplication cciApp = new CCIApplication(app, false);
		
		CCIProject project = new CCIProject();
		project.setProjectName(APPLICATION1a_NAME);

		NumericPrefixedAppAdjuster appAdjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		appAdjuster.init(ccWrapper, protexWrapper, ccConfigManager, tz);
		cciApp.setJustCreated(false);
		appAdjuster.adjustApp(cciApp, project);
		
		boolean foundNumPrefixAttr = false;
		boolean numPrefixSet = false;
		Application app2 = cc.getApplicationApi().getApplication(appIdToken); 
		List<AttributeValue> attrValues = app2.getAttributeValues();
		for (AttributeValue attrValue : attrValues) {
			AttributeIdToken a = (AttributeIdToken) attrValue.getAttributeId();
			String curAttrName = cc.getAttributeApi().getAttribute(a).getName();
			String curAttrValue = attrValue.getValues().get(0);
			System.out.println("attr name: " + curAttrName + 
				"; value: " + curAttrValue);
			
			if (TestServerConfig.getCcCustomAttributeTextfield().equals(curAttrName)) {
				foundNumPrefixAttr = true;
				if (NUMPREFIX1_ATTR_VALUE.equals(curAttrValue)) {
					numPrefixSet = true;
				}
			}
		}
		assertTrue(!foundNumPrefixAttr || !numPrefixSet); // assert that numeric prefix was not set on not-new app
    }
    
    @Test
    public void testAnalyzedDate() throws Exception
    {
    	String configPath = "src/test/resources/numprefixed_analyzeddate.properties";
    	
    	CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(ccConfigManager.getServerBean(), ccConfigManager);
		CodeCenterServerProxyV7_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
		
		ProtexServerWrapper protexWrapper = new ProtexServerWrapper(protexConfigManager.getServerBean(), protexConfigManager, false);
		String projectId = ProtexTestUtils.createProject(protexWrapper, protexConfigManager, APPLICATION2_NAME, "src/test/resources/source");
    	projectIdsToDelete.add(projectId);
    	
    	TestUtils.createUser(cc, USER2_USERNAME, USER_PASSWORD);
    	ApplicationIdToken appIdToken = TestUtils.createApplication(cc, APPLICATION2_NAME, APPLICATION_VERSION, USER2_USERNAME, TestServerConfig.getCcUserRole2(),
    			TestServerConfig.getCcCustomAttributeTextfield(), "test");
    	Application app = TestUtils.getApplication(cc, appIdToken);
    	CCIApplication cciApp = new CCIApplication(app, false);
    	
		CCIProject project = new CCIProject();
		project.setProjectName(APPLICATION2_NAME);
		Date testDateValue = new Date();
		DateFormat df = new SimpleDateFormat("MM-dd-yyyy");

		NumericPrefixedAppAdjuster appAdjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		appAdjuster.init(ccWrapper, protexWrapper, ccConfigManager, tz);
		cciApp.setJustCreated(true);
		appAdjuster.adjustApp(cciApp, project);
		
		boolean foundNumPrefixAttr = false;
		System.out.println("==============\nGetting attr values:");
		Application app2 = cc.getApplicationApi().getApplication(appIdToken);
		List<AttributeValue> attrValues = app2.getAttributeValues();
		for (AttributeValue attrValue : attrValues) {
			AttributeIdToken a = (AttributeIdToken) attrValue.getAttributeId();
			String curAttrName = cc.getAttributeApi().getAttribute(a).getName();
			String curAttrValue = attrValue.getValues().get(0);
			System.out.println("attr name: " + curAttrName + 
				"; value: " + curAttrValue);
			
			if (TestServerConfig.getCcCustomAttributeTextfield().equals(curAttrName)) {
				foundNumPrefixAttr = true;
				assertEquals(df.format(testDateValue), curAttrValue);
			}
		}
		assertTrue(foundNumPrefixAttr);
    }
    
    @Test
    public void testAnalyzedDateOldApp() throws Exception
    {
    	String configPath = "src/test/resources/numprefixed_analyzeddate.properties";
    	
    	CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(ccConfigManager.getServerBean(), ccConfigManager);
		CodeCenterServerProxyV7_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
    	
		ProtexServerWrapper protexWrapper = new ProtexServerWrapper(protexConfigManager.getServerBean(), protexConfigManager, false);
		String projectId = ProtexTestUtils.createProject(protexWrapper, protexConfigManager, APPLICATION2a_NAME, "src/test/resources/source");
		projectIdsToDelete.add(projectId);
		
    	TestUtils.createUser(cc, USER2a_USERNAME, USER_PASSWORD);
    	ApplicationIdToken appIdToken = TestUtils.createApplication(cc, APPLICATION2a_NAME, APPLICATION_VERSION, USER2a_USERNAME, TestServerConfig.getCcUserRole2(),
    			TestServerConfig.getCcCustomAttributeTextfield(), "test");
    	Application app = TestUtils.getApplication(cc, appIdToken);
    	CCIApplication cciApp = new CCIApplication(app, false);
    	
		CCIProject project = new CCIProject();
		project.setProjectName(APPLICATION2a_NAME);
		Date testDateValue = new Date();
		DateFormat df = new SimpleDateFormat("MM-dd-yyyy");

		NumericPrefixedAppAdjuster appAdjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		appAdjuster.init(ccWrapper, protexWrapper, ccConfigManager, tz);
		cciApp.setJustCreated(false);
		appAdjuster.adjustApp(cciApp, project);
		
		boolean foundNumPrefixAttr = false;
		System.out.println("==============\nGetting attr values:");
		Application app2 = cc.getApplicationApi().getApplication(appIdToken);
		List<AttributeValue> attrValues = app2.getAttributeValues();
		for (AttributeValue attrValue : attrValues) {
			AttributeIdToken a = (AttributeIdToken) attrValue.getAttributeId();
			String curAttrName = cc.getAttributeApi().getAttribute(a).getName();
			String curAttrValue = attrValue.getValues().get(0);
			System.out.println("attr name: " + curAttrName + 
				"; value: " + curAttrValue);
			
			if (TestServerConfig.getCcCustomAttributeTextfield().equals(curAttrName)) {
				foundNumPrefixAttr = true;
				assertEquals(df.format(testDateValue), curAttrValue);
			}
		}
		assertTrue(foundNumPrefixAttr);
    }
    
    @Test
    public void testProjectStatus() throws Exception
    {
    	String configPath = "src/test/resources/numprefixed_projectstatus.properties";
    	
    	CodeCenterConfigManager ccConfigManager = ccConfigManager = new CodeCenterConfigManager(configPath);
        ProtexConfigManager protexConfigManager = protexConfigManager = new ProtexConfigManager(configPath);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(ccConfigManager.getServerBean(), ccConfigManager);
		CodeCenterServerProxyV7_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
    	
		ProtexServerWrapper protexWrapper = new ProtexServerWrapper(protexConfigManager.getServerBean(), protexConfigManager, false);
		String projectId = ProtexTestUtils.createProject(protexWrapper, protexConfigManager, APPLICATION3_NAME, "src/test/resources/source");
		projectIdsToDelete.add(projectId);
		
    	TestUtils.createUser(cc, USER3_USERNAME, USER_PASSWORD);
    	ApplicationIdToken appIdToken = TestUtils.createApplication(cc, APPLICATION3_NAME, APPLICATION_VERSION, USER3_USERNAME, TestServerConfig.getCcUserRole2(),
    			TestServerConfig.getCcCustomAttributeTextfield(), "test");
    	Application app = TestUtils.getApplication(cc, appIdToken);
    	CCIApplication cciApp = new CCIApplication(app, false);
    	
		CCIProject project = new CCIProject();
		project.setProjectName(APPLICATION3_NAME);
		Date testDateValue = new Date(TIME_VALUE_OF_JAN1_2000);


		NumericPrefixedAppAdjuster appAdjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		appAdjuster.init(ccWrapper, protexWrapper, ccConfigManager, tz);
		cciApp.setJustCreated(true);
		appAdjuster.adjustApp(cciApp, project);
		
		boolean foundProjectStatus = false;
		System.out.println("==============\nGetting attr values:");
		Application app2 = cc.getApplicationApi().getApplication(appIdToken);
		List<AttributeValue> attrValues = app2.getAttributeValues();
		for (AttributeValue attrValue : attrValues) {
			AttributeIdToken a = (AttributeIdToken) attrValue.getAttributeId();
			String curAttrName = cc.getAttributeApi().getAttribute(a).getName();
			String curAttrValue = attrValue.getValues().get(0);
			System.out.println("attr name: " + curAttrName + 
				"; value: " + curAttrValue);
			
			if (TestServerConfig.getCcCustomAttributeTextfield().equals(curAttrName)) {
				foundProjectStatus = true;
				assertEquals(EXPECTED_PROJECT_STATUS_VALUE, curAttrValue);
			}
		}
		assertTrue(foundProjectStatus);
    }

}
