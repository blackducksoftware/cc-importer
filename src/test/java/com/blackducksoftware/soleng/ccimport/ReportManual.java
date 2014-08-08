package com.blackducksoftware.soleng.ccimport;

import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import soleng.framework.core.config.server.ServerBean;

import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;

/**
 * Tests all the CLI options.
 * 
 * @author akamen
 * 
 */
public class ReportManual
{

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    private CCIProjectImporterHarness harness = new CCIProjectImporterHarness();
    private  String[] args = null;
    
    // Local config files
    CodeCenterConfigManager ccConfigManager = null;
    ProtexConfigManager protexConfigManager = null;



    @Test
    public void testReport()
    {

    	String[] args = { "src/test/resources/report.properties" };
	
    	try {
    		harness.main(args);
    	} catch (Exception e) {
    		System.out.println("Error: " + e.getMessage());
    		StackTraceElement[] elements = e.getStackTrace();
    		for (StackTraceElement element : elements) {
    			System.out.println(element.toString());
    		}
    		
    	}
	
    }
}
