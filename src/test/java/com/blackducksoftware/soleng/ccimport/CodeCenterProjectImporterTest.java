package com.blackducksoftware.soleng.ccimport;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

/**
 * Tests all the CLI options.
 * @author akamen
 *
 */
public class CodeCenterProjectImporterTest {

	@Rule
	public final ExpectedSystemExit exit = ExpectedSystemExit.none();
	
	CCProjectImporterHarness importer = new CCProjectImporterHarness();
	String[] args = null;
	
	/**
	 * Expecting a system exit due to insufficient arguments
	 */
	@Test
	public void testBasicNoArguments()
	{	
		exit.expectSystemExitWithStatus(-1);
		args = new String[]{};
		importer.main(args);		
	}
		
	
}
