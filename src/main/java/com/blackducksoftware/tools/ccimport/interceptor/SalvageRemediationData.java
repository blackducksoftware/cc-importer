package com.blackducksoftware.tools.ccimport.interceptor;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.deprecatedcomp.DeprecatedComponentReplacementTable;
import com.blackducksoftware.tools.ccimport.deprecatedcomp.SqlDeprecatedComponentReplacementTable;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.codecenter.application.ApplicationPojo;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public class SalvageRemediationData implements CompChangeInterceptor {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private Connection conn;

    private ICodeCenterServerWrapper ccsw = null;

    private ApplicationPojo app;

    private List<String> addRequestIds;

    private DeprecatedComponentReplacementTable deprecatedComponentReplacementTable;

    /**
     * Code Center will call this constructor
     * 
     * @throws CodeCenterImportException
     */
    public SalvageRemediationData(ConfigurationManager config) throws CodeCenterImportException {
        deprecatedComponentReplacementTable = new SqlDeprecatedComponentReplacementTable(config);
    }

    /**
     * Test code can call this constructor to inject the DeprecatedComponentReplacementTable
     */
    public SalvageRemediationData(DeprecatedComponentReplacementTable table) {
        deprecatedComponentReplacementTable = table;
    }

    @Override
    public void init(ICodeCenterServerWrapper ccsw, IProtexServerWrapper psw) throws InterceptorException {
        this.ccsw = ccsw;
    }

    @Override
    public void initForApp(String appId) throws InterceptorException {
        try {
            app = ccsw.getApplicationManager().getApplicationById(appId);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException(e.getMessage());
        }
        addRequestIds = new ArrayList<>();
    }

    @Override
    public boolean preProcessAdd(String compId) throws InterceptorException {
        return true;
    }

    @Override
    public void postProcessAdd(String requestId) throws InterceptorException {
        addRequestIds.add(requestId);
    }

    @Override
    public boolean preProcessDelete(String requestId) throws InterceptorException {
        return true;
    }

}