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

package com.blackducksoftware.tools.ccimport.appadjuster;

import java.util.TimeZone;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.model.CCIApplication;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

/**
 * AppAdjusters make changes to each app after it is imported. Must be
 * thread-safe.
 *
 * @author sbillings
 *
 */
public interface AppAdjuster {
    /**
     * Initialize the object.
     *
     * @param ccWrapper
     * @param protexWrapper
     * @param config
     * @param tz
     * @throws CodeCenterImportException
     */
    void init(CodeCenterServerWrapper ccWrapper,
	    ProtexServerWrapper<ProtexProjectPojo> protexWrapper,
	    CCIConfigurationManager config, TimeZone tz)
	    throws CodeCenterImportException;

    /**
     * Make changes to the given application. The corresponding Protex project
     * is also provided in case it is the source of some of the metadata.
     *
     * @param app
     * @param project
     * @throws CodeCenterImportException
     */
    void adjustApp(CCIApplication app, CCIProject project)
	    throws CodeCenterImportException;
}
