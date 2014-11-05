/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import static org.junit.Assert.*;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

public class JpmcAppAdjusterTest {

	@Test
	public void test() throws Exception {
		JpmcAppMetadata metadata;
		
		metadata = JpmcAppAdjuster.parse("123-some application-PROD-current");
		assertEquals("123", metadata.getSealId());
		assertEquals("some application", metadata.getAppNameString());
		assertEquals("PROD", metadata.getWorkStream());
		
		metadata = JpmcAppAdjuster.parse("1234--PROD-current");
		assertEquals("1234", metadata.getSealId());
		assertEquals("", metadata.getAppNameString());
		assertEquals("PROD", metadata.getWorkStream());
		
		metadata = JpmcAppAdjuster.parse("12345-some application-RC1-current");
		assertEquals("12345", metadata.getSealId());
		assertEquals("some application", metadata.getAppNameString());
		assertEquals("RC1", metadata.getWorkStream());
		
		metadata = JpmcAppAdjuster.parse("123-some application.;;/-RC5");
		assertEquals("123", metadata.getSealId());
		assertEquals("some application.;;/", metadata.getAppNameString());
		assertEquals("RC5", metadata.getWorkStream());
	}

}
