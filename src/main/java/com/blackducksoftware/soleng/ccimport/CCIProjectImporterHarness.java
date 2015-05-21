package com.blackducksoftware.soleng.ccimport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.connector.protex.ProtexServerWrapper;
import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.soleng.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIApplication;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

/**
 * Main entry point for the utility.
 * 
 * There are two modes: Single (Protex) Server, and Multi (Protex) Server.
 * 
 * You can configure in an optional AppAdjuster, which modifies the app after the import/sync is done.
 * Currently there is one AppAdjuster available, NumericPrefixedAppAdjuster, developed for a specific customer.
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
				processor = new CCISingleServerProcessor(ccConfigManager,
						protexConfigManager, codeCenterServerWrapper);
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
	
	static Object getAppAdjusterObject(CCIConfigurationManager config) throws CodeCenterImportException {
		
		// See if the user has configured a custom app adjuster
    	String appAdjusterClassname = config.getAppAdjusterClassname();
    	if (appAdjusterClassname == null) {
    		return null;  // No custom app adjuster has been configured
    	}
    	// Get the user-configured custom app adjuster class
    	Class sourceClass = null;
    	try {
    		sourceClass = Class.forName(appAdjusterClassname);
    	} catch (ClassNotFoundException e) {
    		String msg = "Unable to convert name to class for custom app adjuster: Class not found: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	}
    	
    	// Create an instance of the custom app adjuster class
    	Object appAdjusterObject=null;
    	try {
    		appAdjusterObject = sourceClass.newInstance();
    	} catch (IllegalAccessException e) {
    		String msg = "Unable to create instance of app adjuster: Illegal access: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	} catch (InstantiationException e) {
    		String msg = "Unable to create instance of app adjuster: Instantiation exception: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	}
    	
    	return appAdjusterObject;
	}
	
	/**
     * If a custom app adjuster (to modify app metadata after sync) has been configured, initialize it.
     * @param config
     * @throws CodeCenterImportException
     */
    static Method getAppAdjusterMethod(CodeCenterServerWrapper ccWrapper, ProtexServerWrapper protexWrapper,
    		CCIConfigurationManager config, Object appAdjusterObject) throws CodeCenterImportException {

    	// See if the user has configured a custom app adjuster
    	String appAdjusterClassname = config.getAppAdjusterClassname();
    	if (appAdjusterClassname == null) {
    		return null;  // No custom app adjuster has been configured
    	}
    	// Get the user-configured custom app adjuster class
    	Class sourceClass = null;
    	try {
    		sourceClass = Class.forName(appAdjusterClassname);
    	} catch (ClassNotFoundException e) {
    		String msg = "Unable to convert name to class for custom app adjuster: Class not found: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	}
    	
    	// Get the init method on the custom app adjuster class
    	Method initMethod=null;
    	Class[] initMethodArgTypes = { CodeCenterServerWrapper.class, ProtexServerWrapper.class, CCIConfigurationManager.class, TimeZone.class };
    	try {
    		initMethod = sourceClass.getDeclaredMethod("init", initMethodArgTypes);
    	} catch (NoSuchMethodException e) {
    		String msg = "Unable to get init method: No such method exception: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	}
    	
    	// Get the adjustApp method on the custom app adjuster class
    	Class[] adjustAppMethodArgTypes = { CCIApplication.class, CCIProject.class };
    	Method appAdjusterMethod=null;
    	try {
    		appAdjusterMethod = sourceClass.getDeclaredMethod("adjustApp", adjustAppMethodArgTypes);
    	} catch (NoSuchMethodException e) {
    		String msg = "Unable to get app adjuster method: No such method exception: " + appAdjusterClassname;
    		throw new CodeCenterImportException(msg);
    	}
    	
    	TimeZone tz = TimeZone.getTimeZone(config.getTimeZone());
    	
    	// Call the init method to initialize the custom app adjuster
    	try {
    		initMethod.invoke(appAdjusterObject, ccWrapper, protexWrapper, config, tz);
    	} catch (InvocationTargetException e) {
    		String msg = "Error initializing custom app adjuster: InvocationTargetException: " + e.getTargetException().getMessage();
    		throw new CodeCenterImportException(msg);
    	} catch (IllegalAccessException e) {
    		String msg = "Error initializing custom app adjuster: IllegalAccessException: " + e.getMessage();
    		throw new CodeCenterImportException(msg);
    	}
    	
    	return appAdjusterMethod;
    }
    
	
}
