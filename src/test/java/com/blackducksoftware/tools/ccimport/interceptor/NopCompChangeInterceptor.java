package com.blackducksoftware.tools.ccimport.interceptor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

/**
 * This component change interceptor does nothing.
 * Useful for testing.
 *
 * @author sbillings
 *
 */
public class NopCompChangeInterceptor implements CompChangeInterceptor {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    @Override
    public void init(CCIConfigurationManager config, ICodeCenterServerWrapper ccsw, IProtexServerWrapper<ProtexProjectPojo> psw) throws InterceptorException {
        log.info("init()");
    }

    @Override
    public void initForApp(String appId) throws InterceptorException {
        log.info("initForApp(): appId: " + appId);
    }

    @Override
    public boolean preProcessAdd(String compId) throws InterceptorException {
        log.info("preProcessAdd(): compId: " + compId);
        return true;
    }

    @Override
    public void postProcessAdd(String requestId, String compId) throws InterceptorException {
        log.info("postProcessAdd(): requestId: " + requestId + ", comId: " + compId);
    }

    @Override
    public boolean preProcessDelete(String requestId, String compId) throws InterceptorException {
        log.info("preProcessDelete(): requestId: " + requestId + ", comId: " + compId);
        return true;
    }

}
