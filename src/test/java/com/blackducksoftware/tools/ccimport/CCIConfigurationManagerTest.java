/*******************************************************************************
 * Copyright (C) 2015 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *******************************************************************************/

package com.blackducksoftware.tools.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;

public class CCIConfigurationManagerTest {
    private static final String APP_VERSION = "Unspecified";
    private static final String APP_NAME1 = "ccimport IT app";
    private static final String APP_NAME2 = "another app";

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() {
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
	props.setProperty("cc.default.app.version", APP_VERSION);
	props.setProperty("cc.workflow", "notused");
	props.setProperty("cc.owner", "notused");
	props.setProperty("protex.project.list", APP_NAME1);
	props.setProperty("validate.application", "true");
	props.setProperty("cc.submit.request", "true");
	props.setProperty("validate.requests.delete", "true");
	props.setProperty("revalidate.after.changing.bom", "true");
	props.setProperty("app.adjuster.only.if.bomedits", "true");
	props.setProperty("protex.project.name.filter", ".*-CURRENT$");

	CodeCenterConfigManager ccConfig = new CodeCenterConfigManager(props);

	assertTrue(ccConfig.isReValidateAfterBomChange());
	assertTrue(ccConfig.isAppAdjusterOnlyIfBomEdits());

	ProtexConfigManager pConfig = new ProtexConfigManager(props);

	assertTrue(Pattern.compile(".*-CURRENT$").toString()
		.equals(pConfig.getProtexProjectNameFilterPattern().toString()));
    }

    @Test
    public void testAddDeleteSubmit() {
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
	props.setProperty("cc.default.app.version", APP_VERSION);
	props.setProperty("cc.workflow", "notused");
	props.setProperty("cc.owner", "notused");
	props.setProperty("protex.project.list", APP_NAME1);
	props.setProperty("validate.application", "true");

	props.setProperty("cc.submit.request", "false");
	props.setProperty("validate.requests.add", "false");
	props.setProperty("validate.requests.delete", "false");
	CodeCenterConfigManager ccConfig = new CodeCenterConfigManager(props);
	assertFalse(ccConfig.isPerformAdd());
	assertFalse(ccConfig.isPerformDelete());
	assertFalse(ccConfig.isSubmit());

	props.setProperty("validate.requests.add", "true");
	props.setProperty("validate.requests.delete", "true");
	props.setProperty("cc.submit.request", "true");
	ccConfig = new CodeCenterConfigManager(props);
	assertTrue(ccConfig.isPerformAdd());
	assertTrue(ccConfig.isPerformDelete());
	assertTrue(ccConfig.isSubmit());

	props.setProperty("validate.requests.add", "true");
	props.setProperty("validate.requests.delete", "true");
	props.setProperty("cc.submit.request", "false");
	ccConfig = new CodeCenterConfigManager(props);
	assertTrue(ccConfig.isPerformAdd());
	assertTrue(ccConfig.isPerformDelete());
	assertFalse(ccConfig.isSubmit());

	props.setProperty("validate.requests.add", "false");
	props.setProperty("validate.requests.delete", "true");
	props.setProperty("cc.submit.request", "true");
	ccConfig = new CodeCenterConfigManager(props);
	assertFalse(ccConfig.isPerformAdd());
	assertTrue(ccConfig.isPerformDelete());
	assertTrue(ccConfig.isSubmit());
    }

    @Test
    public void testSpaceAfterComma() {
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
	props.setProperty("cc.default.app.version", APP_VERSION);
	props.setProperty("cc.workflow", "notused");
	props.setProperty("cc.owner", "notused");
	props.setProperty("protex.project.list", APP_NAME1 + ", " + APP_NAME2);
	props.setProperty("validate.application", "true");
	props.setProperty("cc.submit.request", "true");
	props.setProperty("validate.requests.delete", "true");
	props.setProperty("revalidate.after.changing.bom", "true");
	props.setProperty("app.adjuster.only.if.bomedits", "true");
	props.setProperty("protex.project.name.filter", ".*-CURRENT$");

	CodeCenterConfigManager ccConfig = new CodeCenterConfigManager(props);

	List<CCIProject> projects = ccConfig.getProjectList();

	// Make sure 2nd app is in list, and without leading space
	boolean foundSecondApp = false;
	for (CCIProject project : projects) {
	    if (project.getProjectName().contains("another")) {
		assertEquals("another app", project.getProjectName());
		foundSecondApp = true;
	    }
	}
	assertTrue(foundSecondApp);

    }

}
