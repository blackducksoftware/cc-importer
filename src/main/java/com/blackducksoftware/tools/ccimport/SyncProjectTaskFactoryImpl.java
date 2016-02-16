package com.blackducksoftware.tools.ccimport;

import java.lang.reflect.Method;
import java.util.concurrent.Callable;

import com.blackducksoftware.tools.ccimport.report.CCIReportSummary;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

public class SyncProjectTaskFactoryImpl implements SyncProjectTaskFactory {

    private final CCIConfigurationManager config;
    private final CodeCenterServerWrapper codeCenterWrapper;
    private final ProtexServerWrapper<ProtexProjectPojo> protexWrapper;
    private final Object appAdjusterObject;
    private final Method appAdjusterMethod;

    public SyncProjectTaskFactoryImpl(
	    CCIConfigurationManager config,
	    CodeCenterServerWrapper codeCenterWrapper,
	    ProtexServerWrapper<ProtexProjectPojo> protexWrapper,
	    Object appAdjusterObject, Method appAdjusterMethod) {
	this.config = config;
	this.codeCenterWrapper = codeCenterWrapper;
	this.protexWrapper = protexWrapper;
	this.appAdjusterObject = appAdjusterObject;
	this.appAdjusterMethod = appAdjusterMethod;
    }

    @Override
    public Callable<CCIReportSummary> createTask(CCIProject project) {
	return new SyncProjectTask(config, codeCenterWrapper,
		protexWrapper, appAdjusterObject, appAdjusterMethod, project);
    }

}
