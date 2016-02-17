package com.blackducksoftware.tools.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.ProtexServerWrapper;

public class CCISingleServerTaskProcessorTest {

    private static final String TEST_APP_NAME_BASE = "testAppName";

    private static final String TEST_PROJECT_ID_BASE = "testProjectId";

    private static final int MAX_THREADS = 7;

    private static final int NUM_APPS = 71;

    private static final String APP_NAME_FILTER = "testApp.*";

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
        for (int numThreads = 1; numThreads <= MAX_THREADS; numThreads++) {
            System.out
                    .println("======================================== Running with "
                            + numThreads + " threads");
            SyncProjectTaskFactory taskFactory = new MockSuccessfulSyncProjectTaskFactory();
            CCIReportSummary results = testGivenWithNumberOfThreads(numThreads,
                    taskFactory);

            assertEquals(Integer.valueOf(NUM_APPS),
                    results.getTotalCCApplications());
            assertEquals(Integer.valueOf(NUM_APPS + 1),
                    results.getTotalProtexProjects()); // one protex project was ignored

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
        for (int numThreads = 1; numThreads <= MAX_THREADS; numThreads++) {
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
            assertEquals(Integer.valueOf(NUM_APPS + 1),
                    results.getTotalProtexProjects()); // we added an ignored project
            assertEquals(Integer.valueOf(1), results.getTotalProjectsSkipped());
            assertEquals(Integer.valueOf(0),
                    results.getTotalValidatesPerfomed());

            List<String> failedImportList = results.getFailedImportList();
            assertEquals(NUM_APPS, failedImportList.size());
            for (int i = 0; i < NUM_APPS; i++) {
                assertTrue(failedImportList.contains(TEST_APP_NAME_BASE + (i + 1)));
            }
        }

    }

    private CCIReportSummary testGivenWithNumberOfThreads(int numThreads,
            SyncProjectTaskFactory taskFactory) throws Exception,
            CommonFrameworkException {

        List<String> appNames = new ArrayList<>(NUM_APPS);
        for (int i = 0; i < NUM_APPS; i++) {
            appNames.add(TEST_APP_NAME_BASE + (i + 1));
        }
        Properties props = createBasicProperties(appNames, numThreads);

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

        for (int i = 0; i < NUM_APPS; i++) {
            ProjectPojo projectPojo = new ProtexProjectPojo(TEST_PROJECT_ID_BASE + (i + 1), TEST_APP_NAME_BASE + (i + 1));
            when(psw.getProjectByName(TEST_APP_NAME_BASE + (i + 1))).thenReturn(projectPojo);
        }

        ProjectPojo projectPojoOutOfScope = new ProtexProjectPojo(
                PROJECT_ID_OUT_OF_SCOPE, APP_NAME_OUT_OF_SCOPE);
        when(psw.getProjectByName(APP_NAME_OUT_OF_SCOPE)).thenReturn(
                projectPojoOutOfScope);

        return psw;
    }

    private static Properties createBasicProperties(List<String> appNames, int numThreads) {
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

        props.setProperty("validate.application", "true");
        props.setProperty("validate.application.smart", "true");
        props.setProperty("cc.submit.request", "true");
        props.setProperty("validate.requests.delete", "true");
        props.setProperty("num.threads", String.valueOf(numThreads));

        props.setProperty("protex.project.name.filter", APP_NAME_FILTER);

        StringBuilder appNameListString = new StringBuilder();
        for (String appName : appNames) {
            appNameListString.append(appName);
            appNameListString.append(',');
        }
        props.setProperty("protex.project.list", appNameListString.toString() + APP_NAME_OUT_OF_SCOPE);
        return props;
    }
}
