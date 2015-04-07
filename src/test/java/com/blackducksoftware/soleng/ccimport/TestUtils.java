/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationCreate;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationUpdate;
import com.blackducksoftware.sdk.codecenter.attribute.data.AbstractAttribute;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeGroup;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeGroupPageFilter;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeGroupTypeEnum;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeNameToken;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributePageFilter;
import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV7_0;
import com.blackducksoftware.sdk.codecenter.common.data.AttributeValue;
import com.blackducksoftware.sdk.codecenter.common.data.UserRolePageFilter;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.sdk.codecenter.role.data.ApplicationRoleAssignment;
import com.blackducksoftware.sdk.codecenter.role.data.RoleNameOrIdToken;
import com.blackducksoftware.sdk.codecenter.role.data.RoleNameToken;
import com.blackducksoftware.sdk.codecenter.user.data.User;
import com.blackducksoftware.sdk.codecenter.user.data.UserCreate;
import com.blackducksoftware.sdk.codecenter.user.data.UserNameOrIdToken;
import com.blackducksoftware.sdk.codecenter.user.data.UserNameToken;

public class TestUtils {
	public static final Long CONNECTION_TIMEOUT = 120 * 1000L;
	public static final String REQUIRED_ATTRNAME = "Sample Textfield";

	public static ApplicationIdToken createApplication(CodeCenterServerProxyV7_0 cc, String appName,
			String appVersion, 
			String applicationOwner,
			String userRole,
			String attrName,
			String attrValue) throws SdkFault {
		System.out.println("Creating application " + appName + " / " + appVersion);
		
		UserNameToken owner = new UserNameToken();
        owner.setName(applicationOwner);
        
		ApplicationCreate appCreateBean = new ApplicationCreate();
		appCreateBean.setName(appName);
		appCreateBean.setVersion(appVersion);
		appCreateBean.setOwnerId(owner);
		AttributeValue attrValueObject = new AttributeValue();
		AttributeNameToken attrNameToken = new AttributeNameToken();
		attrNameToken.setName(attrName);
		attrValueObject.setAttributeId(attrNameToken);
		attrValueObject.getValues().add(attrValue);
		appCreateBean.getAttributeValues().add(attrValueObject);
		
		RoleNameToken roleToken = new RoleNameToken();
		roleToken.setName(userRole);
		appCreateBean.setOwnerRoleId(roleToken);
		
		return cc.getApplicationApi().createApplication(appCreateBean);
	}
	
	public static Application getApplication(CodeCenterServerProxyV7_0 cc, ApplicationIdToken idToken) throws SdkFault {
		return cc.getApplicationApi().getApplication(idToken);
	}
	
	public static void setAppCustomAttributeTextField(CodeCenterServerProxyV7_0 cc, Application app, String attrName, String attrValue)
			 throws SdkFault {
		
		System.out.println("Setting app custom attr text field");
//		AttributeGroupPageFilter attrGroupPageFilter = new AttributeGroupPageFilter();
//		List<AttributeGroup> attrGroups =
//				cc.getAttributeApi().searchAttributeGroups("HasTextField", 
//						AttributeGroupTypeEnum.APPLICATION, attrGroupPageFilter);
//		System.out.println("*** Found " + attrGroups.size() + " matching attribute groups");
//		AttributePageFilter attrPageFilter = new AttributePageFilter();
//		attrPageFilter.setFirstRowIndex(0);
//		attrPageFilter.setLastRowIndex(Integer.MAX_VALUE);
//		List<AbstractAttribute> attrs = cc.getAttributeApi().searchAttributes("NonRequiredTextField", attrPageFilter);
//		for (AbstractAttribute attr : attrs) {
//			System.out.println("attr: " + attr.getName());
//			if (attr.getName().equals(attrName)) {
//				System.out.println("*** Found attr");
////				AttributeValue attrValue = new AttributeValue();
////				attrValue.
////				app.getAttributeValues().add(attrValue);
//			}
//		}
		
		List<AttributeValue> attributesValues = new ArrayList<AttributeValue>();
        AttributeValue attrValueObject = new AttributeValue();
        AttributeNameToken nameToken1 = new AttributeNameToken();
        nameToken1.setName(attrName);
        attrValueObject.setAttributeId(nameToken1);
        attrValueObject.getValues().add(attrValue);
        attributesValues.add(attrValueObject);
        
        ApplicationUpdate appUpdate = new ApplicationUpdate();
        appUpdate.setId(app.getId());
        appUpdate.getAttributeValues().addAll(attributesValues);
        
        cc.getApplicationApi().updateApplication(appUpdate);
        System.out.println("Updated app " + app.getName() + " with attr " + attrName + " value " + attrValue);
        
	}
	
