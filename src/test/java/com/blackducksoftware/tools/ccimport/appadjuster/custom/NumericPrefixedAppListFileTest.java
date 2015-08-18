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

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.blackducksoftware.tools.ccimport.appadjuster.custom.NumericPrefixedAppListFile;

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

	BufferedReader br = new BufferedReader(new FileReader(
		outputFile.getAbsoluteFile()));
	String line = br.readLine();
	assertEquals("test app 1", line);
	line = br.readLine();
	assertEquals("test app 2", line);
    }

}
