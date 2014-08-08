package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.*;
import junit.framework.Assert;

import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ExpectedSystemExit;

import soleng.framework.core.config.server.ServerBean;

import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccreport.datatable.DataTable;
import com.blackducksoftware.soleng.ccreport.datatable.Record;

public class ReportTest
{

    @Rule
    public final ExpectedSystemExit exit = ExpectedSystemExit.none();

    // Local config files
    CodeCenterConfigManager ccConfigManager = null;
    ProtexConfigManager protexConfigManager = null;

    @Test
    public void testReport() throws Exception
    { 	
    	String configPath = "src/test/resources/report.properties";

		ccConfigManager = new CodeCenterConfigManager(configPath);
	    protexConfigManager = new ProtexConfigManager(configPath);
	    
	    CCIProcessor processor = new CCISingleServerProcessor(ccConfigManager, protexConfigManager);
	    processor.runReport();

	    // TODO: make these checks more rigorous
	    
    	DataTable report = processor.getReportGen().getDataTable();
    	assertEquals(29, report.size());
    	
    	boolean foundMatch=false;
    	for (Record rec : report) {
    		if ("BestMatchId_Reference".equals(rec.getStringFieldValue("applicationName"))) {
    			assertEquals("Yes", rec.getStringFieldValue("compListsMatch"));
    			foundMatch=true;
    			break;
    		}
    	}
    	assertTrue(foundMatch);
    	System.out.println("testReport() Done2");
    }
}
