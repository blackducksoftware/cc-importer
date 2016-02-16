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
package com.blackducksoftware.tools.ccimport;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

public class SyncProjectTaskFactoryImpl implements SyncProjectTaskFactory {

    private final CCIConfigurationManager config;
    private final CodeCenterServerWrapper codeCenterWrapper;
    private final ProtexServerWrapper<ProtexProjectPojo> protexWrapper;
    private final Object appAdjusterObject;
    private final Method appAdjusterMethod;

    public SyncProjectTaskFactoryImpl(
	    CCIConfigurationManager config,
	    CodeCenterServerWrapper codeCenterWrapper,
	    ProtexServerWrapper<ProtexProjectPojo> protexWrapper,
	    Object appAdjusterObject, Method appAdjusterMethod) {
	this.config = config;
	this.codeCenterWrapper = codeCenterWrapper;
	this.protexWrapper = protexWrapper;
	this.appAdjusterObject = appAdjusterObject;
	this.appAdjusterMethod = appAdjusterMethod;
    }

    @Override
    public Callable<CCIReportSummary> createTask(CCIProject project) {
	return new SyncProjectTask(config, codeCenterWrapper,
		protexWrapper, appAdjusterObject, appAdjusterMethod, project);
    }

}
