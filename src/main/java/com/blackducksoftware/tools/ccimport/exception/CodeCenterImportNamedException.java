package com.blackducksoftware.tools.ccimport.exception;

public class CodeCenterImportNamedException extends CodeCenterImportException {
    private final String appName;

    public CodeCenterImportNamedException(String appName, String msg) {
	super(msg);
	this.appName = appName;
    }

    public CodeCenterImportNamedException(String appName, Throwable cause) {
	super(cause);
	this.appName = appName;
    }

    public CodeCenterImportNamedException(String appName, String msg,
	    Throwable cause) {
	super(msg, cause);
	this.appName = appName;
    }

    public String getAppName() {
	return appName;
    }
}