	public static void removeApplication(CodeCenterServerProxyV7_0 cc, String appName, String appVersion) {
		System.out.println("Removing application " + appName + " / " + appVersion);
		
		ApplicationNameVersionToken appToken = new ApplicationNameVersionToken();
		appToken.setName(appName);
		appToken.setVersion(appVersion);
		
		try {
		cc.getApplicationApi().deleteApplication(appToken);
		} catch (SdkFault e) {
			System.out.println("Unable to remove application " + appName + " / " + appVersion);
		}
	}
	
	public static ApplicationIdToken createApplicationWithAttrValue(CodeCenterServerProxyV7_0 cc,
			String applicationName, String applicationVersion,
			String applicationOwner, String applicationRole,
			String attrName, String attrValue) throws SdkFault {
		
		System.out.println("Creating application " + applicationName + " / " + applicationVersion +
				" with attr " + attrName + " = " + attrValue);

            // Create the application create bean
            ApplicationCreate appBean = new ApplicationCreate();
            appBean.setName(applicationName);
            appBean.setVersion(applicationVersion);
            UserNameToken owner = new UserNameToken();
            owner.setName(applicationOwner);
            appBean.setOwnerId(owner);
            RoleNameToken role = new RoleNameToken();
            role.setName(applicationRole);
            appBean.setOwnerRoleId(role);

            // Set the required default Attribute values based on the default values and name
            List<AttributeValue> attributesValues = new ArrayList<AttributeValue>();
            AttributeValue attrValue1 = new AttributeValue();
            AttributeNameToken attrNameToken = new AttributeNameToken();
            attrNameToken.setName(attrName);
            attrValue1.setAttributeId(attrNameToken);
            attrValue1.getValues().add(attrValue);
            attributesValues.add(attrValue1);

            // set attributes values.
            appBean.getAttributeValues().addAll(attributesValues);

            ApplicationIdToken appId = null;

            appId = cc.getApplicationApi().createApplication(appBean);
            assertNotNull(appId);

            System.out.println("App created: " + applicationName + " ID: " + appId.getId());

            return appId;
	}
	
	public static boolean isUserAssignedToApp(CodeCenterServerProxyV7_0 cc, String appName, String appVersion,
			String user) throws SdkFault {
		
		ApplicationNameVersionToken appToken = new ApplicationNameVersionToken();
		appToken.setName(appName);
		appToken.setVersion(appVersion);
		
		UserRolePageFilter filter = new UserRolePageFilter();
		filter.setFirstRowIndex(0);
		filter.setLastRowIndex(Integer.MAX_VALUE);
		List<ApplicationRoleAssignment> roles = cc.getApplicationApi().searchUserInApplicationTeam(appToken, user, filter);
		if (roles.size() > 0) {
			return true;
		}
		return false;
		
	}
	
	public static void removeUserFromAppAndCc(CodeCenterServerProxyV7_0 cc, String appName, String appVersion,
			String user,
			String[] userRolesToRemove) throws Exception {
		System.out.println("Removing user " + user + " from app " + appName +
				" and from CC");
		
		ApplicationNameVersionToken appToken = new ApplicationNameVersionToken();
		appToken.setName(appName);
		appToken.setVersion(appVersion);
		
		UserNameToken userToken = new UserNameToken();
		userToken.setName(user);
		
		for (String userRoleToRemove : userRolesToRemove) {
		
			RoleNameToken roleToken = new RoleNameToken();
			roleToken.setName(userRoleToRemove);
			
			try {
				cc.getApplicationApi().removeUserInApplicationTeam(appToken, userToken, roleToken);
			} catch (SdkFault e) {
				System.out.println("Unable to remove user " + user + " from application " + appName +
						" version " + appVersion +
						": " + e.getMessage());
			}
		}
		try {
			cc.getUserApi().deleteUser(userToken);
		} catch (SdkFault e) {
			System.out.println("Unable to delete user " + user + ": " + e.getMessage());
		}
	}
	
