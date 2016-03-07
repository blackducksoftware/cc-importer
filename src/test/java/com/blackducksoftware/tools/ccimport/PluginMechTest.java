package com.blackducksoftware.tools.ccimport;

import static org.junit.Assert.assertEquals;

import java.lang.reflect.Method;
import java.util.Date;
import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.mocks.MockCodeCenterServerWrapper;
import com.blackducksoftware.tools.ccimport.mocks.MockProtexServerWrapper;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public class PluginMechTest {

    private static final String APP_ADJUSTER_CLASSNAME = "com.blackducksoftware.tools.ccimport.appadjuster.custom.NumericPrefixedAppAdjuster";

    private static final String SALVAGE_REM_DATA_CLASSNAME = "com.blackducksoftware.tools.ccimport.interceptor.SalvageRemediationData";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void testAppAdjuster() throws CodeCenterImportException {
        Properties props = getProperties();
        CodeCenterConfigManager config = new CodeCenterConfigManager(props);
        Object appAdjusterObject = PlugInManager.getAppAdjusterObject(config);
        assertEquals(APP_ADJUSTER_CLASSNAME, appAdjusterObject.getClass().getName());

        ICodeCenterServerWrapper ccWrapper = new MockCodeCenterServerWrapper(false, true, new Date());
        IProtexServerWrapper<ProtexProjectPojo> protexWrapper = new MockProtexServerWrapper();
        Method adjustAppMethod = PlugInManager.getAppAdjusterMethod(ccWrapper, protexWrapper, config, appAdjusterObject);

        assertEquals("adjustApp", adjustAppMethod.getName());
    }

    @Test
    public void testCompChangeInterceptor() throws CodeCenterImportException {
        Properties props = getProperties();
        props.setProperty("protex.db.server", "tbd");

        CCIConfigurationManager config = new CodeCenterConfigManager(props);

        Object interceptorObject = PlugInManager.getCompChangeInterceptorObject(config);
        assertEquals(SALVAGE_REM_DATA_CLASSNAME, interceptorObject.getClass().getName());

        ICodeCenterServerWrapper ccWrapper = new MockCodeCenterServerWrapper(false, true, new Date());
        IProtexServerWrapper<ProtexProjectPojo> protexWrapper = new MockProtexServerWrapper();

        Method initMethod = PlugInManager.getCompChangeInterceptorInitMethod(ccWrapper,
                protexWrapper,
                config, interceptorObject);
        assertEquals("init", initMethod.getName());

        Method initForAppMethod = PlugInManager.getCompChangeInterceptorInitForAppMethod(ccWrapper,
                protexWrapper,
                config, interceptorObject);

        assertEquals("initForApp", initForAppMethod.getName());
    }

    private Properties getProperties() {
        Properties props = new Properties();
        props.setProperty("protex.server.name", "notused");
        props.setProperty("protex.user.name", "notused");
        props.setProperty("protex.password", "notused");
        props.setProperty("cc.server.name", "notused");
        props.setProperty("cc.user.name", "notused");
        props.setProperty("cc.password", "notused");
        props.setProperty("protex.password.isencrypted", "false");
        props.setProperty("cc.password.isencrypted", "false");
        props.setProperty("cc.protex.name", "notused");
        props.setProperty("cc.default.app.version", "Unspecified");
        props.setProperty("cc.workflow", "notused");
        props.setProperty("cc.owner", "notused");
        props.setProperty("app.adjuster.classname", APP_ADJUSTER_CLASSNAME);
        props.setProperty("component.change.interceptor.classname", SALVAGE_REM_DATA_CLASSNAME);
        props.setProperty("numprefixed.app.attribute.numericprefix",
                "null");
        props.setProperty("numprefixed.app.attribute.analyzeddate", "null");
        props.setProperty("numprefixed.app.attribute.workstream", "null");
        props.setProperty("numprefixed.app.attribute.projectstatus", "null");
        props.setProperty("numprefixed.analyzed.date.format", "MM-dd-yyyy");
        props.setProperty("numprefixed.appname.pattern.separator", "-");
        props.setProperty("numprefixed.appname.pattern.numericprefix",
                "[0-9][0-9][0-9]+");
        props.setProperty("numprefixed.appname.pattern.workstream",
                "(PROD|RC1|RC2|RC3|RC4|RC5)");

        props.setProperty("numprefixed.new.app.list.filename",
                "new_app_list.txt");
        return props;
    }
}
