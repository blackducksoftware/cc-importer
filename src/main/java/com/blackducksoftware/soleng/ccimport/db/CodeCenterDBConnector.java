/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.soleng.ccimport.db;

import java.sql.Connection;
import java.sql.DriverManager;

import org.apache.http.client.utils.URIBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;

/**
 *  Helper class that connects to the Database
 * 
 *  @author Ari Kamen
 *  @date Sep 8, 2014
 *
 */
public class CodeCenterDBConnector
{
    private static Logger log = LoggerFactory.getLogger(CodeCenterDBConnector.class.getName());

    protected CCIConfigurationManager configManager;
    protected CodeCenterServerWrapper ccWrapper;
    
    public CodeCenterDBConnector(CCIConfigurationManager configManager,
	    CodeCenterServerWrapper ccWrapper)
    {
	this.ccWrapper = ccWrapper;
	this.configManager = configManager;

    }
    
    /**
     * Establishes a connection, and returns it for processing. 
     * @return
     * @throws CodeCenterImportException 
     */
    protected Connection establishConnection() throws CodeCenterImportException
    {
	Connection connection = null;
	try
	{
	    // Get the full db connection string from the user, if it is blank, then default
	    String SQL_DB_url = configManager.getDbString();    
	    if(SQL_DB_url == null)
        	    {
        	    String hostNameForDB = "";
        	    // Grab the user provided hostname, or if none provided attempt to
        	    // determine ourselves
        	    String hostName = configManager.getHostName();
        	    if (hostName != null)
        	    {
        		hostNameForDB = hostName;
        	    } else
        	    {
        		String serverURL = configManager.getServerBean().getServerName();
        		try
        		{
        		    URIBuilder builder = new URIBuilder(serverURL);
        		    hostNameForDB = builder.getHost();
        
        		} catch (Exception e)
        		{
        		    log.warn("Tried to parse provided server URL '{}', but ran into a problem: {}", serverURL, e.getMessage());
        		    throw new Exception("Failure during parsing, pass in host directly using 'validate.host.name' flag", e);
        		}
        	    }
        	    
        	    // Default
        	    SQL_DB_url = "jdbc:postgresql://"
        		    + hostNameForDB + ":55433/bds_catalog";
            }
	  
	    String SQL_DB_user = "blackduck";
	    String SQL_DB_password = "mallard";

	    log.info("Connecting to DB: " + SQL_DB_url);
	    
	    connection = DriverManager.getConnection(SQL_DB_url, SQL_DB_user,
		    SQL_DB_password);
	} catch (Exception e)
	{
	    throw new CodeCenterImportException("Unable to establish raw DB connection", e);
	}
	
	return connection;
    }
	
	
}
