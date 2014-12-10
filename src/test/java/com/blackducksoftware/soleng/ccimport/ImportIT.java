package com.blackducksoftware.soleng.ccimport;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.AfterClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.sdk.codecenter.application.data.Project;
import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV6_6_0;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimport.report.CCIReportSummary;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

import soleng.framework.core.config.ConfigConstants.APPLICATION;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.protex.ProtexProjectPojo;
import soleng.framework.standard.protex.ProtexServerWrapper;

/**
 * TODO: Make this an intergration test that is only run during 'install' phase
 * Tests the import step against a pre-configured pair of CC/Protex servers This
 * is an end-to-end test using a config file
 * 
 * This tests single protex server mode only
 * 
 * @author akamen
 * 
 */
public class ImportIT {
	private static final String APP_VERSION = "Unspecified";
	private static final String APP_NAME = "ccimport IT app";
	private static final String APP_OWNER = "unitTester@blackducksoftware.com";
	private static final String ROLE = "Application Administrator";
	
	private static Logger log = LoggerFactory.getLogger(ImportIT.class
			.getName());

	private static CodeCenterConfigManager ccConfig = null;
	private static CodeCenterServerWrapper ccsw = null;
	private static ProtexServerWrapper<ProtexProjectPojo> psw = null;
	private static ProtexConfigManager pConfig = null;
	
	private static String projectId = null;

	private static CCISingleServerProcessor processor = null;

	/** The exception. */
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@BeforeClass
	static public void setUpBeforeClass() throws Exception {
		try {
			Properties props = new Properties();
			props.setProperty("protex.server.name", "http://se-menger.blackducksoftware.com");
			props.setProperty("protex.user.name", "ccImportUser@blackducksoftware.com");
			props.setProperty("protex.password", "blackduck");
			props.setProperty("cc.server.name", "http://cc-integration/");
			props.setProperty("cc.user.name", "ccImportUser");
			props.setProperty("cc.password", "blackduck");
			props.setProperty("protex.password.isplaintext", "true");
			props.setProperty("cc.password.isplaintext", "true");
			props.setProperty("cc.protex.name", "Menger");
			props.setProperty("cc.default.app.version", APP_VERSION);
			props.setProperty("cc.workflow", "Serial");
			props.setProperty("cc.owner", APP_OWNER);
			props.setProperty("protex.project.list", APP_NAME);
			props.setProperty("validate.application", "true");
			props.setProperty("cc.submit.request", "true");
			props.setProperty("validate.requests.delete", "true");
			
			ccConfig = new CodeCenterConfigManager(props);
			pConfig = new ProtexConfigManager(props);

			// Create cc wrapper so that we can perform cleanup tasks
			ccsw = new CodeCenterServerWrapper(ccConfig.getServerBean(),
					ccConfig);
			psw = new ProtexServerWrapper<ProtexProjectPojo>(pConfig.getServerBean(),
					pConfig, true);

			processor = new CCISingleServerProcessor(ccConfig, pConfig);
			
//			CcTestUtils.createApp(ccsw, ccConfig, APP_NAME, APP_VERSION, "test", APP_OWNER, ROLE, null);
			projectId = ProtexTestUtils.createProject(psw, pConfig, APP_NAME, "src/test/resources/source");

		} catch (Exception e) {
			Assert.fail(e.getMessage());
		}

	}
	
	@AfterClass
	static public void tearDownAfterClass() throws Exception {
		CcTestUtils.deleteAppByName(ccsw, APP_NAME, APP_VERSION);
		ProtexTestUtils.deleteProjectById(psw, projectId);
	}

	@Test
	public void test() {

		try {
			List<CCIProject> projects = ccConfig.getProjectList();
			// Before running the import, make sure to clean up.
			cleanupProjectsBeforeImport(projects);

			// Run the sync
			processor.performSynchronize();

			// Now check to see if the application(s) actually exists.
			boolean exists = doesCcApplicationExist(projects);
			Assert.assertEquals(true, exists);

		} catch (CodeCenterImportException e) {
			Assert.fail(e.getMessage());
		}
	}

	/**
	 * CHecks to see if that list of applications exist and have associations.
	 * 
	 * @param projects
	 */
	private boolean doesCcApplicationExist(List<CCIProject> projects) {
		boolean exists = false;
		try {
			for (CCIProject project : projects) {
				ApplicationPageFilter apf = new ApplicationPageFilter();
				apf.setFirstRowIndex(0);
				apf.setLastRowIndex(1);

				List<Application> applications = ccsw.getInternalApiWrapper().applicationApi
						.searchApplications(project.getProjectName(), apf);

				for (Application app : applications) {
					// No errors here guarantees existence
					ApplicationIdToken token = app.getId();
					Project associatedProject = ccsw.getInternalApiWrapper().applicationApi
							.getAssociatedProtexProject(token);

					String associatedProjectName = associatedProject.getName();
					log.info(
							"Found association of application {} to Protex project {}",
							app.getName() + ":" + app.getVersion(),
							associatedProjectName);

				}
			}

			exists = true;
		} catch (Exception e) {
			log.error("Error during application verification: "
					+ e.getMessage());
		}

		return exists;
	}

	/**
	 * Deletes the specific applications that we are testing with.
	 * 
	 * @param projects
	 */
	private void cleanupProjectsBeforeImport(List<CCIProject> projects) {
		log.info("Cleaning up projects before testing import");

		try {

			for (CCIProject project : projects) {
				ApplicationNameVersionToken token = new ApplicationNameVersionToken();
				token.setName(project.getProjectName());
				token.setVersion(project.getProjectVersion());
				Application appToDelete = ccsw.getInternalApiWrapper().applicationApi
						.getApplication(token);

				if (appToDelete == null) {
					log.info("Nothing to cleanup!");
					return;
				} else {
					// Delete it
					ccsw.getInternalApiWrapper().applicationApi
							.deleteApplication(appToDelete.getId());
					log.info("Deleted application [{}] as part of cleanup",
							project);
				}
			}

		} catch (Exception e) {
			log.warn("Failure during cleanup!: " + e.getMessage());
		}

	}

}
