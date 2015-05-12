package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.connector.protex.ProtexServerWrapper;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.protex.ProtexProjectPojo;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.sdk.codecenter.application.data.Project;
import com.blackducksoftware.soleng.ccimport.appadjuster.custom.MockAppAdjuster;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

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
	private static final String APP_SEALID1 = "123456";
	private static final String APP_NAME1 = APP_SEALID1 + "-test1-PROD-CURRENT";
	
	private static final String APP_SEALID2 = "123457";
	private static final String APP_NAME2 = APP_SEALID2 + "-test2-PROD-CURRENT";
	
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
	
	private static String projectId1 = null;
	private static String projectId2 = null;

	private static CCISingleServerProcessor processor = null;

	@BeforeClass
	static public void setUpBeforeClass() throws Exception {

		Properties props = createPropertiesNumericPrefixAppAdjuster(APP_NAME1);
		
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);

		// Create cc wrapper so that we can perform cleanup tasks
		ccsw = new CodeCenterServerWrapper(ccConfig.getServerBean(),
				ccConfig);
		psw = new ProtexServerWrapper<ProtexProjectPojo>(pConfig.getServerBean(),
				pConfig, true);

		// Construct the factory that the processor will use to create
	    // the objects (run multi-threaded) to handle each subset of the project list
	 	ProjectProcessorThreadWorkerFactory threadWorkerFactory = 
	 				new ProjectProcessorThreadWorkerFactoryImpl(ccsw, ccConfig, null, null);
		processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw, threadWorkerFactory);
		
		
		
	}
	
	@AfterClass
	static public void tearDownAfterClass() throws Exception {
		CcTestUtils.deleteAppByName(ccsw, APP_NAME1, APP_VERSION);
		ProtexTestUtils.deleteProjectById(psw, projectId1);
		
		CcTestUtils.deleteAppByName(ccsw, APP_NAME2, APP_VERSION);
		ProtexTestUtils.deleteProjectById(psw, projectId2);
	}

	@Test
	public void testBasic() throws Exception {
		
		Properties props = createPropertiesNumericPrefixAppAdjuster(APP_NAME1);
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);
		
		Object appAdjusterObject = CCIProjectImporterHarness.getAppAdjusterObject(ccsw, ccConfig);
		Method appAdjusterMethod = CCIProjectImporterHarness.getAppAdjusterMethod(ccsw, ccConfig, appAdjusterObject);
		
		// Construct the factory that the processor will use to create
	    // the objects (run multi-threaded) to handle each subset of the project list
	 	ProjectProcessorThreadWorkerFactory threadWorkerFactory = 
	 				new ProjectProcessorThreadWorkerFactoryImpl(ccsw, ccConfig, appAdjusterObject, appAdjusterMethod);
		processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw, threadWorkerFactory);
		projectId1 = ProtexTestUtils.createProject(psw, pConfig, APP_NAME1, "src/test/resources/source");

		// The project has just been created; the app does not exist yet
		
		List<CCIProject> projects = ccConfig.getProjectList();
		// Before running the import, make sure to clean up.
		removeAppsBeforeImport(projects);

		// Run the sync to create the app
		processor.performSynchronize();

		// Check the app, including a custom attr that'll prove that the (real)
		// app adjuster ran and worked
		// Also verify that the validate status is PASSED
		Map<String, String> expectedAttrValues = new HashMap<String, String>();
		expectedAttrValues.put(CUSTOM_ATTR, APP_SEALID1);
		CcTestUtils.checkApplication(ccsw, APP_NAME1, APP_VERSION, APP_DESCRIPTION, false, expectedAttrValues, true);

		// Switch to the Mock app adjuster so we can easily tell if it
		// was called or not (that logic is a little tricky)
		props = createPropertiesMockAppAdjuster(APP_NAME1);
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);
		
		// Switch to Mock app adjuster
		appAdjusterObject = CCIProjectImporterHarness.getAppAdjusterObject(ccsw, ccConfig);
		appAdjusterMethod = CCIProjectImporterHarness.getAppAdjusterMethod(ccsw, ccConfig, appAdjusterObject);
		
		// Construct the factory that the processor will use to create
	    // the objects (run multi-threaded) to handle each subset of the project list
	 	threadWorkerFactory = 
	 				new ProjectProcessorThreadWorkerFactoryImpl(ccsw, ccConfig, appAdjusterObject, appAdjusterMethod);
		processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw, threadWorkerFactory);
				
		// Change the project BOM
		ProtexTestUtils.makeSomeIds(pConfig, APP_NAME1, true);

		// Run sync
		int lastAdjusterCount = MockAppAdjuster.getAdjustAppCalledCount();
		processor.performSynchronize();
		
		// Make sure validation status is Green (sync should have re-run validation to get it to green)
		CcTestUtils.checkApplicationValidationStatusOk(ccsw, APP_NAME1, APP_VERSION);
		
		// Verify that app adjuster was called (since BOM changed)
		assertEquals(lastAdjusterCount+1, MockAppAdjuster.getAdjustAppCalledCount());
		
		// Sync again (there's no BOM change to make this time)
		processor.performSynchronize();
		
		// Verify that app adjuster was NOT called (since BOM did NOT change)
		assertEquals(lastAdjusterCount+1, MockAppAdjuster.getAdjustAppCalledCount());
		
		// Configure util to filter out (skip) this project/app
		props = createPropertiesWithFilter(APP_NAME1);
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);
		
		// Construct the factory that the processor will use to create
	    // the objects (run multi-threaded) to handle each subset of the project list
	 	threadWorkerFactory = 
	 				new ProjectProcessorThreadWorkerFactoryImpl(ccsw, ccConfig, null, null);
		processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw, threadWorkerFactory);
		
		// Delete the app
		CcTestUtils.deleteAppByName(ccsw, APP_NAME1, APP_VERSION);
		
		// Sync again
		processor.performSynchronize();
		
		// Verify that it skipped the filtered-out app
		CcTestUtils.checkApplicationDoesNotExist(ccsw, APP_NAME1, APP_VERSION);
	}
	
	@Test
	public void testCleanUpOldValidationErrors() throws Exception {

		// The project has just been created; the app does not exist yet
		
		Properties props = createPropertiesLeaveOldValidationErrors(APP_NAME2);
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);
		
		// Construct the factory that the processor will use to create
	    // the objects (run multi-threaded) to handle each subset of the project list
	 	ProjectProcessorThreadWorkerFactory threadWorkerFactory = 
	 				new ProjectProcessorThreadWorkerFactoryImpl(ccsw, ccConfig, null, null);
		processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw, threadWorkerFactory);
		
		projectId2 = ProtexTestUtils.createProject(psw, pConfig, APP_NAME2, "src/test/resources/source");
		
		List<CCIProject> projects = ccConfig.getProjectList();
		// Before running the import, make sure to clean up.
		removeAppsBeforeImport(projects);
		
		// Run the sync to create the app
		processor.performSynchronize();

		// Check the app, and confirm that validation status is OK
		CcTestUtils.checkApplication(ccsw, APP_NAME2, APP_VERSION, APP_DESCRIPTION, false, null, true);
		
		// Change the project BOM
		ProtexTestUtils.makeSomeIds(pConfig, APP_NAME2, true);
		
		// Run the sync to create the app; this should leave validation status = ERROR
		processor.performSynchronize();

		// Switch to "clear old validation errors" mode
		// 
		props = createPropertiesClearOldValidationErrors(APP_NAME2);
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);
		
		// Construct the factory that the processor will use to create
	    // the objects (run multi-threaded) to handle each subset of the project list
	 	threadWorkerFactory = 
	 				new ProjectProcessorThreadWorkerFactoryImpl(ccsw, ccConfig, null, null);
		processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw, threadWorkerFactory);
		
		// Run the sync. This should clear the validation error
		processor.performSynchronize();
		
		// Check the app, and confirm that validation status is OK
		CcTestUtils.checkApplication(ccsw, APP_NAME2, APP_VERSION, APP_DESCRIPTION, false, null, true);
	}
	
	@Test
	public void testExceptionHandling() throws Exception {
		
		Properties props = createPropertiesNumericPrefixAppAdjuster(APP_NAME1);
		ccConfig = new CodeCenterConfigManager(props);
		pConfig = new ProtexConfigManager(props);
		
		// Construct the factory that the processor will use to create
	    // the objects (run multi-threaded) to handle each subset of the project list
	 	ProjectProcessorThreadWorkerFactory threadWorkerFactory = 
	 				new SuicidalProjectProcessorThreadWorkerFactory();
		processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw, threadWorkerFactory);
		projectId1 = ProtexTestUtils.createProject(psw, pConfig, APP_NAME1, "src/test/resources/source");
		
		// Run the sync; should throw mock exception
		processor.performSynchronize();
		assertTrue(processor.getThreadExceptionMessages().contains("Mock exception"));
		
	}
	
	private static Properties createPropertiesNumericPrefixAppAdjuster(String appName) {
		Properties props = createBasicProperties(appName);
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
	
	private static Properties createPropertiesWithFilter(String appName) {
		Properties props = createBasicProperties(appName);
		props.setProperty("protex.project.name.filter", ".*-NONEXISTENT");
		
		return props;
	}
	
	private static Properties createPropertiesMockAppAdjuster(String appName) {
		Properties props = createBasicProperties(appName);
		props.setProperty("app.adjuster.classname", "com.blackducksoftware.soleng.ccimport.appadjuster.custom.MockAppAdjuster");
		props.setProperty("app.adjuster.only.if.bomedits", "true");
		props.setProperty("revalidate.after.changing.bom", "true");
		return props;
	}
	
	private static Properties createPropertiesLeaveOldValidationErrors(String appName) {
		Properties props = createBasicProperties(appName);
		props.setProperty("revalidate.after.changing.bom", "false");
		return props;
	}
	
	private static Properties createPropertiesClearOldValidationErrors(String appName) {
		Properties props = createBasicProperties(appName);
		props.setProperty("revalidate.after.changing.bom", "true");
		return props;
	}
	
	private static Properties createBasicProperties(String appName) {
		Properties props = new Properties();
		props.setProperty("protex.server.name", "http://se-menger.blackducksoftware.com");
		props.setProperty("protex.user.name", "ccImportUser@blackducksoftware.com");
		props.setProperty("protex.password", "blackduck");

		props.setProperty("cc.server.name", "http://int-cc-dev/");
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
		props.setProperty("protex.project.list", appName);
		props.setProperty("validate.application", "true");
		props.setProperty("validate.application.smart", "true");
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

				List<Application> applications = ccsw.getInternalApiWrapper().getApplicationApi()
						.searchApplications(project.getProjectName(), apf);

				for (Application app : applications) {
					// No errors here guarantees existence
					ApplicationIdToken token = app.getId();
					Project associatedProject = ccsw.getInternalApiWrapper().getApplicationApi()
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
	private void removeAppsBeforeImport(List<CCIProject> projects) {
		log.info("Cleaning up projects before testing import");

		try {

			for (CCIProject project : projects) {
				ApplicationNameVersionToken token = new ApplicationNameVersionToken();
				token.setName(project.getProjectName());
				token.setVersion(project.getProjectVersion());
				Application appToDelete = ccsw.getInternalApiWrapper().getApplicationApi()
						.getApplication(token);

				if (appToDelete == null) {
					log.info("Nothing to cleanup!");
					return;
				} else {
					// Delete it
					ccsw.getInternalApiWrapper().getApplicationApi()
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
