package com.blackducksoftware.soleng.ccimport;

import java.util.List;

import junit.framework.Assert;

import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationIdToken;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationPageFilter;
import com.blackducksoftware.sdk.codecenter.application.data.Project;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

import soleng.framework.core.config.ConfigConstants.APPLICATION;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;
import soleng.framework.standard.protex.ProtexServerWrapper;

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
public class BasicImportTest
{

    private static Logger log = LoggerFactory.getLogger(BasicImportTest.class
	    .getName());

    public static String testPropsForBasicImport = "importer_test.properties";
    public static String fullLocation = ClassLoader.getSystemResource(
	    testPropsForBasicImport).getFile();

    private static CodeCenterConfigManager ccConfig = null;
    private static CodeCenterServerWrapper ccsw = null;
    private static ProtexConfigManager pConfig = null;

    private static CCISingleServerProcessor processor = null;

    /** The exception. */
    @Rule
    public ExpectedException exception = ExpectedException.none();

    @BeforeClass
    static public void setUpBeforeClass() throws Exception
    {
	try
	{

	    ccConfig = new CodeCenterConfigManager(fullLocation);
	    pConfig = new ProtexConfigManager(fullLocation);

	    // Create cc wrapper so that we can peform cleanup tasks
	    ccsw = new CodeCenterServerWrapper(ccConfig.getServerBean(),
		    ccConfig);

	    processor = new CCISingleServerProcessor(ccConfig, pConfig);
	} catch (Exception e)
	{
	    Assert.fail(e.getMessage());
	}

    }

    @Test
    public void testBasicImport()
    {
	// This is the config case
	String[] args = new String[]{ fullLocation };
	
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
			    ccsw.getInternalApiWrapper().applicationApi.searchApplications(project.getProjectName(), apf);
		    
		    for(Application app : applications)
		    {
			// No errors here guarantees existence
			ApplicationIdToken token = app.getId();
			Project associatedProject = ccsw.getInternalApiWrapper().applicationApi.getAssociatedProtexProject(token);
			
			String associatedProjectName = associatedProject.getName();
			log.info("Found association of application {} to Protex project {}", app.getName()+":"+app.getVersion(), associatedProjectName);
			
		    }
	    }

	    exists = true;
	} catch (Exception e)
	{
	    log.error("Error during application exist verification", e);
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
	 
	    ApplicationPageFilter apf = new ApplicationPageFilter();
	    apf.setFirstRowIndex(0);
	    apf.setLastRowIndex(1);
	    for (CCIProject project : projects)
	    {
		List<Application> applications = ccsw.getInternalApiWrapper().applicationApi
			.searchApplications(project.getProjectName(), apf);

		if (applications.size() == 0)
		{
		    log.info("Nothing to cleanup!");
		    return;
		} else
		{

		    Application appToDelete = applications.get(0);
		    // Delete it
		    ccsw.getInternalApiWrapper().applicationApi
			    .deleteApplication(appToDelete.getId());
		    log.info("Deleted application [{}] as part of cleanup",
			    project);
		}
	    }

	} catch (Exception e)
	{
	    log.error("Failure during cleanup!", e);
	    Assert.fail();
	}

    }

}
