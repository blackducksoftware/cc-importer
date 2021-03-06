/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
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

package com.blackducksoftware.tools.ccimporter.model;

import com.blackducksoftware.sdk.codecenter.application.data.Application;

/**
 * Application POJO.
 *
 * @author sbillings
 *
 */
public class CCIApplication {
    private final Application app;
    private boolean justCreated; // true if this app was created during this run
				 // of the utility

    public CCIApplication(Application app, boolean justCreated) {
	this.app = app;
	this.justCreated = justCreated;
    }

    /**
     * Returns the underlying Code Center SDK Application object.
     * 
     * @return
     */
    public Application getApp() {
	return app;
    }

    /**
     * Gets a boolean indicating whether or not the application was created
     * during this run of cc-importer.
     * 
     * @return
     */
    public boolean isJustCreated() {
	return justCreated;
    }

    /**
     * Sets a boolean indicating whether or not the application was created
     * during this run of cc-importer.
     * 
     * @param justCreated
     */
    public void setJustCreated(boolean justCreated) {
	this.justCreated = justCreated;
    }

}
