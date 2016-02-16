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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.report.CCIReportGenerator;
import com.blackducksoftware.tools.ccimporter.config.CCIConstants;
import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.core.config.ConfigConstants.APPLICATION;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

/**
 * Synchronizes multiple Protex servers with one Code Center server.
 *
 * @author sbillings
 *
 */
public class CCIMultiServerProcessor extends CCIProcessor {

    private final Logger log = LoggerFactory.getLogger(this.getClass()
	    .getName());

    private final ProtexConfigManager protexConfig;

    /**
     * @param configManager
     * @param protexConfigManager
     * @param multiserver
     * @throws Exception
     */
    public CCIMultiServerProcessor(CodeCenterConfigManager configManager,
	    ProtexConfigManager protexConfigManager,
	    CodeCenterServerWrapper codeCenterServerWrapper) throws Exception {
	super(configManager, codeCenterServerWrapper);
	protexConfig = protexConfigManager;
	log.info("Using Protex URL [{}]", protexConfig.getServerBean()
		.getServerName());
    }

    /**
     * For every established Protex server bean, perform a process which
     * includes: 1) Grab all the projects 2) Create an association
     *
     * @throws CodeCenterImportException
     */
    @Override
    public void performSynchronize() throws CodeCenterImportException {
	// First thing we do, is blank out any project lists that the user has
	// specified
	List<CCIProject> userProjectList = codeCenterConfigManager
		.getProjectList();
	List<ServerBean> protexServers = codeCenterConfigManager
		.getServerListByApplication(APPLICATION.PROTEX);

	if (userProjectList.size() > 0) {
	    log.warn("Project list detected, ignoring in multi-server mode!");
	    userProjectList.clear();
	}

	// For every server bean we have, create a new protexWrapper
	// Use the supplied alias to set the:
	// - Protex Server Name / Alias
	// - Owner
	// Grab the projects and process import
	for (ServerBean protexServer : protexServers) {
	    log.info("Performing synchronization against:" + protexServer);
	    String protexAlias = protexServer.getAlias();

	    if (protexAlias.isEmpty()) {
		throw new CodeCenterImportException(
			"Protex alias cannot be empty!");
	    } else {
		log.info("Setting {} to {}",
			CCIConstants.PROTEX_SERVER_URL_PROPERTY, protexAlias);
		codeCenterConfigManager.setProtexServerName(protexAlias);
	    }

	    ProtexServerWrapper<ProtexProjectPojo> wrapper = null;
	    try {
		wrapper = new ProtexServerWrapper<>(protexServer, protexConfig,
			true);
	    } catch (Exception e) {
		throw new CodeCenterImportException(
			"Unable to establish connection against: "
				+ protexServer);
	    }
	    Object appAdjusterObject = CCIProjectImporterHarness
		    .getAppAdjusterObject(codeCenterConfigManager);
	    Method appAdjusterMethod = CCIProjectImporterHarness
		    .getAppAdjusterMethod(super.codeCenterWrapper, wrapper,
			    codeCenterConfigManager, appAdjusterObject);
	    CodeCenterProjectSynchronizer synchronizer = new CodeCenterProjectSynchronizer(
		    codeCenterWrapper, wrapper, codeCenterConfigManager,
		    appAdjusterObject, appAdjusterMethod);
	    List<CCIProject> projectList = getAllProjects(wrapper);
	    synchronizer.synchronize(projectList);
	}
    }

    @Override
    public void runReport() throws CodeCenterImportException {
	log.error("Not implemented");
    }

    /**
     * This will get all the projects for that particular wrapper The underlying
     * call will always collect all, if the project is empty.
     *
     * @param protexWrapper
     * @return
     * @throws CodeCenterImportException
     */
    private List<CCIProject> getAllProjects(
	    ProtexServerWrapper<ProtexProjectPojo> protexWrapper)
	    throws CodeCenterImportException {
	return getProjects(protexWrapper).getList();

    }

    // Used by unit tests
    @Override
    public CCIReportGenerator getReportGen() {
	throw new UnsupportedOperationException(
		"getReportGen() not implemented for CCIMultiServerProcessor");
    }

}
