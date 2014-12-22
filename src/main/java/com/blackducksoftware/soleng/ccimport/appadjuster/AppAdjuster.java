/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster;

import java.util.TimeZone;

import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIApplication;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public interface AppAdjuster {
	public void init(CodeCenterServerWrapper ccWrapper, CCIConfigurationManager config, TimeZone tz);
	public void adjustApp(CCIApplication app, CCIProject project) throws CodeCenterImportException;
}
