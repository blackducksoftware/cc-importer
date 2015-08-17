/*******************************************************************************
 * Copyright (C) 2015 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *******************************************************************************/

/**
 *
 */
package com.blackducksoftware.tools.ccimporter.model;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

/**
 * Project Pojo that contains both the Protex project name and the CC version
 * During import it acquires a CC Application Object
 *
 * @author Ari Kamen
 * @date Jun 30, 2014
 *
 */
public class CCIProject extends ProtexProjectPojo {
    private String projectVersion = null;
    private CCIApplication cciApplication = null;

    public CCIProject() {
	super();
    }

    public String getProjectVersion() {
	return projectVersion;
    }

    public void setProjectVersion(String projectVersion) {
	this.projectVersion = projectVersion;
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();

	sb.append("Protex Name: " + getProjectName());
	sb.append("\n");
	sb.append("Version: " + getProjectVersion());

	return sb.toString();
    }

    public Application getApplication() {
	return cciApplication.getApp();
    }

    /**
     * Set the CCIApplication with justCreated=false by providing an
     * Application. This is for backward compatibility for all the code that was
     * written using this class when the application was stored as an
     * Application, rather than a CCIApplication.
     *
     * @param application
     */
    public void setApplication(Application application) {
	cciApplication = new CCIApplication(application, false);
    }

    public CCIApplication getCciApplication() {
	return cciApplication;
    }

    public void setCciApplication(CCIApplication cciApplication) {
	this.cciApplication = cciApplication;
    }
}
