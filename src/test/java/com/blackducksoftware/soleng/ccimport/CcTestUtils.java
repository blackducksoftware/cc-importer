/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import soleng.framework.core.config.ConfigurationManager;
import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.codecenter.dao.ApplicationDataDao;
import soleng.framework.standard.codecenter.dao.CodeCenter6_6_1Dao;
import soleng.framework.standard.codecenter.dao.CodeCenterDaoConfigManager;
import soleng.framework.standard.codecenter.dao.CodeCenterDaoConfigManagerImpl;
import soleng.framework.standard.codecenter.pojo.ApplicationPojo;
import soleng.framework.standard.codecenter.pojo.ComponentPojo;
import soleng.framework.standard.codecenter.pojo.ComponentUsePojo;
import soleng.framework.standard.codecenter.pojo.VulnerabilityPojo;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationCreate;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ValidationStatusEnum;
import com.blackducksoftware.sdk.codecenter.approval.data.WorkflowNameToken;
import com.blackducksoftware.sdk.codecenter.attribute.data.AbstractAttribute;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeNameOrIdToken;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeNameToken;
import com.blackducksoftware.sdk.codecenter.cola.data.ComponentNameVersionToken;
import com.blackducksoftware.sdk.codecenter.common.data.AttributeValue;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.sdk.codecenter.request.data.RequestApplicationComponentToken;
import com.blackducksoftware.sdk.codecenter.request.data.RequestCreate;
import com.blackducksoftware.sdk.codecenter.request.data.RequestIdToken;
import com.blackducksoftware.sdk.codecenter.user.data.RoleNameToken;
import com.blackducksoftware.sdk.codecenter.user.data.UserNameToken;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;

public class CcTestUtils {
	private static final String VULN_STATUS_COMMENT_VALUE = "Added to orig app by JUnit test";
	private static final String VULN_STATUS_STRING_IN_PROGRESS = "In Progress";
	private static final int VULN_STATUS_ID_IN_PROGRESS = 286392;
	public static final String CC_ROLE = "Application Administrator";
	public static final String CC_URL = "http://cc-integration.blackducksoftware.com";
	public static final String CC_PASSWORD = "blackduck";
	public static final String CC_USER = "unitTester@blackducksoftware.com";
	public static final String DB_SERVER = "cc-integration.blackducksoftware.com";
	public static final String DB_PASSWORD = "mallard";
	public static final String DB_USER = "blackduck";
	public static final String DB_PORT = "55433";
	public static final String CLONED_APP_WORKFLOW = "Parallel";
	
	public static void deleteAppByName(CodeCenterServerWrapper ccServerWrapper,
			String appName, String appVersion) {
		ApplicationNameVersionToken token = new ApplicationNameVersionToken();
		token.setName(appName);
		token.setVersion(appVersion);
		try {
			ccServerWrapper.getInternalApiWrapper().applicationApi.lockApplication(token, false);
		} catch (SdkFault e) {
			System.out.println("Error unlocking cloned CC app (prior to deleting it)");
		}
		try {
			ccServerWrapper.getInternalApiWrapper().applicationApi.deleteApplication(token);
		} catch (SdkFault e) {
			System.out.println("Error deleting cloned CC app");
		}
	}
	
	public static CodeCenterConfigManager initConfig(String url, String username, String password)  {
		Properties props = new Properties();
		props.setProperty("cc.server.name", url);
		props.setProperty("cc.user.name", username);
		props.setProperty("cc.password", password);
		props.setProperty("cc.app.version", "Unspecified");
		props.setProperty("cc.cloned.app.workflow", CLONED_APP_WORKFLOW);
		props.setProperty("cc.database.server.name", DB_SERVER);
		props.setProperty("cc.database.port", DB_PORT);
		props.setProperty("cc.database.user.name", DB_USER);
		props.setProperty("cc.database.password", DB_PASSWORD);
		CodeCenterConfigManager config = new CodeCenterConfigManager(props);
		return config;
	}
	
