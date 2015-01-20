package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.*;

import java.util.List;

import org.junit.Test;

import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class ConfigFilePlusCmdLineProjectListTest {

	@Test
	public void test() {

		String[] args = { "src/test/resources/importer_test_all_projects.properties",
				"--project", "p1,p2" };

		CodeCenterConfigManager ccConfigManager = new CodeCenterConfigManager(args[0]);
		ProtexConfigManager protexConfigManager = new ProtexConfigManager(args[0]);
		String projectListString = args[2];
		ccConfigManager.setProjectList(projectListString);
		protexConfigManager.setProjectList(projectListString);
		
		assertEquals("ccImportUser", ccConfigManager.getOwner());
		assertEquals("ccImportUser@blackducksoftware.com", protexConfigManager.getServerBean().getUserName());
		List<CCIProject> projects = protexConfigManager.getProjectList();
		boolean foundP1 = false;
		boolean foundP2 = false;
		for (CCIProject project : projects) {
			if ("p1".equals(project.getProjectName())) 
				foundP1 = true;
			else if ("p2".equals(project.getProjectName())) 
				foundP2 = true;
		}
		assertTrue(foundP1);
		assertTrue(foundP2);
	}

}
