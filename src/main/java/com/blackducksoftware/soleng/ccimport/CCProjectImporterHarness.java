package com.blackducksoftware.soleng.ccimport;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.core.config.server.ServerBean;

import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;

/**
 * Main entry point for the utility.
 * 
 * @author akamen
 * 
 */
public class CCProjectImporterHarness extends CodeCenterProjectImporter
{
    private static Logger log = LoggerFactory
	    .getLogger(CCProjectImporterHarness.class.getName());

    private static CodeCenterConfigManager ccConfigManager = null;
    // Used in the case of single-server mode.
    private static ProtexConfigManager protexConfigManager = null;
    
    public static void main(String args[])
    {

	// TODO: Get the version from the POM

	log.info("Code Center Importer, version {}", CC_IMPORTER_VERSION);

	if (args.length == 0)
	{
	    log.error("Missing arguments!");
	    CodeCenterConfigManager.usage();
	    System.exit(-1);
	} else if (args.length == 1)
	{
	    log.info("Configuration file recognized: " + args[0]);
	    ccConfigManager = new CodeCenterConfigManager(args[0]);
	    protexConfigManager = new ProtexConfigManager(args[0]);
	} else
	{
	    ccConfigManager = new CodeCenterConfigManager(args);
	    protexConfigManager = new ProtexConfigManager(args);
	}

	try
	{
	    /**
	     * Here we determine whether we do single or multi-protex support.
	     * By simply checking the server list size we have our answer
	     * quickly The default number for this tool will be two. One for
	     * Protex and one for CC. Anything more than two suggests multiple
	     * servers.
	     */
	    List<ServerBean> servers = ccConfigManager.getServerList();
	    CCIProcessor processor = null;
	    if (servers.size() > 2)
	    {
		log.info("Multi-Protex mode started.");
		processor = new CCIMultiServerProcessor(ccConfigManager);
	    } else
	    {
		/**
		 * In the case of single, we want to pass along the protex config manager
		 */
		log.info("Single-Protex mode started");
		processor = new CCISingleServerProcessor(ccConfigManager, protexConfigManager);
	    }

	    // Do the importation
	    processor.performImport();

	    // Do the validation

	} catch (Exception e)
	{
	    log.error("General failure: " + e.getMessage());
	}
    }

}
