/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import soleng.framework.connector.protex.ProtexServerWrapper;
import soleng.framework.connector.protex.identification.DeclareIdentifier;
import soleng.framework.connector.protex.identification.Identifier;
import soleng.framework.connector.protex.identification.ProtexIdUtils;
import soleng.framework.connector.protex.project.SimpleProtexProjectCreatorImpl;
import soleng.framework.core.config.ConfigurationManager;
import soleng.framework.standard.common.ProjectPojo;
import soleng.framework.standard.protex.ProtexProjectPojo;
import soleng.framework.standard.protex.project.SimpleProtexProjectCreator;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.project.codetree.CodeTreeNode;
import com.blackducksoftware.sdk.protex.project.codetree.PartialCodeTree;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.CodeMatchDiscovery;
import com.blackducksoftware.sdk.protex.project.codetree.discovery.Discovery;
import com.blackducksoftware.sdk.protex.user.User;

public class ProtexTestUtils {
	private static final int CHUNK_SIZE = 7;
	
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
	
//	public static void enableRapidId(ProtexServerWrapper protexServerWrapper, String projectId) throws Exception {
//		protexServerWrapper.getInternalApiWrapper().getProjectApi().updateRapidIdentificationMode(projectId, RapidIdentificationMode.AUTOMATIC_INCLUDE_GLOBAL_CONFIGURATIONS);
//	}
	
//	public static void confirmByProjectIdRapidIdDisabled(ProtexServerWrapper protexServerWrapper, String projectId) throws Exception {
//		assertEquals(RapidIdentificationMode.DISABLED,
//				protexServerWrapper.getInternalApiWrapper().getProjectApi().getRapidIdentificationMode(projectId));
//	}
	
//	public static void confirmRapidIdDisabled(ProtexServerWrapper protexServerWrapper, String projectName) throws Exception {
//		String projectId = protexServerWrapper.getProjectByName(projectName).getProjectKey();
//		confirmByProjectIdRapidIdDisabled(protexServerWrapper, projectId);
//	}
	
	
	
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
	
	public static void makeSomeMatches(ConfigurationManager config,
			String projectName, boolean includeBomRefresh) throws Exception {		
		Identifier identifier = new DeclareIdentifier("test");
		ProtexIdUtils idUtils = new ProtexIdUtils(config, identifier,
				projectName,
				includeBomRefresh);
		identifier.setProtexUtils(idUtils);
		makeIdentifications(idUtils);
	}
	
	
	private static void makeIdentifications(ProtexIdUtils codeMatchUtils) throws Exception {

		String path = "/";
		PartialCodeTree fullTreeFiles = 
				codeMatchUtils.getAllCodeTreeFiles(path);
		
		PartialCodeTree rootDir = 
				codeMatchUtils.getCodeTreeDir(path);
	
		boolean keepGoing = codeMatchUtils.hasPendingIds(rootDir);
		while (keepGoing) {
			doIt(codeMatchUtils, path, fullTreeFiles);
			keepGoing = codeMatchUtils.isMultiPassIdStrategy() && codeMatchUtils.hasPendingIds(rootDir);
		}
		codeMatchUtils.refreshBom();
	}
	
	private static void doIt(ProtexIdUtils codeMatchUtils, String path, PartialCodeTree fullTreeFiles) throws SdkFault {
		List<CodeTreeNode> fileNodes = fullTreeFiles.getNodes();
		List<CodeMatchDiscovery> codeMatchDiscoveries = collectDiscoveries(codeMatchUtils,
				path, fileNodes);
		Map<String, List<CodeMatchDiscovery>> discoveryListMap = 
				getDiscoveriesListsOrganizedByFilePath(codeMatchDiscoveries);
		
		for (String filePath : discoveryListMap.keySet()) {
			Discovery target = ProtexIdUtils.bestMatch(discoveryListMap.get(filePath));
			
			if (target != null) {
				codeMatchUtils.makeId(filePath, target);
			}
		}
	}
	
	private static List<CodeMatchDiscovery> collectDiscoveries(ProtexIdUtils codeMatchUtils, 
			String path,
			List<CodeTreeNode> fileNodes) throws SdkFault {
		
		List<CodeMatchDiscovery> codeMatchDiscoveriesAll = new ArrayList<CodeMatchDiscovery>(1024);
		
		int startIndex = 0;
		int endIndex;
		int listSize = fileNodes.size();
		
		while ((endIndex = getEndIndex(startIndex, listSize, CHUNK_SIZE)) > startIndex) {
			List<CodeTreeNode> fileNodesChunk = fileNodes.subList(startIndex, endIndex+1);
			List<CodeMatchDiscovery> codeMatchDiscoveriesChunk = codeMatchUtils.getCodeMatchDiscoveries(path, fileNodesChunk);
			codeMatchDiscoveriesAll.addAll(codeMatchDiscoveriesChunk);
			startIndex = endIndex+1;
		}
		
		return codeMatchDiscoveriesAll;
	}
	
	public static int getEndIndex(int startIndex, int listSize, int chunkSize) {
		if (listSize == 0) {
			return 0;
		}
		int endIndex = startIndex + chunkSize - 1;
		if (endIndex > (listSize-1)) {
			endIndex = listSize-1;
		}
		return endIndex;
	}
	
	/**
	 * Given a list of discoveries spanning many files, return a map of discovery lists, keys = file path.
	 * @param codeMatchDiscoveries
	 * @return
	 */
	private static Map<String, List<CodeMatchDiscovery>> getDiscoveriesListsOrganizedByFilePath(List<CodeMatchDiscovery> codeMatchDiscoveries) {
		Map<String, List<CodeMatchDiscovery>> map = new HashMap<String, List<CodeMatchDiscovery>>(CHUNK_SIZE);

		for (CodeMatchDiscovery disco : codeMatchDiscoveries) {
			String filePath = disco.getFilePath();
			List<CodeMatchDiscovery> discoveryList;
			if (map.containsKey(filePath)) {
				discoveryList = map.get(filePath);
			} else {
				discoveryList = new ArrayList<CodeMatchDiscovery>(20);
				map.put(filePath, discoveryList);
			}
			discoveryList.add(disco);
		}
		
		return map;
	}
}
