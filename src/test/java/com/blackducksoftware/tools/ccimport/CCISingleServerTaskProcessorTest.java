package com.blackducksoftware.tools.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

public class CCISingleServerTaskProcessorTest {

    private static final int NUM_APPS = 3;
    private static final String APP_NAME_FILTER = "testApp.*";
    private static final String PROJECT_ID1 = "testProjectId1";
    private static final String APP_NAME1 = "testAppName1";
    private static final String PROJECT_ID2 = "testProjectId2";
    private static final String APP_NAME2 = "testAppName2";
    private static final String PROJECT_ID3 = "testProjectId3";
    private static final String APP_NAME3 = "testAppName3";
    private static final String APP_VERSION = "Unspecified";

    private static final String PROJECT_ID_OUT_OF_SCOPE = "testProjectIdOutOfScope";
    private static final String APP_NAME_OUT_OF_SCOPE = "outOfScopeApp";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testSuccessfulTasks() throws Exception {
	for (int numThreads = 1; numThreads <= 4; numThreads++) {
	    System.out
		    .println("======================================== Running with "
			    + numThreads + " threads");
	    SyncProjectTaskFactory taskFactory = new MockSuccessfulSyncProjectTaskFactory();
	    CCIReportSummary results = testGivenWithNumberOfThreads(numThreads,
		    taskFactory);

	    assertEquals(Integer.valueOf(NUM_APPS),
		    results.getTotalCCApplications());
	    assertEquals(Integer.valueOf(NUM_APPS),
		    results.getTotalProtexProjects());

	    assertEquals(Integer.valueOf(NUM_APPS * 2),
		    results.getTotalPotentialAdds());
	    assertEquals(Integer.valueOf(NUM_APPS * 3),
		    results.getTotalPotentialDeletes());

	    assertEquals(Integer.valueOf(NUM_APPS * 2),
		    results.getTotalRequestsAdded());
	    assertEquals(Integer.valueOf(NUM_APPS * 3),
		    results.getTotalRequestsDeleted());

	    assertEquals(Integer.valueOf(1), results.getTotalProjectsSkipped());
	}
    }

    @Test
    public void testFailingTasks() throws Exception {
	for (int numThreads = 1; numThreads <= 4; numThreads++) {
	    System.out
		    .println("======================================== Running with "
			    + numThreads + " threads");
	    SyncProjectTaskFactory taskFactory = new MockFailingSyncProjectTaskFactory();
	    CCIReportSummary results = testGivenWithNumberOfThreads(numThreads,
		    taskFactory);

	    System.out.println("==================\nAggregate Results:\n"
		    + results + "\n================================");

	    assertEquals(Integer.valueOf(NUM_APPS),
		    results.getTotalImportsFailed());
	    assertEquals(Integer.valueOf(NUM_APPS),
		    results.getTotalCCApplications());
	    assertEquals(Integer.valueOf(NUM_APPS),
		    results.getTotalProtexProjects());
	    assertEquals(Integer.valueOf(1), results.getTotalProjectsSkipped());
	    assertEquals(Integer.valueOf(0),
		    results.getTotalValidatesPerfomed());

	    List<String> failedImportList = results.getFailedImportList();
	    assertEquals(NUM_APPS, failedImportList.size());
	    assertTrue(failedImportList.contains(APP_NAME1));
	    assertTrue(failedImportList.contains(APP_NAME2));
	    assertTrue(failedImportList.contains(APP_NAME3));
	}
    }

    private CCIReportSummary testGivenWithNumberOfThreads(int numThreads,
	    SyncProjectTaskFactory taskFactory) throws Exception,
	    CommonFrameworkException {

	Properties props = createBasicProperties(APP_NAME1, APP_NAME2,
		APP_NAME3, numThreads);

	CodeCenterConfigManager ccConfigManager = new CodeCenterConfigManager(
		props);

	ProtexConfigManager protexConfigManager = new ProtexConfigManager(props);
	CodeCenterServerWrapper codeCenterServerWrapper = null;
	ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper = createMockProtexApis();

	CCISingleServerTaskProcessor processor = new CCISingleServerTaskProcessor(
		ccConfigManager, protexConfigManager, codeCenterServerWrapper,
		protexServerWrapper, taskFactory);

	processor.performSynchronize();

	CCIReportSummary results = processor.getAggregatedResults();
	return results;
    }

    private static ProtexServerWrapper<ProtexProjectPojo> createMockProtexApis()
	    throws com.blackducksoftware.sdk.fault.SdkFault,
	    CommonFrameworkException {

	// Mock wrappers and APIs
	ProtexServerWrapper<ProtexProjectPojo> psw = mock(ProtexServerWrapper.class);
	ProjectPojo projectPojo1 = new ProtexProjectPojo(PROJECT_ID1, APP_NAME1);
	when(psw.getProjectByName(APP_NAME1)).thenReturn(projectPojo1);

	ProjectPojo projectPojo2 = new ProtexProjectPojo(PROJECT_ID2, APP_NAME2);
	when(psw.getProjectByName(APP_NAME2)).thenReturn(projectPojo2);

	ProjectPojo projectPojo3 = new ProtexProjectPojo(PROJECT_ID3, APP_NAME3);
	when(psw.getProjectByName(APP_NAME3)).thenReturn(projectPojo3);

	ProjectPojo projectPojoOutOfScope = new ProtexProjectPojo(
		PROJECT_ID_OUT_OF_SCOPE, APP_NAME_OUT_OF_SCOPE);
	when(psw.getProjectByName(APP_NAME_OUT_OF_SCOPE)).thenReturn(
		projectPojoOutOfScope);

	return psw;
    }

    private static Properties createBasicProperties(String appName1,
	    String appName2, String appName3, int numThreads) {
	Properties props = new Properties();
	props.setProperty("protex.server.name", "http://protex.test.com");
	props.setProperty("protex.user.name", "testUser");
	props.setProperty("protex.password", "testPassword");
	props.setProperty("cc.server.name", "http://cc.test.com");
	props.setProperty("cc.user.name", "testUser");
	props.setProperty("cc.password", "testPassword");
	props.setProperty("protex.password.isencrypted", "false");
	props.setProperty("cc.password.isencrypted", "false");
	props.setProperty("cc.protex.name", "protexServer");
	props.setProperty("cc.default.app.version", APP_VERSION);
	props.setProperty("cc.workflow", "testWorkflow");
	props.setProperty("cc.owner", "testOwner");
	props.setProperty("protex.project.list", appName1 + "," + appName2
		+ "," + appName3 + "," + APP_NAME_OUT_OF_SCOPE);
	props.setProperty("validate.application", "true");
	props.setProperty("validate.application.smart", "true");
	props.setProperty("cc.submit.request", "true");
	props.setProperty("validate.requests.delete", "true");
	props.setProperty("num.threads", String.valueOf(numThreads));

	props.setProperty("protex.project.name.filter", APP_NAME_FILTER);
	return props;
    }
}
