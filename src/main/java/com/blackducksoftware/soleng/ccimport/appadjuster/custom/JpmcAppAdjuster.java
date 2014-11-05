/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import java.util.Scanner;
import java.util.regex.Pattern;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.soleng.ccimport.appadjuster.AppAdjuster;
import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class JpmcAppAdjuster implements AppAdjuster {
	private static final String SEAL_ID_PATTERN_STRING_PROPERTY = "jpmc.appname.pattern.sealid";
	private static final String SEPARATOR_PATTERN_STRING_PROPERTY = "jpmc.appname.pattern.separator";
	private static final String WORK_STREAM_PATTERN_STRING_PROPERTY = "jpmc.appname.pattern.workstream";
	
	private static final String SEAL_ID_PATTERN_STRING_DEFAULT = "[0-9][0-9][0-9]+";
	private static final String SEPARATOR_PATTERN_STRING_DEFAULT = "-";
	private static final String WORK_STREAM_PATTERN_STRING_DEFAULT = "(PROD|RC1|RC2|RC3|RC4|RC5)";
	
	private static Pattern sealIdPattern=null;
	private static Pattern separatorPattern=null;
	private static Pattern workStreamPattern=null;

	public void init(CCIConfigurationManager config) {

		String sealIdPatternString = config.getOptionalProperty(SEAL_ID_PATTERN_STRING_PROPERTY);
		if (sealIdPatternString == null) {
			sealIdPatternString = SEAL_ID_PATTERN_STRING_DEFAULT;
		}
		
		String separatorPatternString = config.getOptionalProperty(SEPARATOR_PATTERN_STRING_PROPERTY);
		if (separatorPatternString == null) {
			separatorPatternString = SEPARATOR_PATTERN_STRING_DEFAULT;
		}
		
		String workStreamPatternString = config.getOptionalProperty(WORK_STREAM_PATTERN_STRING_PROPERTY);
		if (workStreamPatternString == null) {
			workStreamPatternString = WORK_STREAM_PATTERN_STRING_DEFAULT;
		}
		
		sealIdPattern = Pattern.compile(sealIdPatternString);
		separatorPattern = Pattern.compile(separatorPatternString);
		workStreamPattern = Pattern.compile(workStreamPatternString);
	}
	public void adjustApp(Application app,
			CCIProject project) throws CodeCenterImportException {

		JpmcAppMetadata metadata = parse(app.getName());
		// TODO: find/set custom attrs
	}
	
	JpmcAppMetadata parse(String appName) throws CodeCenterImportException {
		JpmcAppMetadata metadata = new JpmcAppMetadata();
		Scanner scanner = new Scanner(appName);
		scanner.useDelimiter(separatorPattern);
		try {
			
			if (!scanner.hasNext(sealIdPattern)) {
				throw new CodeCenterImportException("Error parsing SEAL ID from app name " + appName);
			}
			String sealId = scanner.next(sealIdPattern);
			metadata.setSealId(sealId);

			if (!scanner.hasNext(workStreamPattern)) {
				String appNameString = scanner.next();
				metadata.setAppNameString(appNameString);
			}
			
			String workStream = scanner.next(workStreamPattern);
			metadata.setWorkStream(workStream);
		
		} finally {
			scanner.close();
		}
		return metadata;
	}

}
