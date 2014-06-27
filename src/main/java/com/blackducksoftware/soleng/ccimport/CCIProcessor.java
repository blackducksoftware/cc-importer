/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.soleng.ccimport;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.core.config.server.ServerBean;
import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.soleng.ccimporter.config.CodeCenterConfigManager;

/**
 *  Common functionality shared between the single and multi processors.
 *  @author Ari Kamen
 *  @date Jun 27, 2014
 *
 */
public abstract class CCIProcessor 
{
	private static Logger log = LoggerFactory.getLogger(CCIProcessor.class.getName());

	protected CodeCenterConfigManager codeCenterConfigManager = null;
	private CodeCenterServerWrapper codeCenterWrapper = null;
	
	/**
	 * Establish the Code Center connection.
	 * @param configManager
	 * @throws Exception
	 */
	public CCIProcessor(CodeCenterConfigManager configManager) throws Exception
	{
		this.codeCenterConfigManager = configManager;

		try{
			ServerBean ccBean  = configManager.getServerBean();
			if(ccBean ==  null)
				throw new Exception("No valid Protex server configurations found");
			
			codeCenterWrapper = new CodeCenterServerWrapper(ccBean, configManager);
			
		} catch (Exception e)
		{
			throw new Exception("Unable to establish Code Center connection: " + e.getMessage());
		}		
	}
	
	/**
	 * Performs the importation of specified protex projects 
	 * into our Code Center instance.
	 */
	public abstract void performImport();

	
}
