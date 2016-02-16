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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.tools.ccimport;

import java.lang.reflect.Method;
import java.util.List;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

/**
 * A factory that creates ProjectProcessorThreadWorker objects. One
 * ProjectProcessorThreadWorker object will be needed per thread.
 *
 * @author sbillings
 *
 */
public class ProjectProcessorThreadWorkerFactoryImpl implements
        ProjectProcessorThreadWorkerFactory {
    private final CodeCenterServerWrapper codeCenterWrapper;

    private final IProtexServerWrapper<ProtexProjectPojo> protexServerWrapper;

    private final CodeCenterConfigManager codeCenterConfigManager;

    private final Object appAdjusterObject;

    private final Method appAdjusterMethod;

    public ProjectProcessorThreadWorkerFactoryImpl(
            CodeCenterServerWrapper codeCenterWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexServerWrapper,
            CodeCenterConfigManager codeCenterConfigManager,
            Object appAdjusterObject, Method appAdjusterMethod) {
        this.codeCenterConfigManager = codeCenterConfigManager;
        this.protexServerWrapper = protexServerWrapper;
        this.codeCenterWrapper = codeCenterWrapper;
        this.appAdjusterObject = appAdjusterObject;
        this.appAdjusterMethod = appAdjusterMethod;
    }

    /*
     * (non-Javadoc)
     *
     * @see
     * com.blackducksoftware.tools.ccimport.ProjectProcessorThreadWorkerFactory
     * #createProjectProcessorThreadWorker(java.util.List, java.util.List)
     */
    @Override
    public Runnable createProjectProcessorThreadWorker(
            List<CCIProject> partialProjectList,
            List<CCIReportSummary> synchronizedThreadsReportSummaryList) {
        Runnable threadWorker = new ProjectProcessorThreadWorker(
                codeCenterWrapper, protexServerWrapper,
                codeCenterConfigManager, partialProjectList,
                synchronizedThreadsReportSummaryList, appAdjusterObject,
                appAdjusterMethod);
        return threadWorker;
    }
}