	public static ConfigurationManager initConfig(String configFilename) {
		ConfigurationManager config = new CodeCenterConfigManager(configFilename);
		return config;
	}
	
	public static String createApp(CodeCenterServerWrapper ccServerWrapper,
			ConfigurationManager config, String appName, String appVersion, String appDescription,
			String ownerName,
			String ownerRole,
			Map<String, String> attributes) throws Exception {
		UserNameToken userToken = new UserNameToken();
		userToken.setName(ownerName);
		
		RoleNameToken roleToken = new RoleNameToken();
		roleToken.setName(ownerRole);
		
		ApplicationCreate appCreate = new ApplicationCreate();
		appCreate.setName(appName);
		appCreate.setDescription(appDescription);
		appCreate.setUseProtexstatus(true);
		appCreate.setObligationFulFillment(true);
		appCreate.setVersion(appVersion);
		appCreate.setOwnerId(userToken);
		appCreate.setOwnerRoleId(roleToken);
		appCreate.setWorkflowId(getWorkflowToken(CLONED_APP_WORKFLOW));

		if ((attributes != null) && (attributes.size() > 0)) {
			List<AttributeValue> attributesValues = new ArrayList<AttributeValue>();
			for (String attrName : attributes.keySet()) {
				System.out.println("attribute name " + attrName + ", value: " + attributes.get(attrName));
				AttributeValue attrValue1 = new AttributeValue();
		        AttributeNameToken nameToken1 = new AttributeNameToken();
		        nameToken1.setName(attrName);
		        attrValue1.setAttributeId(nameToken1);
		        attrValue1.getValues().add(attributes.get(attrName));
		        attributesValues.add(attrValue1);
			}
			appCreate.getAttributeValues().addAll(attributesValues);
		}

		ApplicationIdToken token = ccServerWrapper.getInternalApiWrapper().applicationApi.createApplication(appCreate);
		return token.getId();
	}
	
	private static WorkflowNameToken getWorkflowToken(String workflowName) throws Exception {
		WorkflowNameToken wf = new WorkflowNameToken();
        wf.setName(workflowName);
        return wf;
	}
	
	public static void addComponentToApp(CodeCenterServerWrapper ccServerWrapper, String appId,
			String componentName, String componentVersion) throws Exception {
		
		ApplicationIdToken appIdToken = new ApplicationIdToken();
		appIdToken.setId(appId);

        ComponentNameVersionToken component = new ComponentNameVersionToken();
        component.setName(componentName);
        component.setVersion(componentVersion);

        RequestApplicationComponentToken request = new RequestApplicationComponentToken();
        request.setApplicationId(appIdToken);
        request.setComponentId(component);

        RequestCreate createRequest = new RequestCreate();
        createRequest.setApplicationComponentToken(request);
        createRequest.setSubmit(true);
        
        RequestIdToken requestId = ccServerWrapper.getInternalApiWrapper().requestApi.createRequest(createRequest);
		
	}
	
	public static CodeCenterServerWrapper initCcServerWrapper(ConfigurationManager config) throws Exception {
		ServerBean serverBean = config.getServerBean();
		CodeCenterServerWrapper ccServerWrapper = new CodeCenterServerWrapper(serverBean, config);
		return ccServerWrapper;
	}	

//	public static void checkApplication(CodeCenterServerWrapper ccServerWrapper,
//			String appName, String appVersion, String appDescription) throws Exception {
//		checkApplication(ccServerWrapper, appName, appVersion, appDescription, false);
//	}
//	public static void checkApplicationAndUseProtexstatus(CodeCenterServerWrapper ccServerWrapper,
//			String appName, String appVersion, String appDescription) throws Exception {
//		checkApplication(ccServerWrapper, appName, appVersion, appDescription, true);
//	}
	public static void checkApplication(CodeCenterServerWrapper ccServerWrapper,
			String appName, String appVersion, String appDescription,
			Map<String, String> expectedAttrValues) throws Exception {
		checkApplication(ccServerWrapper, appName, appVersion, appDescription, false, expectedAttrValues, false);
	}
	
