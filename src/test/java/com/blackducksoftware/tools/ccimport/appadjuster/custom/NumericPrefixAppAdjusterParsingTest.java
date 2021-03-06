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

package com.blackducksoftware.tools.ccimport.appadjuster.custom;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import java.util.TimeZone;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;

public class NumericPrefixAppAdjusterParsingTest {

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
    }

    @AfterClass
    public static void tearDownAfterClass() throws Exception {
    }

    @Test
    public void test() throws Exception {
	NumericPrefixedAppMetadata metadata;

	Properties props = new Properties();
	props.setProperty("cc.user.name", "notused");
	props.setProperty("cc.server.name", "notused");
	props.setProperty("cc.password", "notused");
	props.setProperty("cc.protex.name", "notused");
	props.setProperty("cc.owner", "notused");
	props.setProperty("cc.default.app.version", "notused");
	props.setProperty("cc.workflow", "notused");
	props.setProperty("numprefixed.appname.pattern.separator", "-");
	props.setProperty("numprefixed.appname.pattern.numericprefix",
		"[0-9][0-9][0-9]+");
	props.setProperty("numprefixed.appname.pattern.workstream",
		"(PROD|RC1|RC2|RC3|RC4|RC5)");
	props.setProperty("numprefixed.app.attribute.numericprefix", "notused");
	props.setProperty("numprefixed.app.attribute.workstream", "notused");
	props.setProperty("numprefixed.app.attribute.analyzeddate", "notused");
	props.setProperty("numprefixed.analyzed.date.format", "yyyy-mm-dd");
	props.setProperty("numprefixed.app.attribute.projectstatus", "null");
	props.setProperty("numprefixed.app.value.projectstatus", "CURRENT");

	props.setProperty("numprefixed.app.name.format.without.description",
		"[0-9][0-9][0-9]+-(PROD|RC1|RC2|RC3|RC4|RC5)-CURRENT");
	props.setProperty("numprefixed.app.name.format.with.description",
		"[0-9][0-9][0-9]+-.*-(PROD|RC1|RC2|RC3|RC4|RC5)-CURRENT");
	props.setProperty("numprefixed.appname.pattern.follows.description",
		"-(PROD|RC1|RC2|RC3|RC4|RC5)-CURRENT");

	CCIConfigurationManager config = new CodeCenterConfigManager(props);
	NumericPrefixedAppAdjuster adjuster = new NumericPrefixedAppAdjuster();
	TimeZone tz = TimeZone.getDefault();
	adjuster.init(null, null, config, tz);

	metadata = adjuster.parse("79926-PROD-CURRENT");
	assertEquals("79926", metadata.getNumericPrefix());
	assertEquals("", metadata.getAppNameString());
	assertEquals("PROD", metadata.getWorkStream());

	metadata = adjuster.parse("79926-FI eTrading - Execution-PROD-CURRENT");
	assertEquals("79926", metadata.getNumericPrefix());
	assertEquals("FI eTrading - Execution", metadata.getAppNameString());
	assertEquals("PROD", metadata.getWorkStream());

	metadata = adjuster
		.parse("25776-RDT-Static Data Maintenance (SDM)-PROD-CURRENT");
	assertEquals("25776", metadata.getNumericPrefix());
	assertEquals("RDT-Static Data Maintenance (SDM)",
		metadata.getAppNameString());
	assertEquals("PROD", metadata.getWorkStream());

	System.out.println("Done");
    }

}