	public static void addUserToApplication(CodeCenterServerProxyV7_0 cc, String appName, String appVersion,
			String username, String role) throws Exception {
		
		ApplicationNameVersionToken appToken = new ApplicationNameVersionToken();
		appToken.setName(appName);
		appToken.setVersion(appVersion);

		List<UserNameOrIdToken> userNameTokens = new ArrayList<UserNameOrIdToken>();
		List<RoleNameOrIdToken> applicationRoleIds = new ArrayList<RoleNameOrIdToken>();
		RoleNameToken roleNameToken = new RoleNameToken();
		roleNameToken.setName(role);
		applicationRoleIds.add(roleNameToken);

			
		UserNameToken userNameToken = new UserNameToken();
		userNameToken.setName(username);
		userNameTokens.add(userNameToken);
		

		User u = cc.getUserApi().getUser(userNameToken);
		if (u == null) {
			throw new Exception("User " + username + " does not exist");
		}

		cc.getApplicationApi().addUserToApplicationTeam(appToken, userNameTokens, 
				applicationRoleIds);
	}
	
	public static void removeUserFromCc(CodeCenterServerProxyV7_0 cc, String userName) {
		System.out.println("Removing user " + userName);
		UserNameToken userToken = new UserNameToken();
		userToken.setName(userName);
		try {
			cc.getUserApi().deleteUser(userToken);
		} catch (SdkFault e) {
			System.out.println("Unable to delete user " + userName + ": " + e.getMessage());
		}
	}
	
	public static void createUser(CodeCenterServerProxyV7_0 cc, String username, String password) throws SdkFault {
		System.out.println("Creating user " + username);
		
        UserCreate uc = new UserCreate();
	    uc.setName(username);
	    uc.setActive(true);
	    uc.setPassword(password);
	    cc.getUserApi().createUser(uc);
	}
	
	public static Properties configUserCreatorWithUserAppRoleMappingFile(String role,
			String url, String username, String password,
			String appName, String appVersion) {
		Properties props = createCommonProperties(url, username, password, appVersion, role);
		props.setProperty("path", "src/test/resources/testusers.txt");
		return props;
	}
	
	public static Properties configUserCreatorForLobAdjustMode(String role, 
			String url, String username, String password, String appVersion) {
		Properties props = createCommonPropertiesForLobAdjustMode(url, username, password, role, appVersion);
		return props;
	}
	
	public static Properties configUserCreatorWithoutUserAppRoleMappingList(String role,
			String url, String username, String password,
			String appName,
			String appVersion, String[] usersToAdd) {
		Properties props = createCommonProperties(url, username, password, appVersion, role);
		props.setProperty("app.name", appName);
		String usersToAddString = "";
		for (int i=0; i < usersToAdd.length; i++) {
			usersToAddString += usersToAdd[i];
			if ((i+1) < usersToAdd.length) {
				usersToAddString += ";";
			}
		}
		props.setProperty("add.user.request", usersToAddString);
		return props;
	}
	
	private static Properties createCommonProperties(String url, String username, String password,
			String appVersion, String role) {
		Properties props = new Properties();
		props.setProperty("cc.server.name", url);
		props.setProperty("cc.user.name", username);
		props.setProperty("cc.password", password);
		props.setProperty("app.version", appVersion);
		props.setProperty("user.role", role);
		return props;
	}
	
	private static Properties createCommonPropertiesForLobAdjustMode(String url, String username, String password,
			String role, String appVersion) {
		Properties props = new Properties();
		props.setProperty("lob.adjust.mode", "true");
		props.setProperty("app.version", appVersion);
		props.setProperty("cc.server.name", url);
		props.setProperty("cc.user.name", username);
		props.setProperty("cc.password", password);
		props.setProperty("user.role", role);
		return props;
	}
	
	
//	private static Properties configUserCreator(boolean withUserDataFile, String role,
//			String url, String username, String password,
//			String appName, String appVersion, String[] usersToAdd) {
//		Properties props = new Properties();
//		props.setProperty("cc.server.name", url);
//		props.setProperty("cc.user.name", username);
//		props.setProperty("cc.password", password);
//		
//		if (withUserDataFile) {
//			props.setProperty("user.app.role.mapping.path", "src/test/resources/testusers.txt");
//		} else {
//			props.setProperty("app.name", appName);
//			String usersToAddString = "";
//			for (int i=0; i < usersToAdd.length; i++) {
//				usersToAddString += usersToAdd[i];
//				if ((i+1) < usersToAdd.length) {
//					usersToAddString += ";";
//				}
//			}
//			props.setProperty("add.user.request", usersToAddString);
//		}
//		
//		props.setProperty("app.version", appVersion);
//		props.setProperty("user.role", role);
//		return props;
//	}

}
