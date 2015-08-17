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

package com.blackducksoftware.tools.ccimport;

import java.lang.reflect.Method;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

public class ProjectProcessorThreadWorker implements Runnable {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private CodeCenterServerWrapper codeCenterServerWrapper;
    private ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper;
    private List<CCIProject> partialProjectList;
    private List<CCIReportSummary> reportSummaryList;
    private CodeCenterConfigManager codeCenterConfigManager;
    private Object appAdjusterObject;
    private Method appAdjusterMethod;

    public ProjectProcessorThreadWorker(
	    CodeCenterServerWrapper codeCenterWrapper,
	    ProtexServerWrapper<ProtexProjectPojo> protexWrapper,
	    CodeCenterConfigManager codeCenterConfigManager,
	    List<CCIProject> partialProjectList,
	    List<CCIReportSummary> reportSummaryList, Object appAdjusterObject,
	    Method appAdjusterMethod) {
	codeCenterServerWrapper = codeCenterWrapper;
	protexServerWrapper = protexWrapper;
	this.codeCenterConfigManager = codeCenterConfigManager;
	this.partialProjectList = partialProjectList;
	this.reportSummaryList = reportSummaryList;
	this.appAdjusterObject = appAdjusterObject;
	this.appAdjusterMethod = appAdjusterMethod;
    }

    @Override
    public void run() {
	log.debug("run() called");
	try {
	    CodeCenterProjectSynchronizer synchronizer = new CodeCenterProjectSynchronizer(
		    codeCenterServerWrapper, protexServerWrapper,
		    codeCenterConfigManager, appAdjusterObject,
		    appAdjusterMethod);
	    synchronizer.synchronize(partialProjectList);
	    synchronized (reportSummaryList) {
		if (reportSummaryList.size() == 0) {
		    reportSummaryList.add(synchronizer.getReportSummary());
		} else {
		    reportSummaryList.get(0).addReportSummary(
			    synchronizer.getReportSummary());
		}
	    }

	} catch (Exception e) {
	    log.error(e.getMessage());
	    throw new UnsupportedOperationException(e.getMessage());
	}
    }

}
