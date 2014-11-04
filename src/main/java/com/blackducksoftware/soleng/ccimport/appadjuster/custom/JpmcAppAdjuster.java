/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.soleng.ccimport.appadjuster.AppAdjuster;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class JpmcAppAdjuster implements AppAdjuster {

	@Override
	public void adjustApp(CCIConfigurationManager config, Application app,
			CCIProject project) throws CodeCenterImportException {
		System.out.println("*** JpmcAppAdjuster called for app: " + app.getName());

	}

}
