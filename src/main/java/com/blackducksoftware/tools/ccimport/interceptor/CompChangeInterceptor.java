package com.blackducksoftware.tools.ccimport.interceptor;

import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public interface CompChangeInterceptor {
    void init(ICodeCenterServerWrapper ccsw, IProtexServerWrapper psw) throws InterceptorException;

    void initForApp(String appId) throws InterceptorException;

    boolean preProcessAdd(String compId) throws InterceptorException;

    void postProcessAdd(String requestId) throws InterceptorException;

    boolean preProcessDelete(String requestId) throws InterceptorException;
}
