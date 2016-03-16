package com.blackducksoftware.tools.ccimport.interceptor;

import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public interface CompChangeInterceptor {

    /**
     * Initialize the interceptor.
     * ccimporter calls this once before calling any of the other methods.
     */
    void init(CCIConfigurationManager config, ICodeCenterServerWrapper ccsw, IProtexServerWrapper<ProtexProjectPojo> psw) throws InterceptorException;

    /**
     * Initialize the interceptor for this about-to-be-processed application.
     * 
     * @param appId
     * @throws InterceptorException
     */
    void initForApp(String appId) throws InterceptorException;

    /**
     * This method is called by ccimporter before each add request.
     * Called just before ccimporter adds the request to the application.
     *
     * @param compId
     * @return This method should return true. The return value is currently ignored by ccimporter. In the future this
     *         may be used to tell ccimporter whether or not to go ahead and actually add the request. (A value of true
     *         will tell ccimporter to proceed.)
     * @throws InterceptorException
     */
    boolean preProcessAdd(String compId) throws InterceptorException;

    /**
     * This method is called by ccimporter after each add request.
     *
     * @param requestId
     * @param compId
     * @throws InterceptorException
     */
    void postProcessAdd(String requestId, String compId) throws InterceptorException;

    /**
     * This method is called by ccimporter before each delete request.
     *
     * @param requestId
     * @param compId
     * @return This method should return true. The return value is currently ignored by ccimporter. In the future this
     *         may be used to tell ccimporter whether or not to go ahead and actually delete the request. (A value of
     *         true will tell ccimporter to proceed.)
     * @throws InterceptorException
     */
    boolean preProcessDelete(String requestId, String compId) throws InterceptorException;
}
