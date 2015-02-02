package com.blackducksoftware.soleng.ccimport;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.soleng.ccimporter.config.CCIConstants;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;

/**
 * Main entry point for the utility.
 * 
 * @author akamen
 * 
 */
public class CCIProjectImporterHarness {
	private static Logger log = LoggerFactory
			.getLogger(CCIProjectImporterHarness.class.getName());

	private static CodeCenterConfigManager ccConfigManager = null;
	// Used in the case of single-server mode.
	private static ProtexConfigManager protexConfigManager = null;

	public static void main(String args[]) {
		if (args.length == 0) {
			log.error("Missing arguments!");
			CodeCenterConfigManager.usage();
			System.exit(-1);
		} else if (args.length == 1) {
			log.info("Configuration file recognized: " + args[0]);
			ccConfigManager = new CodeCenterConfigManager(args[0]);
			protexConfigManager = new ProtexConfigManager(args[0]);
		} else if ((args.length == 3) && (!args[0].startsWith("-")) && (!args[2].startsWith("-")) &&
				("--project".equals(args[1]))) {
			// Special case: <config file> --project <comma-separated project list, or project;version list>
			log.info("Configuration file followed by project list recognized: " + args[0]);
			ccConfigManager = new CodeCenterConfigManager(args[0]);
			protexConfigManager = new ProtexConfigManager(args[0]);
			String projectListString = args[2];
			ccConfigManager.setProjectList(projectListString);
			protexConfigManager.setProjectList(projectListString);
		} else {
			ccConfigManager = new CodeCenterConfigManager(args);
			protexConfigManager = new ProtexConfigManager(args);
		}

		log.info("Running Code Center Importer version: "
				+ ccConfigManager.getVersion());

		try {
			CodeCenterServerWrapper codeCenterServerWrapper = createCodeCenterServerWrapper(ccConfigManager);
			/**
			 * Here we determine whether we do single or multi-protex support.
			 * By simply checking the server list size we have our answer
			 * quickly The default number for this tool will be two. One for
			 * Protex and one for CC. Anything more than two suggests multiple
			 * servers.
			 */
			List<ServerBean> servers = ccConfigManager.getServerList();
			CCIProcessor processor = null;
			if (servers.size() > 2) {
				log.info("Multi-Protex mode started.");
				processor = new CCIMultiServerProcessor(ccConfigManager,
						protexConfigManager, codeCenterServerWrapper);
			} else {
				/**
				 * In the case of single, we want to pass along the protex
				 * config manager
				 */
				log.info("Single-Protex mode started");
				
				// Construct the factory that the processor will use to create
				// the objects (run multi-threaded) to handle each subset of the project list
				ProjectProcessorThreadWorkerFactory threadWorkerFactory = 
						new ProjectProcessorThreadWorkerFactoryImpl(codeCenterServerWrapper, ccConfigManager);
				processor = new CCISingleServerProcessor(ccConfigManager,
						protexConfigManager, codeCenterServerWrapper, threadWorkerFactory);
			}

			if (ccConfigManager.isRunReport()) {
				log.info("Generate Report mode activated");
				processor.runReport();
			} else {
				processor.performSynchronize();

			}

			log.info("All finished.");
		} catch (Exception e) {
			log.error("General failure: " + e.getMessage());
		}
	}
	
	private static CodeCenterServerWrapper createCodeCenterServerWrapper(
			CodeCenterConfigManager configManager) throws Exception {
		CodeCenterServerWrapper codeCenterWrapper;
		try {
			// Always just one code center
			ServerBean ccBean = configManager.getServerBean();
			if (ccBean == null)
				throw new Exception("No valid Code Center server configurations found");

			log.info("Using Code Center URL [{}]", ccBean.getServerName());

			codeCenterWrapper = new CodeCenterServerWrapper(ccBean,
					configManager);

		} catch (Exception e) {
			throw new Exception("Unable to establish Code Center connection: "
					+ e.getMessage());
		}
		return codeCenterWrapper;
	}
}
