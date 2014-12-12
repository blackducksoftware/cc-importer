package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
import com.blackducksoftware.soleng.ccimport.appadjuster.custom.MockAppAdjuster;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimport.report.CCIReportSummary;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

import soleng.framework.core.config.ConfigurationManager;
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
	private static final String CUSTOM_ATTR = "Sample Textfield";
//	private static final String CUSTOM_ATTR = "Product Name";

		private static final String WORKFLOW = "Serial";
//	private static final String WORKFLOW = "Application Build Workflow";
	
	private static final String APP_VERSION = "Unspecified";
	private static final String APP_SEALID = "123456";
	private static final String APP_NAME = APP_SEALID + "-test-PROD-CURRENT";
	
	private static final String APP_OWNER = "unitTester@blackducksoftware.com";
//	private static final String APP_OWNER = "sbillings@blackducksoftware.com";
	
	private static final String APP_DESCRIPTION = "Application created by the Code Center Importer version: undefined";
	private static final String ROLE = "Application Administrator";
	
	private static Logger log = LoggerFactory.getLogger(ImportIT.class
			.getName());

	private static CodeCenterConfigManager ccConfig = null;
	private static CodeCenterServerWrapper ccsw = null;
	private static ProtexServerWrapper<ProtexProjectPojo> psw = null;
	private static ProtexConfigManager pConfig = null;
	
	private static String projectId = null;

	private static CCISingleServerProcessor processor = null;

	@BeforeClass
	static public void setUpBeforeClass() throws Exception {

		Properties props = createPropertiesNumericPrefixAppAdjuster();
		
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);

		// Create cc wrapper so that we can perform cleanup tasks
		ccsw = new CodeCenterServerWrapper(ccConfig.getServerBean(),
				ccConfig);
		psw = new ProtexServerWrapper<ProtexProjectPojo>(pConfig.getServerBean(),
				pConfig, true);

		processor = new CCISingleServerProcessor(ccConfig, pConfig);
		
		projectId = ProtexTestUtils.createProject(psw, pConfig, APP_NAME, "src/test/resources/source");
	}
	
	@AfterClass
	static public void tearDownAfterClass() throws Exception {
		CcTestUtils.deleteAppByName(ccsw, APP_NAME, APP_VERSION);
		ProtexTestUtils.deleteProjectById(psw, projectId);
	}

	@Test
	public void test() throws Exception {

		// The project has just been created; the app does not exist yet
		
		List<CCIProject> projects = ccConfig.getProjectList();
		// Before running the import, make sure to clean up.
		cleanupProjectsBeforeImport(projects);

		// Run the sync to create the app
		processor.performSynchronize();

		// Check the app, including a custom attr that'll prove that the (real)
		// app adjuster ran and worked
		// Also verify that the validate status is PASSED
		Map<String, String> expectedAttrValues = new HashMap<String, String>();
		expectedAttrValues.put(CUSTOM_ATTR, "123456");
		CcTestUtils.checkApplication(ccsw, APP_NAME, APP_VERSION, APP_DESCRIPTION, false, expectedAttrValues, true);

		// Switch to the Mock app adjuster so we can easily tell if it
		// was called or not (that logic is a little tricky)
		Properties props = createPropertiesMockAppAdjuster();
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);
		processor = new CCISingleServerProcessor(ccConfig, pConfig);
				
		// Change the project BOM
		ProtexTestUtils.makeSomeMatches(pConfig, APP_NAME, true);

		// Run sync
		int lastAdjusterCount = MockAppAdjuster.getAdjustAppCalledCount();
		processor.performSynchronize();
		
		// Make sure validation status is Green (sync should have re-run validation to get it to green)
		CcTestUtils.checkApplicationValidationStatusOk(ccsw, APP_NAME, APP_VERSION);
		
		// Verify that app adjuster was called (since BOM changed)
		assertEquals(lastAdjusterCount+1, MockAppAdjuster.getAdjustAppCalledCount());
		
		// Sync again (there's no BOM change to make this time)
		processor.performSynchronize();
		
		// Verify that app adjuster was NOT called (since BOM did NOT change)
		assertEquals(lastAdjusterCount+1, MockAppAdjuster.getAdjustAppCalledCount());
		
		// Configure util to filter out (skip) this project/app
		props = createPropertiesWithFilter();
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);
		processor = new CCISingleServerProcessor(ccConfig, pConfig);
		
		// Delete the app
		CcTestUtils.deleteAppByName(ccsw, APP_NAME, APP_VERSION);
		
		// Sync again
		processor.performSynchronize();
		
		// Verify that it skipped the filtered-out app
		CcTestUtils.checkApplicationDoesNotExist(ccsw, APP_NAME, APP_VERSION);
	}
	
	private static Properties createPropertiesNumericPrefixAppAdjuster() {
		Properties props = createBasicProperties();
		props.setProperty("app.adjuster.classname", "com.blackducksoftware.soleng.ccimport.appadjuster.custom.NumericPrefixedAppAdjuster");
//		props.setProperty("app.adjuster.classname", "com.blackducksoftware.soleng.ccimport.appadjuster.custom.MockAppAdjuster");
		
		props.setProperty("numprefixed.app.attribute.numericprefix", CUSTOM_ATTR);
		props.setProperty("numprefixed.app.attribute.analyzeddate", "null");
		props.setProperty("numprefixed.app.attribute.workstream", "null");
		props.setProperty("numprefixed.app.attribute.projectstatus", "null");
		props.setProperty("numprefixed.analyzed.date.format", "MM-dd-yyyy");
		props.setProperty("numprefixed.appname.pattern.separator", "-");
		props.setProperty("numprefixed.appname.pattern.numericprefix", "[0-9][0-9][0-9]+");
		props.setProperty("numprefixed.appname.pattern.workstream", "(PROD|RC1|RC2|RC3|RC4|RC5)");
		
		return props;
	}
	
	private static Properties createPropertiesWithFilter() {
		Properties props = createBasicProperties();
		props.setProperty("protex.project.name.filter", ".*-NONEXISTENT");
		
		return props;
	}
	
	private static Properties createPropertiesMockAppAdjuster() {
		Properties props = createBasicProperties();
		props.setProperty("app.adjuster.classname", "com.blackducksoftware.soleng.ccimport.appadjuster.custom.MockAppAdjuster");
		props.setProperty("app.adjuster.only.if.bomedits", "true");
		props.setProperty("revalidate.after.changing.bom", "true");
		return props;
	}
	
	private static Properties createBasicProperties() {
		Properties props = new Properties();
		props.setProperty("protex.server.name", "http://se-menger.blackducksoftware.com");
		props.setProperty("protex.user.name", "ccImportUser@blackducksoftware.com");
		props.setProperty("protex.password", "blackduck");

		props.setProperty("cc.server.name", "http://cc-integration/");
//		props.setProperty("cc.server.name", "http://salescc/");
		
		props.setProperty("cc.user.name", "ccImportUser");
//		props.setProperty("cc.user.name", "sbillings@blackducksoftware.com");
		
		props.setProperty("cc.password", "blackduck");
		props.setProperty("protex.password.isplaintext", "true");
		props.setProperty("cc.password.isplaintext", "true");
		props.setProperty("cc.protex.name", "Menger");
		props.setProperty("cc.default.app.version", APP_VERSION);
		props.setProperty("cc.workflow", WORKFLOW);
		props.setProperty("cc.owner", APP_OWNER);
		props.setProperty("protex.project.list", APP_NAME);
		props.setProperty("validate.application", "true");
		props.setProperty("cc.submit.request", "true");
		props.setProperty("validate.requests.delete", "true");
		return props;
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
