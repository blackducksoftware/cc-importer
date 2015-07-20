/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import soleng.framework.core.config.ConfigurationManager;
import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

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
import com.blackducksoftware.sdk.codecenter.role.data.RoleNameToken;
import com.blackducksoftware.sdk.codecenter.user.data.UserNameToken;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;

public class CcTestUtils {
	
	public static void deleteAppByName(CodeCenterServerWrapper ccServerWrapper,
			String appName, String appVersion) {
		ApplicationNameVersionToken token = new ApplicationNameVersionToken();
		token.setName(appName);
		token.setVersion(appVersion);
		try {
			ccServerWrapper.getInternalApiWrapper().getApplicationApi().lockApplication(token, false);
		} catch (SdkFault e) {
			System.out.println("Error unlocking cloned CC app (prior to deleting it)");
		}
		try {
			ccServerWrapper.getInternalApiWrapper().getApplicationApi().deleteApplication(token);
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
		props.setProperty("cc.cloned.app.workflow", TestServerConfig.getCcWorkflow());
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
		appCreate.setWorkflowId(getWorkflowToken(TestServerConfig.getCcWorkflow()));

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

		ApplicationIdToken token = ccServerWrapper.getInternalApiWrapper().getApplicationApi().createApplication(appCreate);
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
        
        ccServerWrapper.getInternalApiWrapper().getRequestApi().createRequest(createRequest);
	}
	
	public static CodeCenterServerWrapper initCcServerWrapper(ConfigurationManager config) throws Exception {
		ServerBean serverBean = config.getServerBean();
		CodeCenterServerWrapper ccServerWrapper = new CodeCenterServerWrapper(serverBean, config);
		return ccServerWrapper;
	}	

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
		Application app = ccServerWrapper.getInternalApiWrapper().getApplicationApi().getApplication(token);
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
		
		if (expectedAttrValues != null) {
			List<AttributeValue> actualAttrValues = app.getAttributeValues();
			checkAttrValues(ccServerWrapper, expectedAttrValues, actualAttrValues);
		}
	}
	
	public static void checkApplicationDoesNotExist(CodeCenterServerWrapper ccServerWrapper,
			String appName, String appVersion) throws SdkFault {
		
		ApplicationNameVersionToken token = new ApplicationNameVersionToken();
		token.setName(appName);
		token.setVersion(appVersion);
		try {
			Application app = ccServerWrapper.getInternalApiWrapper().getApplicationApi().getApplication(token);
			fail("Application " + app.getName() + " / " + app.getVersion() + " should not exist");
		} catch (SdkFault e) {
			System.out.println("As expected, got error checking on app that should not exist: " + e.getMessage());
		}
		
	}
	
	private static void checkAttrValues(CodeCenterServerWrapper ccServerWrapper,
			Map<String, String> expectedAttrValues, List<AttributeValue> actualAttrValues) 
					throws SdkFault {
		Map<String, String> actualAttrValuesMap = new HashMap<String, String>();
		for (AttributeValue actualAttrValue : actualAttrValues) {
			AttributeNameOrIdToken actualAttrIdToken = actualAttrValue.getAttributeId();
			AbstractAttribute actualAttrDef = ccServerWrapper.getInternalApiWrapper().getAttributeApi().getAttribute(actualAttrIdToken);

			String actualAttrName = actualAttrDef.getName();
			actualAttrValuesMap.put(actualAttrName, actualAttrValue.getValues().get(0));
		}
		
		for (String expectedAttrName : expectedAttrValues.keySet()) {
			String actualAttrValue = actualAttrValuesMap.get(expectedAttrName);
			assertEquals(expectedAttrValues.get(expectedAttrName), actualAttrValue);
		}
	}
}
