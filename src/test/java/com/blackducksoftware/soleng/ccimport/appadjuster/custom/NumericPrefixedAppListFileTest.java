/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.soleng.ccimport.appadjuster.custom.NumericPrefixedAppListFile;

public class NumericPrefixedAppListFileTest {
	private static final String expectedListContents = "test app 1\ntest app 2\n";
	private static File outputFile;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		outputFile = File.createTempFile("JUnit_AppListFile_", ".txt");
	}

	@AfterClass
	public static void tearDownAfterClass() throws Exception {
		outputFile.delete();
	}

	@Test
	public void test() throws Exception {
		
		NumericPrefixedAppListFile appList = new NumericPrefixedAppListFile();
		appList.addApp("test app 1");
		appList.addApp("test app 2");
		assertEquals(expectedListContents, appList.toString());
		appList.save(outputFile.getAbsolutePath());
		
		BufferedReader br = new BufferedReader(new FileReader(outputFile.getAbsoluteFile()));
		String line = br.readLine();
		assertEquals("test app 1", line);
		line = br.readLine();
		assertEquals("test app 2", line);
	}

}
