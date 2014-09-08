/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.soleng.ccimport;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.soleng.ccimport.db.CodeCenterDBConnector;
import com.blackducksoftware.soleng.ccimport.validate.CodeCenterValidator;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;

/**
 * This class makes a DB lookup to the table server_project inside bds_catalog.
 * This provides a list of Protex project IDs and their respective Applications
 * 
 * Returns a map
 * 
 * @author Ari Kamen
 * @date Sep 8, 2014
 * 
 */
public class CodeCenterAssociationLookup extends CodeCenterDBConnector
{
    private static Logger log = LoggerFactory
	    .getLogger(CodeCenterAssociationLookup.class.getName());
    private HashMap<String, String> associationMap = new HashMap<String, String>();
    
    private final static String SQL_QUERY = "SELECT project_id,application_id FROM server_project;";
    
    public CodeCenterAssociationLookup(CCIConfigurationManager configManager,
	    CodeCenterServerWrapper ccWrapper)
    {
	super(configManager, ccWrapper);
    }

    public HashMap<String, String> getAssociationMap()
    {
	Connection connection = null;
	Statement statement = null;
	ResultSet resultSet = null;
	
	
	try
	{
	    connection = super.establishConnection();
	    statement = connection.createStatement(
		    ResultSet.TYPE_SCROLL_INSENSITIVE,
		    ResultSet.CONCUR_READ_ONLY);
	    
	    resultSet = statement.executeQuery(SQL_QUERY);
	    
	    while(resultSet.next())
	    {
		String projectId = resultSet.getString(1);
		String applicationId  = resultSet.getString(2);
	
		if(projectId != null && applicationId != null)
		    associationMap.put(projectId, applicationId);
		
		log.debug("Adding projectID [{}] and application ID [{}] to map", projectId, applicationId);
	    }
	    
	    log.info("Populated association map with [{}] entries", associationMap.size());
	    
	} catch (Exception e)
	{
	    log.error("Unable to determine association map", e);
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
	
	return associationMap;
    }
    
}
