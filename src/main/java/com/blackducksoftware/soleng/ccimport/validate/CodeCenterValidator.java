/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.soleng.ccimport.validate;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.soleng.ccimport.CodeCenterProjectSynchronizer;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

/**
 * Class responsible for performing validations for applications
 * 
 * @author Ari Kamen
 * @date Aug 5, 2014
 * 
 */
public class CodeCenterValidator
{
    private static Logger log = LoggerFactory
	    .getLogger(CodeCenterValidator.class.getName());

    private CodeCenterServerWrapper ccWrapper = null;
    private CCIConfigurationManager configManager = null;
    private Application application = null;
    private CCIProject protexProject = null;
    
    public CodeCenterValidator(CCIConfigurationManager configManager,
	    CodeCenterServerWrapper ccWrapper, Application app, CCIProject importedProject)
    {
	this.ccWrapper = ccWrapper;
	this.configManager = configManager;
	protexProject = importedProject;
	application = app;
    }

    /**
     * TODO: This is a temporary workaround using direct DB connectivity To be
     * replaced with SDK call upon completion of:
     * https://jira.blackducksoftware.com/browse/CC-11106
     * 
     * @return
     */
    public String getLastValidatedDate()
    {
	// Our columns
	int i = 1;
	int id = i++;   
	int applicationid_col = i++;
	int serverid_col  = i++;                              
	int projectid_col  = i++;                                  
	int timestamp_col   = i++;     
	
	// Our data
	String app_id = application.getId().getId();
	
	String validatedDate = null;
	Connection connection = null;
	Statement statement = null;
	ResultSet resultSet = null;
	
	
	try
	{
	    String serverURL = configManager.getServerName();
	    // Chop off the http
	    String serverName = serverURL.split("//")[1];

	    if(serverName.endsWith("/"))
		serverName = serverName.substring(0, serverName.lastIndexOf('/'));
	    
	    String SQL_DB_url = "jdbc:postgresql://"
		    + serverName + ":55433/bds_catalog";
	    String SQL_DB_user = "blackduck";
	    String SQL_DB_password = "mallard";

	    log.info("Connecting to DB: " + SQL_DB_url);
	    
	    connection = DriverManager.getConnection(SQL_DB_url, SQL_DB_user,
		    SQL_DB_password);
	    statement = connection.createStatement(
		    ResultSet.TYPE_SCROLL_INSENSITIVE,
		    ResultSet.CONCUR_READ_ONLY);

	    // Build a query that looks through the validate timestamp history
	    // and returns us the timestamp for our application, sorted by most recent date first.
	    // Limit to '1' as we are just interested in the first date sorted by Descending.
	    String query = "select id,applicationid,serverid,projectid,timestamp from server_validation WHERE applicationid='"+ app_id +"' ORDER BY timestamp DESC limit 1";
	    log.debug("Query to determine date: " + query);
	    // Check to see if you have the Super User role, R401
	    resultSet = statement.executeQuery(query);
	    
	    while(resultSet.next())
	    {
		String applicationid = resultSet.getString(applicationid_col);
		String associatedProjectId  = resultSet.getString(projectid_col);
		validatedDate = resultSet.getString(timestamp_col);

		// Just as a safety check, make sure we got the same ID on the protex project
		String protexProjId = this.protexProject.getProjectKey();
		if(protexProjId.equals(associatedProjectId))
		{
		    log.info("Time stamp associated project id, matches to: " + protexProjId);
		    
		}
		else
		{
		    log.warn("Time stamp associated project is mismatching! Expecting '{}' but got '{}'", protexProjId, associatedProjectId);
		}
	    }
	} catch (Exception e)
	{
	    log.error("[{}] Unable to determine validation date",
		    application.getName(), e);
	}
	finally
	{
	    try
	    {
		connection.close();
	    } catch (SQLException e)
	    {
	    }
	    try
	    {
		statement.close();
	    } catch (SQLException e)
	    {
	    }
	    try
	    {
		resultSet.close();
	    } catch (SQLException e)
	    {
	    }
	}

	return validatedDate;
    }
}
