/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Properties;

import soleng.framework.core.config.ConfigurationManager;
import soleng.framework.standard.common.ProjectPojo;
import soleng.framework.standard.protex.ProtexProjectPojo;
import soleng.framework.standard.protex.ProtexServerWrapper;
import soleng.framework.standard.protex.project.SimpleProtexProjectCreator;
import soleng.framework.standard.protex.project.SimpleProtexProjectCreatorImpl;

import com.blackducksoftware.sdk.protex.project.RapidIdentificationMode;
import com.blackducksoftware.sdk.protex.user.User;

public class ProtexTestUtils {
//	public static ConfigurationManager initConfig(String protexUrl, String username, String password) {
//		Properties props = new Properties();
//		props.setProperty("protex.server.name", protexUrl);
//		props.setProperty("protex.user.name", username);
//		props.setProperty("protex.password", password);
//		props.setProperty("cc.database.server.name", CcTestUtils.DB_SERVER);
//		props.setProperty("cc.database.port", CcTestUtils.DB_PORT);
//		props.setProperty("cc.database.user.name", CcTestUtils.DB_USER);
//		props.setProperty("cc.database.password", CcTestUtils.DB_PASSWORD);
//		ConfigurationManager config = new ProtexConfigMgr(props);
//		return config;
//	}
	
//	public static ProtexConfigMgr initConfig(String configFilename) {
//		ProtexConfigMgr config = new ProtexConfigMgr(configFilename);
//		return config;
//	}
	
	public static void confirmByProjectIdAccessLimitedTo(ProtexServerWrapper protexServerWrapper, String projectId, String username) throws Exception {
		// Get the current list of users that have access to the project
		List<User> projectUsers = protexServerWrapper.getInternalApiWrapper().getProjectApi().getProjectUsers(projectId);
		assertEquals(1, projectUsers.size());
		// Make sure the one user is the given/target user
		assertEquals(username, projectUsers.get(0).getUserId());
	}
	
	public static void confirmAccessLimitedTo(ProtexServerWrapper protexServerWrapper, String projectName, String username) throws Exception {
		String projectId = protexServerWrapper.getProjectByName(projectName).getProjectKey();				
		confirmByProjectIdAccessLimitedTo(protexServerWrapper, projectId, username);
	}
	
	public static void confirmProjectExists(ProtexServerWrapper protexServerWrapper, String projectName) throws Exception {
		ProjectPojo projectPojo = protexServerWrapper.getProjectByName(projectName);
		assertEquals(projectName, projectPojo.getProjectName());
	}
	
	public static void enableRapidId(ProtexServerWrapper protexServerWrapper, String projectId) throws Exception {
		protexServerWrapper.getInternalApiWrapper().getProjectApi().updateRapidIdentificationMode(projectId, RapidIdentificationMode.AUTOMATIC_INCLUDE_GLOBAL_CONFIGURATIONS);
	}
	
	public static void confirmByProjectIdRapidIdDisabled(ProtexServerWrapper protexServerWrapper, String projectId) throws Exception {
		assertEquals(RapidIdentificationMode.DISABLED,
				protexServerWrapper.getInternalApiWrapper().getProjectApi().getRapidIdentificationMode(projectId));
	}
	
	public static void confirmRapidIdDisabled(ProtexServerWrapper protexServerWrapper, String projectName) throws Exception {
		String projectId = protexServerWrapper.getProjectByName(projectName).getProjectKey();
		confirmByProjectIdRapidIdDisabled(protexServerWrapper, projectId);
	}
	
	
	
	public static String createProject(ProtexServerWrapper protexServerWrapper, ConfigurationManager config, 
			String projectName, String sourcePath) throws Exception {
		
		SimpleProtexProjectCreator projectCreator = new SimpleProtexProjectCreatorImpl(config, protexServerWrapper);
		
		// create and analyze project
		ProjectPojo projectPojo1 = projectCreator.createProjectAsPojo(projectName, "test", false);
		projectCreator.analyzeProject(projectPojo1, sourcePath);
		return projectPojo1.getProjectKey();
	}
	
	public static void deleteProjectById(ProtexServerWrapper protexServerWrapper, String projectId) {
		try {
			protexServerWrapper.getInternalApiWrapper().getProjectApi().deleteProject(projectId);
		} catch (Exception e) {
			System.out.println("Error deleting project " + projectId + ": " + e.getMessage());
		}
	}
	
	public static String getProjectIdByName(ProtexServerWrapper protexServerWrapper, String projectName) throws Exception {
		return protexServerWrapper.getProjectByName(projectName).getProjectKey();
	}
	
	
	
	public static ProtexServerWrapper<ProtexProjectPojo> initProtexServerWrapper(ConfigurationManager config) throws Exception {
		ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper = 
				new ProtexServerWrapper<ProtexProjectPojo>(config.getServerBean(), config, true);
		return protexServerWrapper;
	}
}
