package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import junit.framework.Assert;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.connector.protex.ProtexServerWrapper;
import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.sdk.codecenter.application.data.Project;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeIdToken;
import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV6_6_0;
import com.blackducksoftware.sdk.codecenter.common.data.AttributeValue;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

/**
 * Test the mechanism that allows custom app adjusters to be plugged in via configuration
 * @author Steve Billings
 * @date Nov 6, 2014
 *
 */
public class AppAdjusterIT {

	private static final String NEW_APPS_LIST_FILENAME = "unit_test_new_apps.txt";
	private static final String DATE_FORMAT = "MM-dd-yyyy";
	private static final String CUSTOM_ATTR_NAME = "Sample Textfield";
	private static final String CC_URL = "http://int-cc-dev.blackducksoftware.com/";
	public static final String SUPERUSER_USERNAME = "super";
	public static final String SUPERUSER_PASSWORD = "super";
	
	private static final String NUMPREFIX1_ATTR_VALUE = "123456";
	private static final String APP_NAME_STRING1 = "my application1";
	private static final String APP_NAME_STRING2 = "my application2";
	private static final String WORK_STREAM = "RC3";
	private static String APPLICATION1_NAME = NUMPREFIX1_ATTR_VALUE + "-" + APP_NAME_STRING1 + "-" + WORK_STREAM + "-CURRENT";
	private static String APPLICATION2_NAME = NUMPREFIX1_ATTR_VALUE + "-" + APP_NAME_STRING2 + "-" + WORK_STREAM + "-CURRENT";
	
	private static String APPLICATION_VERSION = "TestVersion";
	private static Logger log = LoggerFactory.getLogger(AppAdjusterIT.class.getName());

	public static String CONFIG_FILE = "src/test/resources/adjuster_test.properties";

	private static String protexProjectIdOrig1;
	private static String protexProjectIdOrig2;
	
	@Rule
	public ExpectedException exception = ExpectedException.none();

	@BeforeClass
	static public void setUpBeforeClass() throws Exception {
	}
	
	@AfterClass
	static public void tearDownAfterClass() throws Exception {

        CodeCenterConfigManager ccConfigManager = new CodeCenterConfigManager(CONFIG_FILE);
        ProtexConfigManager protexConfigManager = new ProtexConfigManager(CONFIG_FILE);
		
    	ServerBean bean = new ServerBean();
		bean.setServerName(CC_URL);
		bean.setUserName(SUPERUSER_USERNAME);
		bean.setPassword(SUPERUSER_PASSWORD);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(bean, ccConfigManager);
		CodeCenterServerProxyV6_6_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
    	
		ProtexServerWrapper protexServerWrapper = ProtexTestUtils.initProtexServerWrapper(protexConfigManager);
		ProtexTestUtils.deleteProjectById(protexServerWrapper, protexProjectIdOrig1);
		ProtexTestUtils.deleteProjectById(protexServerWrapper, protexProjectIdOrig2);
		TestUtils.removeApplication(cc, APPLICATION1_NAME, APPLICATION_VERSION);
		TestUtils.removeApplication(cc, APPLICATION2_NAME, APPLICATION_VERSION);
	}

