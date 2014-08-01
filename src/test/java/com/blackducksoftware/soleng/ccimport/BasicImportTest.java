package com.blackducksoftware.soleng.ccimport;


import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

import soleng.framework.core.config.ConfigConstants.APPLICATION;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.protex.ProtexServerWrapper;

/**
 * Tests the import step against a pre-configured pair of CC/Protex servers This
 * is an end-to-end test using a config file
 * 
 * @author akamen
 * 
 */
public class BasicImportTest {

	private static Logger log = LoggerFactory
			    .getLogger(BasicImportTest.class.getName());

	
	public static String testPropsForBasicImport = "importer_test.properties";
	public static String fullLocation = ClassLoader.getSystemResource(
			testPropsForBasicImport).getFile();

	private static CodeCenterConfigManager ccConfig = null;
	private static CodeCenterServerWrapper ccsw = null;

	private static CCIProjectImporterHarness cciHarness = null;

	/** The exception. */
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@BeforeClass
	static public void setUpBeforeClass() throws Exception 
	{
		try{
			// Create these so that we can peform cleanup tasks
			ccConfig = new CodeCenterConfigManager(fullLocation);
			ccsw = new CodeCenterServerWrapper(ccConfig.getServerBean(), ccConfig);
		} catch (Exception e)
		{
			Assert.fail(e.getMessage());
		}
		
		cciHarness = new CCIProjectImporterHarness();

	}

	@Test
	public void testBasicImport()
	{
		// This is the config case
		String[] args = new String[]{fullLocation};
		try{
			// Before running the import, make sure to clean up. 
			cleanupProjectsBeforeImport();		
			cciHarness.main(args);
		}
		catch (Exception e)
		{
			Assert.fail(e.getMessage());
		}
	}

	private void cleanupProjectsBeforeImport()
	{
		log.info("Cleaning up projects before testing import");
		
		try{
			List<CCIProject> projects = ccConfig.getProjectList();
			ApplicationPageFilter apf = new ApplicationPageFilter();
			apf.setFirstRowIndex(0);
			apf.setLastRowIndex(1);
			for(CCIProject project : projects)
			{
				List<Application> applications = 
						ccsw.getInternalApiWrapper().applicationApi.searchApplications(project.getProjectName(), apf);
				
				if(applications.size() == 0)
				{
					log.info("Nothing to cleanup!");
					return;
				}
				else
				{
					// Delete it
					Application appToDelete = applications.get(0);
					ccsw.getInternalApiWrapper().applicationApi.deleteApplication(appToDelete.getId());
					log.info("Deleted application {} as part of cleanup", project);
				}
			}

		} catch (Exception e)
		{
			log.error("Failure during cleanup!", e);
			Assert.fail();
		}
		
	}
	
}
