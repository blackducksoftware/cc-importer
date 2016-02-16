package com.blackducksoftware.tools.ccimport;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.sdk.codecenter.application.ApplicationApi;
import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationCreate;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.Project;
import com.blackducksoftware.sdk.codecenter.application.data.ProjectIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ProtexRequest;
import com.blackducksoftware.sdk.codecenter.cola.ColaApi;
import com.blackducksoftware.sdk.codecenter.cola.data.ComponentIdToken;
import com.blackducksoftware.sdk.codecenter.cola.data.LicenseIdToken;
import com.blackducksoftware.sdk.codecenter.cola.data.LicenseSummary;
import com.blackducksoftware.sdk.codecenter.fault.ErrorCode;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.sdk.codecenter.fault.SdkFaultDetails;
import com.blackducksoftware.sdk.codecenter.request.RequestApi;
import com.blackducksoftware.sdk.codecenter.request.data.RequestApplicationComponentOrIdToken;
import com.blackducksoftware.sdk.codecenter.request.data.RequestApplicationComponentToken;
import com.blackducksoftware.sdk.codecenter.request.data.RequestCreate;
import com.blackducksoftware.sdk.codecenter.request.data.RequestSummary;
import com.blackducksoftware.sdk.protex.policy.PolicyApi;
import com.blackducksoftware.sdk.protex.project.ProjectApi;
import com.blackducksoftware.sdk.protex.project.bom.BomApi;
import com.blackducksoftware.sdk.protex.report.ReportApi;
import com.blackducksoftware.tools.ccimport.appadjuster.MockAppAdjuster;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIApplication;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterAPIWrapper;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.ProtexAPIWrapper;
import com.blackducksoftware.tools.connector.protex.ProtexServerWrapper;

public class SyncProjectTaskTest {
    private static final String APP_NAME = "testAppName";

    private static final String APP_VERSION = "Unspecified";

    private static final String APP_ID = "testAppId";

    private static final String PROJECT_ID = "testProjectId";

    private static CodeCenterConfigManager ccConfig;

    private static CodeCenterServerWrapper ccsw;

    private static ProtexServerWrapper<ProtexProjectPojo> psw;

    private static RequestApi mockRequestApi;

    private static Date lastRefreshDate = new Date();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Properties props = createBasicProperties(APP_NAME);

        ccConfig = new CodeCenterConfigManager(props);

        psw = createMockProtexApis();
        ccsw = createMockCodeCenterApis();
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws CommonFrameworkException, SdkFault,
            CodeCenterImportException {

        // Setup project object: populate project ID in project object
        CCIProject project = new CCIProject();
        project.setProjectName(APP_NAME);
        project.setProjectVersion(APP_VERSION);
        project.setProjectKey(PROJECT_ID);

        MockAppAdjuster mockAppAdjuster = new MockAppAdjuster();

        // Create task to test
        SyncProjectTask task = new SyncProjectTask(ccConfig, ccsw, psw,
                mockAppAdjuster, getAppAdjusterMethod(mockAppAdjuster), project);

        // Execute the task
        CCIReportSummary report = task.call();

        verify(mockRequestApi, times(1))
                .createRequest(any(RequestCreate.class));
        verify(mockRequestApi, times(2)).deleteRequest(
                any(RequestApplicationComponentOrIdToken.class));

        // Verify results
        System.out.println("Report: " + report);
        assertEquals(0, report.getFailedImportList().size());
        assertEquals(0, report.getFailedValidationList().size());
        assertEquals(0, report.getTotalImportsFailed().intValue());
        assertEquals(1, report.getTotalValidatesPerfomed().intValue());

        // make sure appAdjuster was called
        assertEquals(1, mockAppAdjuster.getAdjustAppCalledCount());
    }

    private static Properties createBasicProperties(String appName) {
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
        props.setProperty("protex.project.list", appName);
        props.setProperty("validate.application", "true");
        props.setProperty("validate.application.smart", "true");
        props.setProperty("cc.submit.request", "true");
        props.setProperty("validate.requests.delete", "true");
        return props;
    }

    private static ProtexServerWrapper<ProtexProjectPojo> createMockProtexApis()
            throws com.blackducksoftware.sdk.fault.SdkFault {

        // Mock wrappers and APIs
        ProtexServerWrapper<ProtexProjectPojo> psw = mock(ProtexServerWrapper.class);
        ProtexAPIWrapper mockApiWrapper = mock(ProtexAPIWrapper.class);
        ReportApi mockReportApi = mock(ReportApi.class);
        PolicyApi mockPolicyApi = mock(PolicyApi.class);
        ProjectApi mockProjectApi = mock(ProjectApi.class);
        BomApi mockBomApi = mock(BomApi.class);

        when(psw.getInternalApiWrapper()).thenReturn(mockApiWrapper);
        when(mockApiWrapper.getReportApi()).thenReturn(mockReportApi);
        when(mockApiWrapper.getPolicyApi()).thenReturn(mockPolicyApi);
        when(mockApiWrapper.getProjectApi()).thenReturn(mockProjectApi);
        when(mockApiWrapper.getBomApi()).thenReturn(mockBomApi);

        when(mockBomApi.getLastBomRefreshFinishDate(PROJECT_ID)).thenReturn(
                lastRefreshDate);

        return psw;
    }

