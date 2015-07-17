package com.blackducksoftware.soleng.ccimport;

import static org.junit.Assert.assertEquals;

import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.connector.protex.ProtexServerWrapper;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationNameVersionToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.sdk.codecenter.application.data.Project;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimport.report.CCIReportSummary;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

/**
 * TODO: Make this an intergration test that is only run during 'install' phase
 * Tests the import step against a pre-configured pair of CC/Protex servers This
 * is an end-to-end test using a config file
 * 
 * This tests single protex server mode only
 * 
 * @author akamen
 * 
 */
public class BasicImportIT
{

    private static Logger log = LoggerFactory.getLogger(BasicImportIT.class
	    .getName());

    public static String testPropsForBasicImport = "importer_test.properties";
    public static String testPropsForIgnoreAssociations = "importer_test_ignore_assoc.properties";
    public static String testPropsForAllProjects = "importer_test_all_projects.properties";
    
    
    public static String fullLocationBasicImport = ClassLoader.getSystemResource(
	    testPropsForBasicImport).getFile();

    public static String fullLocationIgnoreAssoc = ClassLoader.getSystemResource(
	    testPropsForIgnoreAssociations).getFile();
    
    public static String fullLocationtestPropsForAllProjects = ClassLoader.getSystemResource(
	    testPropsForAllProjects).getFile();
    
    private static CodeCenterConfigManager ccConfig = null;
    private static CodeCenterServerWrapper ccsw = null;
    private static ProtexConfigManager pConfig = null;
    private static ProtexServerWrapper psw = null;

    private static CCISingleServerProcessor processor = null;

    /** The exception. */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    static public void setUpBeforeClass() throws Exception
    {
	try
	{

	    ccConfig = new CodeCenterConfigManager(fullLocationBasicImport);
	    pConfig = new ProtexConfigManager(fullLocationBasicImport);

	    // Create cc wrapper so that we can peform cleanup tasks
	    ccsw = new CodeCenterServerWrapper(ccConfig.getServerBean(),
		    ccConfig);
	    
	    psw = new ProtexServerWrapper(pConfig.getServerBean(), pConfig, false);
	    processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw);
	} catch (Exception e)
	{
	    Assert.fail(e.getMessage());
	}

    }

    // TODO: Does this test need to run first to succeed?? I think that might explain some of the behavior I've observed.
    @Test
    public void testBasicImport()
    {
	// This is the config case
	String[] args = new String[]{ fullLocationBasicImport };
	
	try
	{
	    List<CCIProject> projects = ccConfig.getProjectList();
	    // Before running the import, make sure to clean up.
	    cleanupProjectsBeforeImport(projects);

	    // Run the sync
	    processor.performSynchronize();

	    // Now check to see if the application(s) actually exists.
	    boolean exists = doesCcApplicationExist(projects);
	    Assert.assertEquals(true, exists);

	} catch (CodeCenterImportException e)
	{
	    Assert.fail(e.getMessage());
	}
    }

    /**
     * Attempts to perform a validation against an already associated project
     * Using the ignore association flag
     */
    @Test
    public void testIgnoreAssociation()
    {
	try
	{
	    ccConfig = new CodeCenterConfigManager(fullLocationIgnoreAssoc);
	    // Create cc wrapper so that we can peform cleanup tasks
	    ccsw = new CodeCenterServerWrapper(ccConfig.getServerBean(),
		    ccConfig);

	    processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw);
	} catch (Exception e)
	{
	    Assert.fail(e.getMessage());
	}

	try
	{
	    // Run the sync
	    processor.performSynchronize();

	    List<CCIReportSummary> summaries = processor.getReportSummaryList();
	    
	    CCIReportSummary singleProcessor = summaries.get(0);
	    
	    // Check the report summary to make sure validation was a success
	    Assert.assertEquals(0, singleProcessor.getFailedValidationList().size());

	} catch (CodeCenterImportException e)
	{
	    Assert.fail(e.getMessage());
	}
    }
    
    /**
     * Tests all the projects that belong to our user
     */
    @Test 
    public void testAllProjectsForUser()
    {
	try
	{
	    ccConfig = new CodeCenterConfigManager(fullLocationtestPropsForAllProjects);
	    // Create cc wrapper so that we can peform cleanup tasks
	    ccsw = new CodeCenterServerWrapper(ccConfig.getServerBean(),
		    ccConfig);
	    
	    processor = new CCISingleServerProcessor(ccConfig, pConfig, ccsw);
	} catch (Exception e)
	{
	    Assert.fail(e.getMessage());
	}

	try
	{
	    // Run the sync
	    processor.performSynchronize();

	    List<CCIReportSummary> summaries = processor.getReportSummaryList();
	    
	    assertEquals(1, summaries.size());
	    
	    CCIReportSummary summary = summaries.get(0);
	    	
	    // Check the report summary to make sure validation was a success
		assertEquals(0, summary.getFailedValidationList().size());
		    
		System.out.println("\nSummary: " + summary);

	} catch (CodeCenterImportException e)
	{
	    Assert.fail(e.getMessage());
	}
    }
    
    /**
     * CHecks to see if that list of applications exist and have associations.
     * @param projects
     */
    private boolean doesCcApplicationExist(List<CCIProject> projects)
    {
	boolean exists = false;
	try
	{
	    for (CCIProject project : projects)
	    {
		ApplicationPageFilter apf = new ApplicationPageFilter();
		    apf.setFirstRowIndex(0);
		    apf.setLastRowIndex(1);
		    
		    List<Application> applications = 
			    ccsw.getInternalApiWrapper().getApplicationApi().searchApplications(project.getProjectName(), apf);
		    
		    for(Application app : applications)
		    {
			// No errors here guarantees existence
			ApplicationIdToken token = app.getId();
			Project associatedProject = ccsw.getInternalApiWrapper().getApplicationApi().getAssociatedProtexProject(token);
			
			String associatedProjectName = associatedProject.getName();
			log.info("Found association of application {} to Protex project {}", app.getName()+":"+app.getVersion(), associatedProjectName);
			
		    }
	    }

	    exists = true;
	} catch (Exception e)
	{
	    log.error("Error during application verification: " + e.getMessage());
	}

	return exists;
    }

    /**
     * Deletes the specific applications that we are testing with.
     * @param projects
     */
    private void cleanupProjectsBeforeImport(List<CCIProject> projects)
    {
	log.info("Cleaning up projects before testing import");

	try
	{
	 
	    for (CCIProject project : projects)
	    {
		ApplicationNameVersionToken token = new ApplicationNameVersionToken();
		token.setName(project.getProjectName());
		token.setVersion(project.getProjectVersion());
		Application appToDelete = ccsw.getInternalApiWrapper().getApplicationApi().getApplication(token);

		if (appToDelete == null)
		{
		    log.info("Nothing to cleanup!");
		    return;
		} else
		{
		    // Delete it
		    ccsw.getInternalApiWrapper().getApplicationApi()
			    .deleteApplication(appToDelete.getId());
		    log.info("Deleted application [{}] as part of cleanup",
			    project);
		}
	    }

	} catch (Exception e)
	{
	    log.warn("Failure during cleanup!: " + e.getMessage());
	}

    }

}
