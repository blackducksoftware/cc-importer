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
import com.blackducksoftware.tools.ccimport.mocks.MockCompChangeInterceptor;
import com.blackducksoftware.tools.ccimport.mocks.MockProtexServerWrapper;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public class PluginManagerTest {

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
        Properties props = getPropertiesRealInterceptor();
        CodeCenterConfigManager config = new CodeCenterConfigManager(props);
        ICodeCenterServerWrapper ccWrapper = new MockCodeCenterServerWrapper(false, true, new Date());
        IProtexServerWrapper<ProtexProjectPojo> protexWrapper = new MockProtexServerWrapper();
        PlugInManager plugInManager = new PlugInManager(config, ccWrapper, protexWrapper);

        Object appAdjusterObject = plugInManager.initAppAdjusterObject();
        assertEquals(APP_ADJUSTER_CLASSNAME, appAdjusterObject.getClass().getName());

        Method adjustAppMethod = plugInManager.initAppAdjusterAdjustAppMethod();

        assertEquals("adjustApp", adjustAppMethod.getName());
    }

    @Test
    public void testCompChangeInterceptorInit() throws CodeCenterImportException {
        Properties props = getPropertiesRealInterceptor();
        CodeCenterConfigManager config = new CodeCenterConfigManager(props);
        ICodeCenterServerWrapper ccWrapper = new MockCodeCenterServerWrapper(false, true, new Date());
        IProtexServerWrapper<ProtexProjectPojo> protexWrapper = new MockProtexServerWrapper();
        PlugInManager plugInManager = new PlugInManager(config, ccWrapper, protexWrapper);

        Object interceptorObject = plugInManager.initComponentChangeInterceptorObject();
        assertEquals(SALVAGE_REM_DATA_CLASSNAME, interceptorObject.getClass().getName());

        Method initMethod = plugInManager.initComponentChangeInterceptorInitMethod();
        assertEquals("init", initMethod.getName());

        Method initForAppMethod = plugInManager.initComponentChangeInterceptorInitForAppMethod();
        assertEquals("initForApp", initForAppMethod.getName());

        Method preProcessAddMethod = plugInManager.initComponentChangeInterceptorPreProcessAddMethod();
        assertEquals("preProcessAdd", preProcessAddMethod.getName());

        Method postProcessAddMethod = plugInManager.initComponentChangeInterceptorPostProcessAddMethod();
        assertEquals("postProcessAdd", postProcessAddMethod.getName());

        Method preProcessDeleteMethod = plugInManager.initComponentChangeInterceptorPreProcessDeleteMethod();
        assertEquals("preProcessDelete", preProcessDeleteMethod.getName());
    }

    @Test
    public void testSalvageRemDataInterceptor() throws CodeCenterImportException {
        Properties props = getPropertiesMockInterceptor();
        CodeCenterConfigManager config = new CodeCenterConfigManager(props);
        ICodeCenterServerWrapper ccWrapper = new MockCodeCenterServerWrapper(false, true, new Date());
        IProtexServerWrapper<ProtexProjectPojo> protexWrapper = new MockProtexServerWrapper();
        PlugInManager plugInManager = new PlugInManager(config, ccWrapper, protexWrapper);

        plugInManager.invokeComponentChangeInterceptorInitMethod();
        plugInManager.invokeComponentChangeInterceptorInitForAppMethod("testAppId1");
        plugInManager.invokeComponentChangeInterceptorPreProcessAddMethod("addedComp1");
        plugInManager.invokeComponentChangeInterceptorPostProcessAddMethod("addedRequest1", "addedComp1");
        plugInManager.invokeComponentChangeInterceptorPreProcessDeleteMethod("deletedRequest1", "deletedComp1");

        plugInManager.invokeComponentChangeInterceptorInitForAppMethod("testAppId2");
        plugInManager.invokeComponentChangeInterceptorPreProcessAddMethod("addedComp2");
        plugInManager.invokeComponentChangeInterceptorPostProcessAddMethod("addedRequest2", "addedComp2");
        plugInManager.invokeComponentChangeInterceptorPreProcessDeleteMethod("deletedRequest2", "deletedComp2");

        plugInManager.invokeComponentChangeInterceptorPreProcessDeleteMethod("deletedRequest3", "deletedComp3");

        MockCompChangeInterceptor mockCompChangeInterceptor = (MockCompChangeInterceptor) plugInManager.getComponentChangeInterceptorObject();

        assertEquals(1, mockCompChangeInterceptor.getCallCountInit());

        assertEquals(2, mockCompChangeInterceptor.getCallsInitForApp().size());
        assertEquals("testAppId1", mockCompChangeInterceptor.getCallsInitForApp().get(0));
        assertEquals("testAppId2", mockCompChangeInterceptor.getCallsInitForApp().get(1));

        assertEquals(2, mockCompChangeInterceptor.getCallsPreProcessAdd().size());
        assertEquals("testAppId1|addedComp1", mockCompChangeInterceptor.getCallsPreProcessAdd().get(0));
        assertEquals("testAppId2|addedComp2", mockCompChangeInterceptor.getCallsPreProcessAdd().get(1));

        assertEquals(2, mockCompChangeInterceptor.getCallsPostProcessAdd().size());
        assertEquals("testAppId1|addedRequest1|addedComp1", mockCompChangeInterceptor.getCallsPostProcessAdd().get(0));
        assertEquals("testAppId2|addedRequest2|addedComp2", mockCompChangeInterceptor.getCallsPostProcessAdd().get(1));

        assertEquals(3, mockCompChangeInterceptor.getCallsPreProcessDelete().size());
        assertEquals("testAppId1|deletedRequest1|deletedComp1", mockCompChangeInterceptor.getCallsPreProcessDelete().get(0));
        assertEquals("testAppId2|deletedRequest2|deletedComp2", mockCompChangeInterceptor.getCallsPreProcessDelete().get(1));
        assertEquals("testAppId2|deletedRequest3|deletedComp3", mockCompChangeInterceptor.getCallsPreProcessDelete().get(2));
    }

    private Properties getPropertiesRealInterceptor() {
        Properties props = getBasicProperties();

        props.setProperty("component.change.interceptor.classname", SALVAGE_REM_DATA_CLASSNAME);
        return props;
    }

    private Properties getPropertiesMockInterceptor() {
        Properties props = getBasicProperties();

        props.setProperty("component.change.interceptor.classname", "com.blackducksoftware.tools.ccimport.mocks.MockCompChangeInterceptor");
        return props;
    }

    private Properties getBasicProperties() {
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
