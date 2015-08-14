/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.tools.ccimport.appadjuster.custom;

public class NumericPrefixedAppMetadata {
	private String numericPrefix;
	private String appNameString="";
	private String workStream;
	
	public String getNumericPrefix() {
		return numericPrefix;
	}
	public void setNumericPrefix(String numericPrefix) {
		this.numericPrefix = numericPrefix;
	}
	public String getAppNameString() {
		return appNameString;
	}
	public void setAppNameString(String appNameString) {
		this.appNameString = appNameString;
	}
	public String getWorkStream() {
		return workStream;
	}
	public void setWorkStream(String workStream) {
		this.workStream = workStream;
	}
	
	
}
