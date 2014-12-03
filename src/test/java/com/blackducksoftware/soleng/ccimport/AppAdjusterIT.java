package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

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

import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.protex.ProtexServerWrapper;

/**
 * Test the mechanism that allows custom app adjusters to be plugged in via configuration
 * @author Steve Billings
 * @date Nov 6, 2014
 *
 */
public class AppAdjusterIT {

	private static final String DATE_FORMAT = "MM-dd-yyyy";
	private static final String CUSTOM_ATTR_NAME = "Sample Textfield";
	private static final String CC_URL = "http://cc-integration.blackducksoftware.com/";
	public static final String SUPERUSER_USERNAME = "super";
	public static final String SUPERUSER_PASSWORD = "super";
	
	private static final String NUMPREFIX1_ATTR_VALUE = "123456";
	private static final String APP_NAME_STRING = "my application";
	private static final String WORK_STREAM = "RC3";
	private static String APPLICATION1_NAME = NUMPREFIX1_ATTR_VALUE + "-" + APP_NAME_STRING + "-" + WORK_STREAM + "-CURRENT";
	
	private static String APPLICATION_VERSION = "TestVersion";
	private static Logger log = LoggerFactory.getLogger(AppAdjusterIT.class.getName());

	public static String CONFIG_FILE = "src/test/resources/adjuster_test.properties";

	private static String protexProjectIdOrig;
	
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
		ProtexTestUtils.deleteProjectById(protexServerWrapper, protexProjectIdOrig);
		TestUtils.removeApplication(cc, APPLICATION1_NAME, APPLICATION_VERSION);
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
		protexProjectIdOrig = ProtexTestUtils.createProject(protexServerWrapper, protexConfigManager, APPLICATION1_NAME,
				"src/test/resources/source");
    	
    	CCISingleServerProcessor processor = new CCISingleServerProcessor(ccConfigManager, protexConfigManager);

		try {
			List<CCIProject> projects = ccConfigManager.getProjectList();
			// Before running the import, make sure to clean up.
			cleanupProjectsBeforeImport(ccWrapper, projects);

			// Run the sync
			processor.performSynchronize();

			// Now check to see if the application(s) actually exists.
			boolean exists = checkCcApp(ccWrapper, projects);
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