	@Test
	public void testCustomAdjusterPluginMechanism() throws Exception {

        CodeCenterConfigManager ccConfigManager = new CodeCenterConfigManager(CONFIG_FILE);
        ProtexConfigManager protexConfigManager = new ProtexConfigManager(CONFIG_FILE);
		
    	ServerBean bean = new ServerBean();
		bean.setServerName(CC_URL);
		bean.setUserName(SUPERUSER_USERNAME);
		bean.setPassword(SUPERUSER_PASSWORD);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(bean, ccConfigManager);
		CodeCenterServerProxyV6_6_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
    	
    	// Create protex project to import into CC
    	ProtexServerWrapper protexServerWrapper = ProtexTestUtils.initProtexServerWrapper(protexConfigManager);
		protexProjectIdOrig1 = ProtexTestUtils.createProject(protexServerWrapper, protexConfigManager, APPLICATION1_NAME,
				"src/test/resources/source");
		protexProjectIdOrig2 = ProtexTestUtils.createProject(protexServerWrapper, protexConfigManager, APPLICATION2_NAME,
				"src/test/resources/source");
    	
		String[] args = {"-config", "config.properties", "-new-app-list-filename", NEW_APPS_LIST_FILENAME};
		ccConfigManager.setCmdLineArgs(args);
		
		Object appAdjusterObject = CCIProjectImporterHarness.getAppAdjusterObject(ccWrapper, ccConfigManager);
		Method appAdjusterMethod = CCIProjectImporterHarness.getAppAdjusterMethod(ccWrapper, ccConfigManager, appAdjusterObject);
		
		// Construct the factory that the processor will use to create
		// the objects (run multi-threaded) to handle each subset of the project list
		ProjectProcessorThreadWorkerFactory threadWorkerFactory = 
				new ProjectProcessorThreadWorkerFactoryImpl(ccWrapper, ccConfigManager, appAdjusterObject, appAdjusterMethod);
    	CCISingleServerProcessor processor = new CCISingleServerProcessor(ccConfigManager, protexConfigManager, ccWrapper,
    			threadWorkerFactory);

		try {
			List<CCIProject> projects = ccConfigManager.getProjectList();
			// Before running the import, make sure to clean up.
			cleanupProjectsBeforeImport(ccWrapper, projects);
			File outputFile = new File(NEW_APPS_LIST_FILENAME);
			outputFile.delete();

			// Run the sync
			processor.performSynchronize();

			// Now check to see if both applications actually exists.
			boolean exists = checkCcApp(ccWrapper, projects);
			Assert.assertEquals(true, exists);
			
			// Now check to see if both applications got written to the "new apps" output file
			BufferedReader br = new BufferedReader(new FileReader(NEW_APPS_LIST_FILENAME));
			boolean app1Listed=false;
			boolean app2Listed=false;
			for (int i=0; i < 2; i++) {
				String line = br.readLine();
				if (APPLICATION1_NAME.equals(line)) {
					app1Listed = true;
				} else if (APPLICATION2_NAME.equals(line)) {
					app2Listed = true;
				}
			}
			assertTrue(app1Listed);
			assertTrue(app2Listed);

		} catch (CodeCenterImportException e) {
			Assert.fail(e.getMessage());
		}
	}

	
	/**
	 * CHecks to see if that list of applications exist and have associations.
	 * 
	 * @param projects
	 */
	private boolean checkCcApp(CodeCenterServerWrapper ccsw, List<CCIProject> projects) {
		SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT);
		String expectedAnalyzedDate = formatter.format(new Date());

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

					checkAnalyzedDate(ccsw, app, expectedAnalyzedDate);
				}
			}

			exists = true;
		} catch (Exception e) {
			log.error("Error during application verification: "
					+ e.getMessage());
		}

		return exists;
	}
	
	private void checkAnalyzedDate(CodeCenterServerWrapper ccsw, Application app, String expectedAnalyzedDate) throws Exception {
		boolean foundCustomAttr = false;
		List<AttributeValue> attrValues = app.getAttributeValues();
		for (AttributeValue attrValue : attrValues) {
			AttributeIdToken a = (AttributeIdToken) attrValue.getAttributeId();
			String curAttrName = ccsw.getInternalApiWrapper().getProxy().getAttributeApi().getAttribute(a).getName();
			String curAttrValue = attrValue.getValues().get(0);
			System.out.println("attr name: " + curAttrName + 
				"; value: " + curAttrValue);
			
			if (CUSTOM_ATTR_NAME.equals(curAttrName)) {
				foundCustomAttr = true;
				assertEquals(expectedAnalyzedDate, curAttrValue);
			}
		}
		assertTrue(foundCustomAttr);
	}

	/**
	 * Deletes the specific applications that we are testing with.
	 * 
	 * @param projects
	 */
	private void cleanupProjectsBeforeImport(CodeCenterServerWrapper ccsw, List<CCIProject> projects) {
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
