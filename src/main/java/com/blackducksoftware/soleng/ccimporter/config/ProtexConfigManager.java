/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.soleng.ccimporter.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  The Protex configuration manager
 * 
 *  @author Ari Kamen
 *  @date Jun 27, 2014
 *
 */
public class ProtexConfigManager extends CCIConfigurationManager 
{
    private final static Logger log = LoggerFactory
	    .getLogger(ProtexConfigManager.class.getName());

    public ProtexConfigManager(String[] args)
    {
	super();
	initCommandLine(args, APPLICATION.PROTEX);
    }
    
    public ProtexConfigManager(String fileLocation)
    {
	super(fileLocation, APPLICATION.PROTEX);
	log.info("Importing user configuration from file for Protex.");
	initConfigFile();
    }
}
