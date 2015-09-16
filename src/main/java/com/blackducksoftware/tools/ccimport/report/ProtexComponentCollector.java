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
package com.blackducksoftware.tools.ccimport.report;

import java.util.List;
import java.util.TreeSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.protex.common.ComponentInfo;
import com.blackducksoftware.sdk.protex.common.ComponentType;
import com.blackducksoftware.sdk.protex.project.bom.BomComponent;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ComponentPojo;
import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ComponentPojoImpl;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

/**
 * A list of components for a project.
 *
 * @author sbillings
 *
 */
public class ProtexComponentCollector extends ComponentCollector {
    private final Logger log = LoggerFactory.getLogger(this.getClass()
	    .getName());

    public ProtexComponentCollector(
	    ProtexServerWrapper<ProtexProjectPojo> protexWrapper,
	    String protexProjectId) throws Exception {
	loadProjectComponents(protexWrapper, protexProjectId);
    }

    /**
     * Gets protex elements
     *
     * @return
     * @throws Exception
     */
    private void loadProjectComponents(
	    ProtexServerWrapper<ProtexProjectPojo> protexWrapper,
	    String protexProjectId) throws Exception {

	ProjectPojo protexProject = protexWrapper
		.getProjectByID(protexProjectId);
	if (protexProject == null) {
	    throw new Exception("Unable to find project with ID: "
		    + protexProjectId);
	}

	List<BomComponent> bomComps = protexWrapper.getInternalApiWrapper()
		.getBomApi().getBomComponents(protexProjectId);
	compPojoList = new TreeSet<ComponentPojo>();
	for (BomComponent bomcomponent : bomComps) {

	    ComponentInfo componentInfo = protexWrapper
		    .getInternalApiWrapper()
		    .getProjectApi()
		    .getComponentByKey(protexProjectId,
			    bomcomponent.getComponentKey());

	    log.debug("Comp " + componentInfo.getComponentName()
		    + ": Comp Type: " + componentInfo.getComponentType());
	    log.debug("Comp " + componentInfo.getComponentName()
		    + ": BomComp Type: " + bomcomponent.getType());
	    log.debug("Comp " + componentInfo.getComponentName()
		    + ": BomComp VersionName: " + bomcomponent.getVersionName());

	    // log.debug("\tBomComp approval state: " +
	    // bomcomponent.getApprovalInfo().getApproved().name());
	    // log.debug("\tBomComp file count identified: " +
	    // bomcomponent.getFileCountIdentified());
	    // log.debug("\tBomComp file count rapidId identified: " +
	    // bomcomponent.getFileCountRapidIdIdentifications());

	    if (componentInfo.getComponentType() == ComponentType.PROJECT) {
		continue;
	    }
	    ComponentPojo compPojo = new ComponentPojoImpl(bomcomponent
		    .getComponentKey().getComponentId(),
		    componentInfo.getComponentName(),
		    bomcomponent.getBomVersionName(), bomcomponent
			    .getComponentKey().getComponentId());
	    compPojoList.add(compPojo);
	}
    }
}
