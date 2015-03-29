package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

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
	
	// parameters for multi-threaded new app list file generation test
	private static final int NUM_THREADS = 20; // 20 is a pretty good test; 3/29/2015: successfully tested 100 
	private static final int APP_COUNT = NUM_THREADS*2;

	private static final String NEW_APPS_LIST_FILENAME = "unit_test_new_apps.txt";
	private static final String NEW_APPS_LIST_FILENAME_MANY_THREADS = "unit_test_new_apps_many_threads.txt";
	private static final String DATE_FORMAT = "MM-dd-yyyy";
	private static final String CUSTOM_ATTR_NAME = "Sample Textfield";
	private static final String CC_URL = "http://int-cc-dev.blackducksoftware.com/";
	public static final String SUPERUSER_USERNAME = "super";
	public static final String SUPERUSER_PASSWORD = "super";
	
	private static final String NUMPREFIX1_ATTR_VALUE = "123456";
	private static final String APP_NAME_STRING_BASE = "test application";
	private static final String APP_NAME_STRING1 = APP_NAME_STRING_BASE +"1";
	private static final String APP_NAME_STRING2 = APP_NAME_STRING_BASE +"2";
	private static final String WORK_STREAM = "RC3";
	private static String APPLICATION1_NAME = NUMPREFIX1_ATTR_VALUE + "-" + APP_NAME_STRING1 + "-" + WORK_STREAM + "-CURRENT";
	private static String APPLICATION2_NAME = NUMPREFIX1_ATTR_VALUE + "-" + APP_NAME_STRING2 + "-" + WORK_STREAM + "-CURRENT";
	
	private static final String CUSTOM_ATTR = "Sample Textfield";
	private static final String WORKFLOW = "Serial";
	private static final String APP_OWNER = "unitTester@blackducksoftware.com";
	private static String APPLICATION_VERSION = "TestVersion";
	private static Logger log = LoggerFactory.getLogger(AppAdjusterIT.class.getName());

	private static String CONFIG_FILE = "src/test/resources/adjuster_test.properties";

	
	
	private static List<String> protexProjectIds = new ArrayList<String>(APP_COUNT + 10);
	private static List<String> ccAppNames = new ArrayList<String>(APP_COUNT + 10);

	
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
		for (String protexProjectId : protexProjectIds) {
			ProtexTestUtils.deleteProjectById(protexServerWrapper, protexProjectId);
		}
		for (String ccAppName : ccAppNames) {
			TestUtils.removeApplication(cc, ccAppName, APPLICATION_VERSION);
		}
	}

	@Test
	public void testCustomAdjusterPluginMechanism() throws Exception {

		File outputFile = new File(NEW_APPS_LIST_FILENAME);
		outputFile.delete();
		
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
		String protexProjectId = ProtexTestUtils.createProject(protexServerWrapper, protexConfigManager, APPLICATION1_NAME,
				"src/test/resources/source");
		protexProjectIds.add(protexProjectId);
		ccAppNames.add(APPLICATION1_NAME);
		protexProjectId = ProtexTestUtils.createProject(protexServerWrapper, protexConfigManager, APPLICATION2_NAME,
				"src/test/resources/source");
		protexProjectIds.add(protexProjectId);
		ccAppNames.add(APPLICATION2_NAME);
    	
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
			
			// Run the sync
			processor.performSynchronize();

			// Now check to see if both applications actually exists.
			boolean exists = checkCcApp(ccWrapper, projects);
			Assert.assertEquals(true, exists);
			
			// Now check to see if both applications got written to the "new apps" output file
			assertTrue(outputFile.canRead());
			
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
	
	private static Properties createBasicProperties() {
		Properties props = new Properties();
		props.setProperty("protex.server.name", "http://se-menger.blackducksoftware.com");
		props.setProperty("protex.user.name", "ccImportUser@blackducksoftware.com");
		props.setProperty("protex.password", "blackduck");

		props.setProperty("cc.server.name", "http://int-cc-dev/");
		props.setProperty("cc.user.name", "ccImportUser");
		props.setProperty("cc.password", "blackduck");
		
		props.setProperty("protex.password.isplaintext", "true");
		props.setProperty("cc.password.isplaintext", "true");
		
		props.setProperty("cc.protex.name", "Menger2");
		props.setProperty("cc.default.app.version", APPLICATION_VERSION);
		props.setProperty("cc.workflow", WORKFLOW);
		props.setProperty("cc.owner", APP_OWNER);
		
		props.setProperty("validate.application", "false");
		props.setProperty("validate.application.smart", "false");
		props.setProperty("cc.submit.request", "false");
		props.setProperty("validate.requests.delete", "false");
		props.setProperty("validate.requests.add", "false");
		return props;
	}
	
	private Properties createPropertiesManyThreads(int numThreads) {
		Properties props = createBasicProperties();
		props.setProperty("app.adjuster.classname", "com.blackducksoftware.soleng.ccimport.appadjuster.custom.NumericPrefixedAppAdjuster");
		props.setProperty("num.threads", Integer.toString(numThreads));
		props.setProperty("numprefixed.app.attribute.numericprefix", CUSTOM_ATTR);
		props.setProperty("numprefixed.app.attribute.analyzeddate", "null");
		props.setProperty("numprefixed.app.attribute.workstream", "null");
		props.setProperty("numprefixed.app.attribute.projectstatus", "null");
		props.setProperty("numprefixed.analyzed.date.format", "MM-dd-yyyy");
		props.setProperty("numprefixed.appname.pattern.separator", "-");
		props.setProperty("numprefixed.appname.pattern.numericprefix", "[0-9][0-9][0-9]+");
		props.setProperty("numprefixed.appname.pattern.workstream", "(PROD|RC1|RC2|RC3|RC4|RC5)");
		
		props.setProperty("numprefixed.new.app.list.filename", NEW_APPS_LIST_FILENAME_MANY_THREADS);
		
		return props;
	}
	

	@Test
	public void testNewAppsFileManyThreads() throws Exception {
		List<String> testAppNames = new ArrayList<String>();
		Properties props = createPropertiesManyThreads(NUM_THREADS);
		
		StringBuilder sb = new StringBuilder();
		for (int i=0; i < APP_COUNT; i++) {
			String appName = NUMPREFIX1_ATTR_VALUE + "-" + APP_NAME_STRING_BASE + "10" + i + "-" + WORK_STREAM + "-CURRENT";
			testAppNames.add(appName);
			sb.append(appName);
			sb.append(',');
		}
		props.setProperty("protex.project.list", sb.toString()); // add project list to properties
		
        CodeCenterConfigManager ccConfigManager = new CodeCenterConfigManager(props);
        ProtexConfigManager protexConfigManager = new ProtexConfigManager(props);
		
    	ServerBean bean = new ServerBean();
		bean.setServerName(CC_URL);
		bean.setUserName(SUPERUSER_USERNAME);
		bean.setPassword(SUPERUSER_PASSWORD);
		
		CodeCenterServerWrapper ccWrapper = new CodeCenterServerWrapper(bean, ccConfigManager);
		CodeCenterServerProxyV6_6_0 cc = ccWrapper.getInternalApiWrapper().getProxy();
    	
    	// Create protex projects to import into CC
    	ProtexServerWrapper protexServerWrapper = ProtexTestUtils.initProtexServerWrapper(protexConfigManager);
    	
    	// Create a bunch of projects: 2x #threads
    	int i=0;
    	String cloneFromProjectId=null;
    	for (String appName : testAppNames) {
    		String protexProjectId;
    		if (i == 0) {
	    		System.out.println("Creating project: " + appName);
	    		protexProjectId = ProtexTestUtils.createProject(protexServerWrapper, protexConfigManager, appName,
					"src/test/resources/source");
	    		cloneFromProjectId = protexProjectId;
    		} else {
    			System.out.println("Cloning to project: " + appName);
	    		protexProjectId = ProtexTestUtils.cloneProject(protexServerWrapper, protexConfigManager, cloneFromProjectId, appName);
    		}
    		protexProjectIds.add(protexProjectId);
    		ccAppNames.add(appName); // Class-level list of apps to delete
    		i++;
    	}
    	
		String[] args = {"-config", "config.properties", "-new-app-list-filename", NEW_APPS_LIST_FILENAME_MANY_THREADS};
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
			File outputFile = new File(NEW_APPS_LIST_FILENAME_MANY_THREADS);
			outputFile.delete();

			// Run the sync
			System.out.println("Performing sync");
			processor.performSynchronize();

			// Now check to see if both applications actually exists.
//			boolean exists = checkCcApp(ccWrapper, projects);
//			Assert.assertEquals(true, exists);
			
			// Now check to see if both applications got written to the "new apps" output file
			BufferedReader br = new BufferedReader(new FileReader(NEW_APPS_LIST_FILENAME_MANY_THREADS));
//			boolean app1Listed=false;
//			boolean app2Listed=false;
//			for (int i=0; i < 2; i++) {
//				String line = br.readLine();
//				if (APPLICATION1_NAME.equals(line)) {
//					app1Listed = true;
//				} else if (APPLICATION2_NAME.equals(line)) {
//					app2Listed = true;
//				}
//			}
//			assertTrue(app1Listed);
//			assertTrue(app2Listed);
			
			String line;
			int lineCount=0;
			while ((line = br.readLine()) != null) {
				System.out.println("Read from new app list file: " + line);
				lineCount++;
			}
			assertEquals(APP_COUNT, lineCount);

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
