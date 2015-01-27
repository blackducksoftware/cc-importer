package com.blackducksoftware.soleng.ccimport.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.codecenter.pojo.ComponentPojo;
import soleng.framework.standard.codecenter.pojo.ComponentPojoImpl;
import soleng.framework.standard.common.ProjectPojo;
import soleng.framework.standard.protex.ProtexAPIWrapper;
import soleng.framework.standard.protex.ProtexServerWrapper;

import com.blackducksoftware.sdk.fault.SdkFault;
import com.blackducksoftware.sdk.protex.common.Component;
import com.blackducksoftware.sdk.protex.common.ComponentType;
import com.blackducksoftware.sdk.protex.common.UsageLevel;
import com.blackducksoftware.sdk.protex.component.custom.CustomComponent;
import com.blackducksoftware.sdk.protex.component.standard.StandardComponent;
import com.blackducksoftware.sdk.protex.component.version.ComponentVersion;
import com.blackducksoftware.sdk.protex.project.bom.BomComponent;
import com.blackducksoftware.sdk.protex.project.bom.BomComponentType;
import com.blackducksoftware.sdk.protex.project.localcomponent.LocalComponent;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class ProtexComponentCollector extends ComponentCollector {
	private static Logger log = LoggerFactory
		    .getLogger(ProtexComponentCollector.class.getName());
	private String protexProjectId;

	public ProtexComponentCollector(ProtexServerWrapper protexWrapper, String protexProjectId) throws Exception {
		this.protexProjectId = protexProjectId;
		loadProjectComponents(protexWrapper, protexProjectId);
	}
	
	/**
	 * Gets protex elements
	 * @return
	 * @throws Exception
	 */
	private void loadProjectComponents(ProtexServerWrapper protexWrapper, String protexProjectId) throws Exception
	{
		
		ProjectPojo protexProject = protexWrapper.getProjectByID(protexProjectId);
		if (protexProject == null)
			throw new Exception("Unable to find project with ID: " + protexProjectId);

		String projectIdFromProtex = protexProject.getProjectKey();
		
		List<BomComponent> bomComps = protexWrapper.getInternalApiWrapper().bomApi.getBomComponents(protexProjectId);
		compPojoList = new TreeSet<ComponentPojo>();
		for(BomComponent bomcomponent : bomComps)
		{

			Component component = protexWrapper.getInternalApiWrapper().projectApi.getComponentById(protexProjectId, 
					bomcomponent.getComponentId());
		
			log.debug("Comp " + component.getName() + ": Comp Type: " + component.getType());
			log.debug("Comp " + component.getName() + ": BomComp Type: " + bomcomponent.getType());
			log.debug("Comp " + component.getName() + ": BomComp VersionID: " + bomcomponent.getVersionId());
			
			log.debug("\tBomComp approval state: " + bomcomponent.getApprovalInfo().getApproved().name());
			log.debug("\tBomComp file count identified: " + bomcomponent.getFileCountIdentified());
			log.debug("\tBomComp file count rapidId identified: " + bomcomponent.getFileCountRapidIdIdentifications());	
			
			if (component.getType() == ComponentType.PROJECT) {
				continue;
			}
			ComponentPojo compPojo = new ComponentPojoImpl(bomcomponent.getComponentId(),
					component.getName(),
					bomcomponent.getBomVersionName(),
					bomcomponent.getComponentId());
			compPojoList.add(compPojo);
		}
	}
}
