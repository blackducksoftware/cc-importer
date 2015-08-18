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

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import com.blackducksoftware.tools.ccimport.CCIProjectImporterHarness;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;

/**
 * Tests all the CLI options.
 * 
 * @author akamen
 * 
 */
public class CodeCenterProjectHarnessTest {
    // OUR TEST DATA
    private static String PROTEX_SERVER = "protex/server";
    private static String PROTEX_USER = "akamen";
    private static String PROTEX_PASS = "blackDu@ckkk";

    private static String CC_SERVER = "codeceNter/server";
    private static String CC_USER = "akamen@cc.com";
    private static String CC_PASS = "cc_blackDu@ckkk";

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private CCIProjectImporterHarness harness = new CCIProjectImporterHarness();
    private String[] args = null;

    // Local config files
    CodeCenterConfigManager ccConfigManager = null;
    ProtexConfigManager protexConfigManager = null;

    /**
     * Expecting a system exit due to insufficient arguments
     */
    @Test
    public void testBasicNoArguments() {
	exit.expectSystemExitWithStatus(-1);
	args = new String[] {};
	harness.main(args);
    }

    @Test
    public void testBasicArgumentsComplete() {
	String NEWLINE = "\n";

	StringBuilder sb = new StringBuilder();
	sb.append("--p-server" + NEWLINE + PROTEX_SERVER);
	sb.append("\n");
	sb.append("--p-username" + NEWLINE + PROTEX_USER);
	sb.append("\n");
	sb.append("--p-password" + NEWLINE + PROTEX_PASS);
	sb.append("\n");
	sb.append("--cc-server" + NEWLINE + CC_SERVER);
	sb.append("\n");
	sb.append("--cc-username" + NEWLINE + CC_USER);
	sb.append("\n");
	sb.append("--cc-password" + NEWLINE + CC_PASS);
	sb.append("\n");
	sb.append("--protex-name" + NEWLINE + "Protex7Test");
	sb.append("\n");
	sb.append("--default-app-version" + NEWLINE + "2.9.9");
	sb.append("\n");
	sb.append("--workflow" + NEWLINE + "My Workflow");
	sb.append("\n");
	sb.append("--owner" + NEWLINE + "akamen");
	sb.append("\n");
	sb.append("--submit" + NEWLINE + "true");
	sb.append("\n");
	sb.append("--project");
	sb.append("\n");
	sb.append("--validate" + NEWLINE + "false");

	String[] args = sb.toString().split("\n");

	ccConfigManager = new CodeCenterConfigManager(args);
	protexConfigManager = new ProtexConfigManager(args);

	ServerBean ccBean = ccConfigManager.getServerBean();
	// Test config
	Assert.assertEquals(CC_SERVER, ccBean.getServerName());
	Assert.assertEquals(CC_USER, ccBean.getUserName());
	Assert.assertEquals(CC_PASS, ccBean.getPassword());

	ServerBean protexBean = protexConfigManager.getServerBean();

	Assert.assertEquals(PROTEX_SERVER, protexBean.getServerName());
	Assert.assertEquals(PROTEX_USER, protexBean.getUserName());
	Assert.assertEquals(PROTEX_PASS, protexBean.getPassword());

	// Test the remainder
	Assert.assertEquals("My Workflow", ccConfigManager.getWorkflow());
    }
}
