/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import static org.junit.Assert.*;

import java.util.Date;
import java.util.Properties;
import java.util.TimeZone;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import soleng.framework.core.config.ConfigConstants.APPLICATION;

import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;

public class NumericPrefixedAppAdjusterTest {
	private static final long TIME_VALUE_OF_JAN1_2000 = 946702800000L;
	
	@Test
	public void testDefaultPatterns() throws Exception {
		NumericPrefixedAppMetadata metadata;
		
		Properties props = new Properties();
		props.setProperty("cc.user.name", "notused");
		props.setProperty("cc.server.name", "notused");
		props.setProperty("cc.password", "notused");
		props.setProperty("numprefixed.app.attribute.numericprefix", "notused");
		props.setProperty("numprefixed.app.attribute.workstream", "notused");
		props.setProperty("numprefixed.app.attribute.analyzeddate", "notused");
		props.setProperty("numprefixed.analyzed.date.format", "yyyy-mm-dd");
		props.setProperty("numprefixed.app.attribute.projectstatus", "null");
		props.setProperty("numprefixed.app.value.projectstatus", "CURRENT");
		CCIConfigurationManager config = new CCIConfigurationManager(props, APPLICATION.CODECENTER);
		NumericPrefixedAppAdjuster adjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		adjuster.init(null, config, tz);
		
		metadata = adjuster.parse("123-some application-PROD-CURRENT");
		assertEquals("123", metadata.getNumericPrefix());
		assertEquals("some application", metadata.getAppNameString());
		assertEquals("PROD", metadata.getWorkStream());
		
		metadata = adjuster.parse("1234-PROD-CURRENT");
		assertEquals("1234", metadata.getNumericPrefix());
		assertEquals("", metadata.getAppNameString());
		assertEquals("PROD", metadata.getWorkStream());
		
		metadata = adjuster.parse("12345-some application-RC1-CURRENT");
		assertEquals("12345", metadata.getNumericPrefix());
		assertEquals("some application", metadata.getAppNameString());
		assertEquals("RC1", metadata.getWorkStream());
		
		metadata = adjuster.parse("123-some application.;;/-RC5-CURRENT");
		assertEquals("123", metadata.getNumericPrefix());
		assertEquals("some application.;;/", metadata.getAppNameString());
		assertEquals("RC5", metadata.getWorkStream());
	}
	
	@Test
	public void testConfiguredPatterns() throws Exception {
		NumericPrefixedAppMetadata metadata;
		
		Properties props = new Properties();
		props.setProperty("cc.user.name", "notused");
		props.setProperty("cc.server.name", "notused");
		props.setProperty("cc.password", "notused");
		props.setProperty("numprefixed.appname.pattern.separator", "_");
		props.setProperty("numprefixed.appname.pattern.numericprefix", "[a-z][a-z][a-z]+");
		props.setProperty("numprefixed.appname.pattern.workstream", "(PPPP|RCA|RCB|RCC|RCD|RCE)");
		props.setProperty("numprefixed.app.attribute.numericprefix", "notused");
		props.setProperty("numprefixed.app.attribute.workstream", "notused");
		props.setProperty("numprefixed.app.attribute.analyzeddate", "notused");
		props.setProperty("numprefixed.analyzed.date.format", "yyyy-mm-dd");
		props.setProperty("numprefixed.app.attribute.projectstatus", "null");
		props.setProperty("numprefixed.app.value.projectstatus", "CURRENT");
		
		props.setProperty("numprefixed.app.name.format.without.description", "[a-z][a-z][a-z]+_(PPPP|RCA|RCB|RCC|RCD|RCE)_CURRENT");
		props.setProperty("numprefixed.app.name.format.with.description", "[a-z][a-z][a-z]+_.*_(PPPP|RCA|RCB|RCC|RCD|RCE)_CURRENT");
		props.setProperty("numprefixed.appname.pattern.follows.description", "_(PPPP|RCA|RCB|RCC|RCD|RCE)_CURRENT");
		
		CCIConfigurationManager config = new CCIConfigurationManager(props, APPLICATION.CODECENTER);
		NumericPrefixedAppAdjuster adjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		adjuster.init(null, config, tz);
		
		metadata = adjuster.parse("abc_123_PPPP_CURRENT");
		assertEquals("abc", metadata.getNumericPrefix());
		assertEquals("123", metadata.getAppNameString());
		assertEquals("PPPP", metadata.getWorkStream());
		
		metadata = adjuster.parse("abcd_PPPP_CURRENT");
		assertEquals("abcd", metadata.getNumericPrefix());
		assertEquals("", metadata.getAppNameString());
		assertEquals("PPPP", metadata.getWorkStream());
		
		metadata = adjuster.parse("abcde_456_RCA_CURRENT");
		assertEquals("abcde", metadata.getNumericPrefix());
		assertEquals("456", metadata.getAppNameString());
		assertEquals("RCA", metadata.getWorkStream());
		
		metadata = adjuster.parse("abc_789.;;/_RCE_CURRENT");
		assertEquals("abc", metadata.getNumericPrefix());
		assertEquals("789.;;/", metadata.getAppNameString());
		assertEquals("RCE", metadata.getWorkStream());
	}
	
