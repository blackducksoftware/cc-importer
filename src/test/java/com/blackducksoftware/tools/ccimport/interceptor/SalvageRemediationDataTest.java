package com.blackducksoftware.tools.ccimport.interceptor;

import static org.junit.Assert.assertEquals;

import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.tools.ccimport.deprecatedcomp.DeprecatedComponentReplacementTable;
import com.blackducksoftware.tools.ccimport.mocks.MockCodeCenterServerWrapper;
import com.blackducksoftware.tools.ccimport.mocks.MockDeprecatedComponentReplacementTable;
import com.blackducksoftware.tools.ccimport.mocks.MockRequestManager;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.codecenter.common.RequestVulnerabilityPojo;

public class SalvageRemediationDataTest {
    private static final String ADDED_COMP_ID = "addedCompId";

    private static final String DELETED_COMP_ID = "deletedCompId";

    private static final String APP_NAME = "testAppName";

    private static final String APP_ID = "testAppId";

    private static final String APP_VERSION = "Unspecified";

    private static CodeCenterConfigManager ccConfig;

    private static ICodeCenterServerWrapper ccsw;

    private static Date today = new Date();

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Properties props = createBasicProperties(APP_NAME);

        ccConfig = new CodeCenterConfigManager(props);
        ccsw = new MockCodeCenterServerWrapper(false, true, today);
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws InterceptorException {

        DeprecatedComponentReplacementTable table = new MockDeprecatedComponentReplacementTable(false);
        CompChangeInterceptor interceptor = new SalvageRemediationData(table, true, null);

        interceptor.init(ccConfig, ccsw, null); // this interceptor does not use Protex
        interceptor.initForApp(APP_ID);
        interceptor.preProcessAdd(ADDED_COMP_ID);
        interceptor.postProcessAdd("addRequestId", ADDED_COMP_ID);
        interceptor.preProcessDelete("deleteRequestId", DELETED_COMP_ID);

        MockRequestManager mockRequestManager = (MockRequestManager) ccsw.getRequestManager();
        List<RequestVulnerabilityPojo> ops = mockRequestManager.getUpdateOperations();
        assertEquals(1, ops.size());
        assertEquals("testVulnerabilityId", ops.get(0).getVulnerabilityId());
        assertEquals("testAddRequestId", ops.get(0).getRequestId());
        assertEquals(today, ops.get(0).getActualRemediationDate());
        assertEquals(today, ops.get(0).getTargetRemediationDate());
        assertEquals("REMEDIATED", ops.get(0).getReviewStatusName());
        assertEquals("test comments", ops.get(0).getComments());

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
}
