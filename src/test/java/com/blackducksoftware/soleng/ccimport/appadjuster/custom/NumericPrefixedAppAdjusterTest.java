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
		CCIConfigurationManager config = new CCIConfigurationManager(props, APPLICATION.CODECENTER);
		NumericPrefixedAppAdjuster adjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		adjuster.init(null, config, tz);
		
		metadata = adjuster.parse("123-some application-PROD-current");
		assertEquals("123", metadata.getNumericPrefix());
		assertEquals("some application", metadata.getAppNameString());
		assertEquals("PROD", metadata.getWorkStream());
		
		metadata = adjuster.parse("1234--PROD-current");
		assertEquals("1234", metadata.getNumericPrefix());
		assertEquals("", metadata.getAppNameString());
		assertEquals("PROD", metadata.getWorkStream());
		
		metadata = adjuster.parse("12345-some application-RC1-current");
		assertEquals("12345", metadata.getNumericPrefix());
		assertEquals("some application", metadata.getAppNameString());
		assertEquals("RC1", metadata.getWorkStream());
		
		metadata = adjuster.parse("123-some application.;;/-RC5");
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
		
		CCIConfigurationManager config = new CCIConfigurationManager(props, APPLICATION.CODECENTER);
		NumericPrefixedAppAdjuster adjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		adjuster.init(null, config, tz);
		
		metadata = adjuster.parse("abc_some application_PPPP_current");
		assertEquals("abc", metadata.getNumericPrefix());
		assertEquals("some application", metadata.getAppNameString());
		assertEquals("PPPP", metadata.getWorkStream());
		
		metadata = adjuster.parse("abcd__PPPP_current");
		assertEquals("abcd", metadata.getNumericPrefix());
		assertEquals("", metadata.getAppNameString());
		assertEquals("PPPP", metadata.getWorkStream());
		
		metadata = adjuster.parse("abcde_some application_RCA_current");
		assertEquals("abcde", metadata.getNumericPrefix());
		assertEquals("some application", metadata.getAppNameString());
		assertEquals("RCA", metadata.getWorkStream());
		
		metadata = adjuster.parse("abc_some application.;;/_RCE");
		assertEquals("abc", metadata.getNumericPrefix());
		assertEquals("some application.;;/", metadata.getAppNameString());
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
		
		CCIConfigurationManager config = new CCIConfigurationManager(props, APPLICATION.CODECENTER);
		NumericPrefixedAppAdjuster adjuster = new NumericPrefixedAppAdjuster();
		TimeZone tz = TimeZone.getDefault();
		adjuster.init(null, config, tz);
		
		String generatedDate = adjuster.getDateString(new Date(TIME_VALUE_OF_JAN1_2000), TimeZone.getDefault(), dateFormat);
		assertEquals("2000-01-01 00:00:00", generatedDate);
	}

}