	@Test
	public void testDateFormatting() {
		String dateFormat = "yyyy-MM-dd HH:mm:ss";

		Properties props = new Properties();
		props.setProperty("cc.user.name", "notused");
		props.setProperty("cc.server.name", "notused");
		props.setProperty("cc.password", "notused");
		props.setProperty("numprefixed.appname.pattern.separator", "_");
		props.setProperty("numprefixed.appname.pattern.numericprefix", "[a-z][a-z][a-z]+");
		props.setProperty("numprefixed.appname.pattern.workstream", "(PPPP|RCA|RCB|RCC|RCD|RCE)");
		props.setProperty("numprefixed.app.attribute.numericprefix", "notused");
		props.setProperty("numprefixed.app.attribute.workstream", "notused");
		props.setProperty("numprefixed.app.attribute.analyzeddate", "notused");
		props.setProperty("numprefixed.analyzed.date.format", dateFormat);
		props.setProperty("numprefixed.app.attribute.projectstatus", "null");
		props.setProperty("numprefixed.app.value.projectstatus", "CURRENT");
		
		CCIConfigurationManager config = new CCIConfigurationManager(props, APPLICATION.CODECENTER);
		NumericPrefixedAppAdjuster adjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		adjuster.init(null, config, tz);
		
		String generatedDate = adjuster.getDateString(new Date(TIME_VALUE_OF_JAN1_2000), TimeZone.getDefault(), dateFormat);
		assertEquals("2000-01-01 00:00:00", generatedDate);
	}
	
	@Test
	public void testOnNonConformingAppNames() throws Exception {
		
		Properties props = new Properties();
		props.setProperty("cc.user.name", "notused");
		props.setProperty("cc.server.name", "notused");
		props.setProperty("cc.password", "notused");
		props.setProperty("numprefixed.app.attribute.numericprefix", "notused");
		props.setProperty("numprefixed.app.attribute.workstream", "notused");
		props.setProperty("numprefixed.app.attribute.analyzeddate", "notused");
		props.setProperty("numprefixed.analyzed.date.format", "yyyy-mm-dd");
		props.setProperty("numprefixed.app.attribute.projectstatus", "null");
		props.setProperty("numprefixed.app.value.projectstatus", "CURRENT");
		CCIConfigurationManager config = new CCIConfigurationManager(props, APPLICATION.CODECENTER);
		NumericPrefixedAppAdjuster adjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		adjuster.init(null, config, tz);
		
		try {
			adjuster.parse("30165-Chargeback imaging system");
			fail("adjuster.parse should have thrown an exception");
		} catch (Exception e) {
			// expected
		}
	}

	
}
