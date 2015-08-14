/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.tools.ccimporter.model;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.tools.ccimport.CodeCenterProjectSynchronizer;

public class CCIApplication {
	private Application app;
	private boolean justCreated; // true if this app was created during this run of the utility
	public CCIApplication(Application app, boolean justCreated) {
		this.app = app;
		this.justCreated = justCreated;
	}
	public Application getApp() {
		return app;
	}
	public boolean isJustCreated() {
		return justCreated;
	}
	public void setJustCreated(boolean justCreated) {
		this.justCreated = justCreated;
	}
	
}
