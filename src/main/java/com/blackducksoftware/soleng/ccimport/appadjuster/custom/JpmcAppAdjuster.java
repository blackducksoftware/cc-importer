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
	private static final Pattern sealIdPattern = Pattern.compile("[0-9][0-9][0-9]+");
	private static final Pattern separatorPattern = Pattern.compile("-");
	private static final Pattern workStreamPattern = Pattern.compile("(PROD|RC1|RC2|RC3|RC4|RC5)");

	public void adjustApp(CCIConfigurationManager config, Application app,
			CCIProject project) throws CodeCenterImportException {
		
		JpmcAppMetadata metadata = parse(app.getName());
		
	}
	
	static JpmcAppMetadata parse(String appName) throws CodeCenterImportException {
		System.out.println("appName: " + appName);
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