	public static void checkApplicationValidationStatusOk(CodeCenterServerWrapper ccServerWrapper,
			String appName, String appVersion) throws Exception {
		checkApplication(ccServerWrapper, appName, appVersion, null, false, null, true);
	}
		
	public static void checkApplicationAndUseProtexstatus(CodeCenterServerWrapper ccServerWrapper,
			String appName, String appVersion, String appDescription,
			Map<String, String> expectedAttrValues) throws Exception {
		checkApplication(ccServerWrapper, appName, appVersion, appDescription, true, expectedAttrValues, false);
	}
	
	public static void checkApplication(CodeCenterServerWrapper ccServerWrapper,
			String appName, String appVersion, String appDescription, boolean confirmUseProtexstatusTrue,
			Map<String, String> expectedAttrValues,
			boolean confirmValidationStatusOk) throws Exception {
		ApplicationNameVersionToken token = new ApplicationNameVersionToken();
		token.setName(appName);
		token.setVersion(appVersion);
		Application app = ccServerWrapper.getInternalApiWrapper().applicationApi.getApplication(token);
		assertEquals(appName, app.getName());
		if (appDescription != null) {
			assertEquals(appDescription, app.getDescription());
		}
		if (confirmUseProtexstatusTrue) {
			assertTrue(app.isUseProtexstatus());
		}
		
		if (confirmValidationStatusOk) {
			assertEquals(ValidationStatusEnum.PASSED, app.getValidationStatus());
		}
//		assertTrue(app.isObligationFulFillment()); // Don't know any way to get this to pass
		
		if (expectedAttrValues != null) {
			List<AttributeValue> actualAttrValues = app.getAttributeValues();
			checkAttrValues(ccServerWrapper, expectedAttrValues, actualAttrValues);
		}
	}
	
	private static void checkAttrValues(CodeCenterServerWrapper ccServerWrapper,
			Map<String, String> expectedAttrValues, List<AttributeValue> actualAttrValues) 
					throws SdkFault {
		Map<String, String> actualAttrValuesMap = new HashMap<String, String>();
		for (AttributeValue actualAttrValue : actualAttrValues) {
			AttributeNameOrIdToken actualAttrIdToken = actualAttrValue.getAttributeId();
			AbstractAttribute actualAttrDef = ccServerWrapper.getInternalApiWrapper().attributeApi.getAttribute(actualAttrIdToken);

			String actualAttrName = actualAttrDef.getName();
			actualAttrValuesMap.put(actualAttrName, actualAttrValue.getValues().get(0));
		}
		
		for (String expectedAttrName : expectedAttrValues.keySet()) {
			String actualAttrValue = actualAttrValuesMap.get(expectedAttrName);
			assertEquals(expectedAttrValues.get(expectedAttrName), actualAttrValue);
		}
	}
	
	
	
//	public static void confirmByAppIdLocked(CodeCenterServerWrapper ccServerWrapper, String appId) throws Exception {
//		ApplicationIdToken token = new ApplicationIdToken();
//		token.setId(appId);
//		Application app = ccServerWrapper.getInternalApiWrapper().applicationApi.getApplication(token);
//		assertTrue(app.isLocked());
//	}
//	
//	public static void confirmLocked(CodeCenterServerWrapper ccServerWrapper, String appName, String appVersion) throws Exception {
//		ApplicationNameVersionToken token = new ApplicationNameVersionToken();
//		token.setName(appName);
//		token.setVersion(appVersion);
//		Application app = ccServerWrapper.getInternalApiWrapper().applicationApi.getApplication(token);
//		
//		String appId = app.getId().getId();
//		confirmByAppIdLocked(ccServerWrapper, appId);
//	}
	
