/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

/**
 * 
 */
package com.blackducksoftware.soleng.ccimporter.model;

import java.util.Date;

import com.blackducksoftware.sdk.codecenter.application.data.Application;

import soleng.framework.standard.protex.ProtexProjectPojo;

/**
 *  Project Pojo that contains both the Protex project name and the CC version
 *  During import it acquires a CC Application Object
 *  @author Ari Kamen
 *  @date Jun 30, 2014
 *
 */
public class CCIProject extends ProtexProjectPojo
{
    private String projectVersion = null;
    private Application application = null;
    private Date lastBOMRefreshDate = null;
    private Date analyzedDateValue = null;
    
    public CCIProject()
    {
	super();
    }

    public String getProjectVersion()
    {
	return projectVersion;
    }


    public void setProjectVersion(String projectVersion)
    {
	this.projectVersion = projectVersion;
    }
   
    
    public String toString()
    {
	StringBuilder sb = new StringBuilder();
	
	sb.append("Protex Name: " + this.getProjectName());
	sb.append("\n");
	sb.append("Version: " + this.getProjectVersion());


	
	return sb.toString();
    }

    public Application getApplication()
    {
	return application;
    }

    public void setApplication(Application application)
    {
	this.application = application;
    }

    public Date getLastBOMRefreshDate()
    {
	return lastBOMRefreshDate;
    }

    public void setLastBOMRefreshDate(Date lastBOMRefreshDate)
    {
	this.lastBOMRefreshDate = lastBOMRefreshDate;
    }

	public Date getAnalyzedDateValue() {
		return analyzedDateValue;
	}

	public void setAnalyzedDateValue(Date analyzedDateValue) {
		this.analyzedDateValue = analyzedDateValue;
	}
    
}
