package com.blackducksoftware.tools.ccimport;

import static org.junit.Assert.assertEquals;

import java.util.Properties;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;

public class AppAdjusterPluginMechTest {

    private static final String APP_ADJUSTER_CLASSNAME = "com.blackducksoftware.tools.ccimport.appadjuster.custom.NumericPrefixedAppAdjuster";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws CodeCenterImportException {
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
        CodeCenterConfigManager config = new CodeCenterConfigManager(props);
        Object object = CCIProjectImporterHarness.getAppAdjusterObject(config);
        assertEquals(APP_ADJUSTER_CLASSNAME, object.getClass().getName());
    }

}