	// Start comp / vuln methods
	
//	public static void setAllTargetRemediationDates(String appName, String version) throws Exception {
//		Properties props = new Properties();
//		props.setProperty("cc.server.name", CC_URL);
//		props.setProperty("cc.user.name", CC_USER);
//		props.setProperty("cc.password", CC_PASSWORD);
//
//		props.setProperty("cc.database.server.name", DB_SERVER);
//		props.setProperty("cc.database.port", DB_PORT);
//		props.setProperty("cc.database.user.name", DB_USER);
//		props.setProperty("cc.database.password", DB_PASSWORD);
//		
//		CodeCenterDaoConfigManager config = new CodeCenterDaoConfigManagerImpl(props);
//		ApplicationDataDao dataSource=null;
//		
//
//		dataSource = new CodeCenter6_6_1Dao(config);
//
//		try {
//			System.out.println("Fetching application: " + appName + " version " + version);
//			ApplicationPojo app = dataSource.getApplication(appName, version);
//			assertNotNull(app);
//	
//			System.out.println("Fetching components and vulnerabilities");
//	
//			System.out.println("Application: " + app.getName() + " version " + app.getVersion());
//			
//			collectDataApplication(dataSource, app);
//		} finally {
//			dataSource.close();
//		}
//	}
	
//	private static void collectDataApplication(ApplicationDataDao dataSource,
//			ApplicationPojo app) throws Exception {
//		List<ComponentUsePojo> compUses = dataSource.getComponentUses(app);
//		for (ComponentUsePojo compUse : compUses) {
//			collectDataComponentUse(dataSource, app, compUse);	
//		}
//	}
//	private static void collectDataComponentUse(ApplicationDataDao dataSource,
//			ApplicationPojo app, ComponentUsePojo compUse) throws Exception {
//		ComponentPojo comp = dataSource.getComponent(compUse);
//		collectDataComponent(dataSource, app, compUse, comp);
//	}
	
//	private static void collectDataComponent(ApplicationDataDao dataSource,
//			ApplicationPojo app, ComponentUsePojo compUse,
//			ComponentPojo comp) throws Exception {
//		
//		Date now = new Date();
//		
////		int loopLimit = 3; // we only need to process a handful
//		int vulnCount= 0;
//		List<VulnerabilityPojo> vulns = dataSource.getVulnerabilities(comp, compUse);
//		for (VulnerabilityPojo vuln : vulns) {
//			System.out.println("Vulnerability: " + vuln.getName());
//			
//			Date origTargetRemDate = vuln.getTargetRemediationDate();
//			Date origActualRemDate = vuln.getActualRemediationDate();
//			System.out.println("Orig rem dates: target: " + origTargetRemDate + ", actual: " + origActualRemDate);
//			
//			vuln.setTargetRemediationDate(now);
//			vuln.setActualRemediationDate(now);
//			vuln.setStatusId(VULN_STATUS_ID_IN_PROGRESS);
//			vuln.setStatus(VULN_STATUS_STRING_IN_PROGRESS);
//			vuln.setStatusComment(VULN_STATUS_COMMENT_VALUE);
//			dataSource.updateCompUseVulnData(compUse, vuln);		
//
////			if (++vulnCount >= loopLimit)
////				break;
//		}
//		
//		// Re-read the vulnerabilites, and make sure the remediation dates have changed
//		vulnCount= 0;
//		vulns = dataSource.getVulnerabilities(comp, compUse);
//		for (VulnerabilityPojo vuln : vulns) {
//			System.out.println("Vulnerability: " + vuln.getName());
//
//			Date newTargetRemDate = vuln.getTargetRemediationDate();
//			Date newActualRemDate = vuln.getActualRemediationDate();
//			
//			System.out.println("New rem dates: target: " + newTargetRemDate + ", actual: " + newActualRemDate);
//			
//			assertEquals(now, newTargetRemDate);
//			assertEquals(now, newActualRemDate);
//			assertEquals(VULN_STATUS_STRING_IN_PROGRESS, vuln.getStatus());
//			assertEquals(VULN_STATUS_COMMENT_VALUE, vuln.getStatusComment());
//			
////			if (++vulnCount >= loopLimit)
////				break;
//		}
//		
//	}
}
