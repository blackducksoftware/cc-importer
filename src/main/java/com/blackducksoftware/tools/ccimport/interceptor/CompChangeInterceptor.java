package com.blackducksoftware.tools.ccimport.interceptor;

import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public interface CompChangeInterceptor {
    void init(ConfigurationManager config, ICodeCenterServerWrapper ccsw, IProtexServerWrapper<ProtexProjectPojo> psw) throws InterceptorException;

    void initForApp(String appId) throws InterceptorException;

    boolean preProcessAdd(String compId) throws InterceptorException;

    void postProcessAdd(String requestId, String compId) throws InterceptorException;

    boolean preProcessDelete(String requestId, String compId) throws InterceptorException;
}
