/**
 * Copyright (C)2014 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms set forth in the Black Duck App Exchange
 * Terms of Use located at:
 * http://www.blackducksoftware.com/legal/appexchange
 * IF YOU DO NOT AGREE TO THE THESE TERMS OR ANY SPECIAL TERMS, 
 * DO NOT ACCESS OR USE THIS SITE OR THE SOFTWARE.
 * 
 * @author Niles Madison, namdison@blackducksoftware.com
 */
package com.blackducksoftware.soleng.ccimporter.config;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CC Configuration Manager class for CCI.
 * 
 * @author akamen
 * 
 */
public class CodeCenterConfigManager extends CCIConfigurationManager
{
    private final static Logger log = LoggerFactory
	    .getLogger(CodeCenterConfigManager.class.getName());

    public CodeCenterConfigManager(String[] args)
    {
	super();
	initCommandLine(args, APPLICATION.CODECENTER);
    }

    public CodeCenterConfigManager(String fileLocation)
    {
	super(fileLocation, APPLICATION.CODECENTER);
	log.info("Importing user configuration from file...");
	initConfigFile();
    }
    
    public CodeCenterConfigManager(Properties props)
    {
	super(props, APPLICATION.CODECENTER);
	log.info("Importing user configuration from file...");
	initConfigFile();
    }
}