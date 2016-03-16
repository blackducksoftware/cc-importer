package com.blackducksoftware.tools.ccimport.mocks;

import java.util.ArrayList;
import java.util.List;

import com.blackducksoftware.tools.ccimport.interceptor.CompChangeInterceptor;
import com.blackducksoftware.tools.ccimport.interceptor.InterceptorException;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public class MockCompChangeInterceptor implements CompChangeInterceptor {
    private int callCountInit = 0;

    private List<String> callsInitForApp = new ArrayList<>();

    private List<String> callsPreProcessAdd = new ArrayList<>();

    private List<String> callsPostProcessAdd = new ArrayList<>();

    private List<String> callsPreProcessDelete = new ArrayList<>();

    private String appId = "";

    @Override
    public void init(CCIConfigurationManager config, ICodeCenterServerWrapper ccsw, IProtexServerWrapper<ProtexProjectPojo> psw) throws InterceptorException {
        callCountInit++;
    }

    @Override
    public void initForApp(String appId) throws InterceptorException {
        callsInitForApp.add(appId);
        this.appId = appId;
    }

    @Override
    public boolean preProcessAdd(String compId) throws InterceptorException {
        callsPreProcessAdd.add(appId + "|" + compId);
        return true;
    }

    @Override
    public void postProcessAdd(String requestId, String compId) throws InterceptorException {
        callsPostProcessAdd.add(appId + "|" + requestId + "|" + compId);
    }

    @Override
    public boolean preProcessDelete(String requestId, String compId) throws InterceptorException {
        callsPreProcessDelete.add(appId + "|" + requestId + "|" + compId);
        return true;
    }

    public int getCallCountInit() {
        return callCountInit;
    }

    public List<String> getCallsInitForApp() {
        return callsInitForApp;
    }

    public List<String> getCallsPreProcessAdd() {
        return callsPreProcessAdd;
    }

    public List<String> getCallsPostProcessAdd() {
        return callsPostProcessAdd;
    }

    public List<String> getCallsPreProcessDelete() {
        return callsPreProcessDelete;
    }
}
