/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.tools.ccimport.appadjuster;

import java.util.TimeZone;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.model.CCIApplication;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;

/**
 * AppAdjusters make changes to each app after it is imported.
 * Must be thread-safe.
 * @author sbillings
 *
 */
public interface AppAdjuster {
	public void init(CodeCenterServerWrapper ccWrapper, ProtexServerWrapper protexWrapper, CCIConfigurationManager config, TimeZone tz) throws CodeCenterImportException;
	public void adjustApp(CCIApplication app, CCIProject project) throws CodeCenterImportException;
}
