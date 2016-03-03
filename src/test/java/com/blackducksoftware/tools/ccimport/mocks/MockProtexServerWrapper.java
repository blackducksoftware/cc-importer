package com.blackducksoftware.tools.ccimport.mocks;

import java.util.List;

import org.apache.log4j.Logger;

import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.common.ILicenseManager;
import com.blackducksoftware.tools.connector.protex.CodeTreeHelper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;
import com.blackducksoftware.tools.connector.protex.ProtexAPIWrapper;
import com.blackducksoftware.tools.connector.protex.component.IProtexComponentManager;
import com.blackducksoftware.tools.connector.protex.license.ProtexLicensePojo;
import com.blackducksoftware.tools.connector.protex.obligation.IObligationManager;
import com.blackducksoftware.tools.connector.protex.project.IProjectManager;
import com.blackducksoftware.tools.connector.protex.report.IReportManager;

public class MockProtexServerWrapper implements
        IProtexServerWrapper<ProtexProjectPojo> {
    private final Logger log = Logger.getLogger(this.getClass());

    private final ILicenseManager<ProtexLicensePojo> licMgr = new MockProtexLicenseManager();

    private final IReportManager reportMgr = new MockReportManager();

    private final IProtexComponentManager compMgr = new MockProtexComponentManager(
            licMgr);

    private final IProjectManager projectMgr = new MockProjectManager(compMgr);

    private final IObligationManager obligationMgr = new MockObligationManager();

    @Override
    public CodeTreeHelper getCodeTreeHelper() {
        throw new UnsupportedOperationException(
                "This method is not implemented on this mock object");
    }

    @Override
    public ProjectPojo getProjectByName(String projectName)
            throws CommonFrameworkException {
        return projectMgr.getProjectByName(projectName);
    }

    @Override
    public ProjectPojo getProjectByID(String projectID)
            throws CommonFrameworkException {
        return projectMgr.getProjectById(projectID);
    }

    @Override
    public String getProjectURL(ProjectPojo pojo) {
        throw new UnsupportedOperationException(
                "This method is not implemented on this mock object");
    }

    @Override
    public <T> List<T> getProjects(Class<T> theProjectClass) throws Exception {
        throw new UnsupportedOperationException(
                "This method is not implemented on this mock object");
    }

    @Override
    public String createProject(String projectName, String description)
            throws Exception {
        throw new UnsupportedOperationException(
                "This method is not implemented on this mock object");
    }

    @Override
    public ProtexAPIWrapper getInternalApiWrapper() {
        throw new UnsupportedOperationException(
                "This method is not implemented on this mock object");
    }

    @Override
    public ConfigurationManager getConfigManager() {
        throw new UnsupportedOperationException(
                "This method is not implemented on this mock object");
    }

    @Override
    public ILicenseManager<ProtexLicensePojo> getLicenseManager() {
        return licMgr;
    }

    @Override
    public IReportManager getReportManager() {
        return reportMgr;
    }

    @Override
    public IProjectManager getProjectManager() {
        return projectMgr;
    }

    @Override
    public IProtexComponentManager getComponentManager() {
        return compMgr;
    }

    @Override
    public IObligationManager getObligationManager() {
        // TODO Auto-generated function stub
        return null;
    }

}