    private static CodeCenterServerWrapper createMockCodeCenterApis()
            throws SdkFault {

        ApplicationApi mockApplicationApi = mock(ApplicationApi.class);
        mockRequestApi = mock(RequestApi.class);
        ColaApi mockColaApi = mock(ColaApi.class);
        CodeCenterAPIWrapper mockCodeCenterApiWrapper = mock(CodeCenterAPIWrapper.class);
        when(mockCodeCenterApiWrapper.getApplicationApi()).thenReturn(
                mockApplicationApi);
        CodeCenterServerWrapper ccsw = mock(CodeCenterServerWrapper.class);
        when(ccsw.getInternalApiWrapper()).thenReturn(mockCodeCenterApiWrapper);
        when(mockCodeCenterApiWrapper.getColaApi()).thenReturn(mockColaApi);
        when(mockCodeCenterApiWrapper.getRequestApi()).thenReturn(
                mockRequestApi);

        Application mockApplication = mock(Application.class);

        ApplicationIdToken appIdToken = new ApplicationIdToken();
        appIdToken.setId(APP_ID);
        when(mockApplication.getId()).thenReturn(appIdToken);
        when(mockApplication.getName()).thenReturn(APP_NAME);
        when(mockApplication.getVersion()).thenReturn(APP_VERSION);
        // when(
        // mockApplicationApi
        // .getAssociatedApplication(any(ProjectIdToken.class)))
        // .thenReturn(mockApplication);

        when(
                mockApplicationApi
                        .getAssociatedApplication(any(ProjectIdToken.class)))
                .thenThrow(new SdkFault("Application does not exist"));

        SdkFaultDetails excDetails = new SdkFaultDetails();
        excDetails
                .setErrorCode(ErrorCode.APPLICATION_NOT_ASSOCIATED_WITH_PROTEX_PROJECT);
        excDetails.setMessage("No associated project");
        SdkFault noAssociatedProjectException = new SdkFault(
                "Not associated yet", excDetails);

        ProjectIdToken mockProjectIdToken = mock(ProjectIdToken.class);
        when(mockProjectIdToken.getId()).thenReturn(PROJECT_ID);
        Project mockProject = mock(Project.class);
        when(mockProject.getId()).thenReturn(mockProjectIdToken);
        when(
                mockApplicationApi
                        .getAssociatedProtexProject(any(ApplicationIdToken.class)))
                .thenThrow(noAssociatedProjectException)
                .thenReturn(mockProject);

        when(mockApplicationApi.createApplication(any(ApplicationCreate.class)))
                .thenReturn(appIdToken);

        // Unfortunately, ApplicationIdToken seems not to have a reasonable
        // equals() method
        when(mockApplicationApi.getApplication(any(ApplicationIdToken.class)))
                .thenReturn(mockApplication);

        // Component adds
        List<ProtexRequest> protexOnlyComponentRequests = new ArrayList<>();
        ProtexRequest protexOnlyComponentRequest = new ProtexRequest();
        protexOnlyComponentRequest.setApplicationId(appIdToken);
        ComponentIdToken componentIdToken = new ComponentIdToken();
        componentIdToken.setId("testComponentToAddId");
        protexOnlyComponentRequest.setComponentId(componentIdToken);

        // protexRequest.getLicenseInfo().getId()
        LicenseSummary licenseSummary = new LicenseSummary();
        LicenseIdToken licenseIdToken = new LicenseIdToken();
        licenseIdToken.setId("testLicenseId");
        licenseSummary.setId(licenseIdToken);
        protexOnlyComponentRequest.setLicenseInfo(licenseSummary);
        protexOnlyComponentRequests.add(protexOnlyComponentRequest);
        when(
                mockApplicationApi
                        .getProtexOnlyComponentsFromLastValidation(any(ApplicationIdToken.class)))
                .thenReturn(protexOnlyComponentRequests);

        // Component deletes
        List<RequestSummary> ccOnlyComponentRequests = new ArrayList<>();
        RequestSummary ccOnlyComponentRequest = new RequestSummary();
        RequestApplicationComponentToken requestApplicationComponentToken = new RequestApplicationComponentToken();
        requestApplicationComponentToken.setApplicationId(appIdToken);
        requestApplicationComponentToken.setComponentId(componentIdToken);
        ccOnlyComponentRequest
                .setApplicationComponentToken(requestApplicationComponentToken);
        componentIdToken = new ComponentIdToken();
        componentIdToken.setId("testComponentToAddId");
        ccOnlyComponentRequest.setComponentId(componentIdToken);

        // protexRequest.getLicenseInfo().getId()
        licenseSummary = new LicenseSummary();
        licenseIdToken = new LicenseIdToken();
        licenseIdToken.setId("testLicenseId");
        licenseSummary.setId(licenseIdToken);
        ccOnlyComponentRequest.setLicenseInfo(licenseSummary);
        ccOnlyComponentRequests.add(ccOnlyComponentRequest);
        ccOnlyComponentRequests.add(ccOnlyComponentRequest);
        when(
                mockApplicationApi
                        .getCodeCenterOnlyComponentsFromLastValidation(any(ApplicationIdToken.class)))
                .thenReturn(ccOnlyComponentRequests);

        return ccsw;
    }

    private static Method getAppAdjusterMethod(Object appAdjusterObject)
            throws CodeCenterImportException {

        // Get the adjustApp method on the custom app adjuster class
        Class<?>[] adjustAppMethodArgTypes = { CCIApplication.class,
                CCIProject.class };
        Method appAdjusterMethod = null;
        try {
            appAdjusterMethod = MockAppAdjuster.class.getDeclaredMethod(
                    "adjustApp", adjustAppMethodArgTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get app adjuster method: No such method exception";
            throw new CodeCenterImportException(msg);
        }

        return appAdjusterMethod;
    }
}
