package com.blackducksoftware.tools.ccimport.mocks;

import java.util.ArrayList;
import java.util.List;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;
import com.blackducksoftware.tools.connector.protex.common.ProtexComponentPojo;
import com.blackducksoftware.tools.connector.protex.component.IProtexComponentManager;
import com.blackducksoftware.tools.connector.protex.project.IProjectManager;

public class MockProjectManager implements IProjectManager {
    private final IProtexComponentManager compMgr;

    private final ProjectPojo project;

    public MockProjectManager(IProtexComponentManager compMgr) {
        this.compMgr = compMgr;
        project = new ProtexProjectPojo("c_Customerproject1_9246",
                "Customer Project1");

    }

    @Override
    public ProjectPojo getProjectByName(String projectName)
            throws CommonFrameworkException {
        return project;
    }

    @Override
    public ProjectPojo getProjectById(String projectID)
            throws CommonFrameworkException {
        return project;
    }

    @Override
    public <T extends ProtexComponentPojo> List<T> getComponentsByProjectId(
            Class<T> pojoClass, String projectId)
            throws CommonFrameworkException {

        List<ComponentNameVersionIds> nameVersionIdsList = new ArrayList<>();
        ComponentNameVersionIds nameVersionIds = new ComponentNameVersionIds(
                "jqueryjs527713", "3038508");
        nameVersionIdsList.add(nameVersionIds);

        return compMgr.getComponentsByNameVersionIds(pojoClass,
                nameVersionIdsList);
    }

}
