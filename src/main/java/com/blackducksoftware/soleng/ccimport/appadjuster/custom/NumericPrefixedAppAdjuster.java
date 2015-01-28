/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;
import java.util.TimeZone;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.application.data.Application;
import com.blackducksoftware.sdk.codecenter.application.data.ApplicationUpdate;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeIdToken;
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeNameToken;
import com.blackducksoftware.sdk.codecenter.client.util.CodeCenterServerProxyV6_6_0;
import com.blackducksoftware.sdk.codecenter.common.data.AttributeValue;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.soleng.ccimport.CCIMultiServerProcessor;
import com.blackducksoftware.soleng.ccimport.appadjuster.AppAdjuster;

import soleng.framework.standard.codecenter.CodeCenterServerWrapper;

import com.blackducksoftware.soleng.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.soleng.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.soleng.ccimporter.config.CCIConstants;
import com.blackducksoftware.soleng.ccimporter.model.CCIApplication;
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class NumericPrefixedAppAdjuster implements AppAdjuster {
	private static final String NEW_APP_LIST_FILENAME_CMDLINE_ARG = "--new-app-list-filename";

	private static Logger log = LoggerFactory.getLogger(NumericPrefixedAppAdjuster.class.getName());
	
	private static final String NUMERIC_PREFIX_PATTERN_STRING_PROPERTY = "numprefixed.appname.pattern.numericprefix";
	private static final String SEPARATOR_PATTERN_STRING_PROPERTY = "numprefixed.appname.pattern.separator";
	private static final String WORK_STREAM_PATTERN_STRING_PROPERTY = "numprefixed.appname.pattern.workstream";
	
	private static final String ANALYZED_DATE_NEVER_ANALYZED = "numprefixed.analyzeddate.never";
	
	private static final String DATE_FORMAT_STRING_PROPERTY = "numprefixed.analyzed.date.format";
	
	private static final String NUMERIC_PREFIX_ATTRNAME_PROPERTY = "numprefixed.app.attribute.numericprefix";
	private static final String ANALYZED_DATE_ATTRNAME_PROPERTY = "numprefixed.app.attribute.analyzeddate";
	private static final String WORK_STREAM_ATTRNAME_PROPERTY = "numprefixed.app.attribute.workstream";
	
	private static final String NUMERIC_PREFIX_PATTERN_STRING_DEFAULT = "[0-9][0-9][0-9]+";
	private static final String SEPARATOR_PATTERN_STRING_DEFAULT = "-";
	private static final String WORK_STREAM_PATTERN_STRING_DEFAULT = "(PROD|RC1|RC2|RC3|RC4|RC5)";
	
	private static final String ANALYZED_DATE_NEVER_ANALYZED_DEFAULT = "Protex project has never been analyzed";
	
	private static final String PROJECT_STATE_ATTRNAME_PROPERTY = "numprefixed.app.attribute.projectstatus";
	private static final String PROJECT_STATE_VALUE_PROPERTY = "numprefixed.app.value.projectstatus";
	private static final String PROJECT_STATE_VALUE_DEFAULT = "CURRENT";
	
	private static final String WITHOUT_DESCRIPTION_FORMAT_PATTERN_STRING_PROPERTY = "numprefixed.app.name.format.without.description";
	private static final String WITHOUT_DESCRIPTION_FORMAT_PATTERN_STRING_DEFAULT = "[0-9][0-9][0-9]+-(PROD|RC1|RC2|RC3|RC4|RC5)-CURRENT";
	private static final String WITH_DESCRIPTION_FORMAT_PATTERN_STRING_PROPERTY = "numprefixed.app.name.format.with.description";
	private static final String WITH_DESCRIPTION_FORMAT_PATTERN_STRING_DEFAULT = "[0-9][0-9][0-9]+-.*-(PROD|RC1|RC2|RC3|RC4|RC5)-CURRENT";
	
	// Used to find the end of the app description
	private static final String FOLLOWS_DESCRIPTION_PATTERN_STRING_PROPERTY = "numprefixed.appname.pattern.follows.description";
	private static final String FOLLOWS_DESCRIPTION_PATTERN_STRING_DEFAULT = "-(PROD|RC1|RC2|RC3|RC4|RC5)-CURRENT";
	
	private static final String PROJECT_STATE_PATTERN_STRING_PROPERTY = "numprefixed.appname.pattern.projectstatus";
	private static final String PROJECT_STATE_PATTERN_STRING_DEFAULT = "CURRENT";
	
	public static final String NEW_APP_LIST_FILENAME_PROPERTY = "numprefixed.new.app.list.filename";
	
	// These patterns are used to determine whether or not the app name includes the app description.
	// That is: <sealid>-<workstream>-CURRENT vs. <sealid>-<appdescription>-<workstream>-CURRENT
	// They also ensure we only work on app names that conform to one of those formats
	private Pattern withoutDescriptionFormatPattern=null;
	private Pattern withDescriptionFormatPattern=null;
	
	// These patterns are used to extract individual parts of the app name
	private Pattern numericPrefixPattern=null;
	private Pattern separatorPattern=null;
	private Pattern workStreamPattern=null;
	private Pattern projectStatePattern=null;
	
	// Used to find end of app description part of app name
	private Pattern followsDescriptionPattern=null;
	
	private String numericPrefixAttrName=null;
	private String analyzedDateAttrName=null;
	private String workStreamAttrName=null;
	private String projectStateAttrName=null;
	private String projectStateValue=null;
	
	private String dateFormatString;
	
	private CodeCenterServerWrapper ccWrapper;
	private TimeZone tz;
	
	private String analyzedDateNeverString=null;
	
	private String newAppListFilename = null;
	private NumericPrefixedAppListFile newAppList = null;

	public void init(CodeCenterServerWrapper ccWrapper, CCIConfigurationManager config, TimeZone tz) throws CodeCenterImportException {
		this.ccWrapper = ccWrapper;
		this.tz = tz;

		String numericPrefixPatternString = config.getOptionalProperty(NUMERIC_PREFIX_PATTERN_STRING_PROPERTY);
		if (numericPrefixPatternString == null) {
			numericPrefixPatternString = NUMERIC_PREFIX_PATTERN_STRING_DEFAULT;
		}
		
		String separatorPatternString = config.getOptionalProperty(SEPARATOR_PATTERN_STRING_PROPERTY);
		if (separatorPatternString == null) {
			separatorPatternString = SEPARATOR_PATTERN_STRING_DEFAULT;
		}
		
		String workStreamPatternString = config.getOptionalProperty(WORK_STREAM_PATTERN_STRING_PROPERTY);
		if (workStreamPatternString == null) {
			workStreamPatternString = WORK_STREAM_PATTERN_STRING_DEFAULT;
		}
		
		String followsDescriptionPatternString = config.getOptionalProperty(FOLLOWS_DESCRIPTION_PATTERN_STRING_PROPERTY);
		if (followsDescriptionPatternString == null) {
			followsDescriptionPatternString = FOLLOWS_DESCRIPTION_PATTERN_STRING_DEFAULT;
		}
		
		
//		withoutDescriptionFormatPattern
		String withoutDescriptionFormatPatternString = config.getOptionalProperty(WITHOUT_DESCRIPTION_FORMAT_PATTERN_STRING_PROPERTY);
		if (withoutDescriptionFormatPatternString == null) {
			withoutDescriptionFormatPatternString = WITHOUT_DESCRIPTION_FORMAT_PATTERN_STRING_DEFAULT;
		}
		
//		withDescriptionFormatPattern
		String withDescriptionFormatPatternString = config.getOptionalProperty(WITH_DESCRIPTION_FORMAT_PATTERN_STRING_PROPERTY);
		if (withDescriptionFormatPatternString == null) {
			withDescriptionFormatPatternString = WITH_DESCRIPTION_FORMAT_PATTERN_STRING_DEFAULT;
		}
		
//		projectStatePattern
		String projectStatePatternString = config.getOptionalProperty(PROJECT_STATE_PATTERN_STRING_PROPERTY);
		if (projectStatePatternString == null) {
			projectStatePatternString = PROJECT_STATE_PATTERN_STRING_DEFAULT;
		}
		
		String analyzedDateNeverString = config.getOptionalProperty(ANALYZED_DATE_NEVER_ANALYZED);
		if (analyzedDateNeverString == null) {
			analyzedDateNeverString = ANALYZED_DATE_NEVER_ANALYZED_DEFAULT;
		}
		this.analyzedDateNeverString = analyzedDateNeverString;
		
		deriveNewAppListFilename(config);
		
		numericPrefixPattern = Pattern.compile(numericPrefixPatternString);
		separatorPattern = Pattern.compile(separatorPatternString);
		workStreamPattern = Pattern.compile(workStreamPatternString);
		
		withoutDescriptionFormatPattern = Pattern.compile(withoutDescriptionFormatPatternString);
		withDescriptionFormatPattern = Pattern.compile(withDescriptionFormatPatternString);
		projectStatePattern = Pattern.compile(projectStatePatternString);
		followsDescriptionPattern = Pattern.compile(followsDescriptionPatternString);
		
		numericPrefixAttrName = config.getProperty(NUMERIC_PREFIX_ATTRNAME_PROPERTY);
		analyzedDateAttrName = config.getProperty(ANALYZED_DATE_ATTRNAME_PROPERTY);
		workStreamAttrName = config.getProperty(WORK_STREAM_ATTRNAME_PROPERTY);
		projectStateAttrName = config.getProperty(PROJECT_STATE_ATTRNAME_PROPERTY);
		
		String projectStateValue = config.getOptionalProperty(PROJECT_STATE_VALUE_PROPERTY);
		if (projectStateValue == null) {
			projectStateValue = PROJECT_STATE_VALUE_DEFAULT;
		}
		this.projectStateValue = projectStateValue;
		
		dateFormatString = config.getProperty(DATE_FORMAT_STRING_PROPERTY);
	}
	
	private void deriveNewAppListFilename(CCIConfigurationManager config) throws CodeCenterImportException{
		newAppListFilename = null;
		
		// try to get the "new app" list filename from the cmd line first
		String[] args = config.getCmdLineArgs();
		if (args != null) {
			for (int i=0; i < args.length; i++) {
				if ((NEW_APP_LIST_FILENAME_CMDLINE_ARG.equals(args[i])) && (args.length > (i+1))) {
					newAppListFilename = args[i+1];
					break;
				}
			}
		}
		
		// if not set on cmd line, try config properties
		if (newAppListFilename == null) {
			newAppListFilename = config.getOptionalProperty(NEW_APP_LIST_FILENAME_PROPERTY);
		}
		
		// if it was set, start an empty "new app" list
		if (newAppListFilename != null) {
			log.info("A list of created applications will be written to: " + newAppListFilename);
			this.newAppList = new NumericPrefixedAppListFile();
			try {
				newAppList.save(this.newAppListFilename);
			} catch (IOException e) {
				String msg = "Unable to save new application list to file (" + newAppListFilename + "): " + e.getMessage();
				log.error(msg);
				throw new CodeCenterImportException(msg);
			}
		}
	}
	
	public void adjustApp(CCIApplication app,
			CCIProject project) throws CodeCenterImportException {

		if (app.isJustCreated()) {
			if (newAppList != null) {
				log.info("Adding app " + app.getApp().getName() + " to list of created applications");
				newAppList.addApp(app.getApp().getName());
				try {
					newAppList.save(this.newAppListFilename); // save current list, in case this is the last one
				} catch (IOException e) {
					String msg = "Unable to save new application list to file (" + newAppListFilename + "): " + e.getMessage();
					log.error(msg);
					throw new CodeCenterImportException(msg);
				}
			}
		}
		NumericPrefixedAppMetadata metadata = parse(app.getApp().getName());
		setAttributes(app.getApp(), project, metadata, app.isJustCreated());
	}
	
	private void setAttributes(Application app, CCIProject project, NumericPrefixedAppMetadata metadata,
			boolean newApp) throws CodeCenterImportException {
		
		if (newApp) {
			setAttribute(app, metadata, "numeric prefix", numericPrefixAttrName, metadata.getNumericPrefix());
			setAttribute(app, metadata, "work stream", workStreamAttrName, metadata.getWorkStream());
			setAttribute(app, metadata, "project state", projectStateAttrName, projectStateValue);
		}
		
		String analyzedDateString = getDateString(project.getAnalyzedDateValue(), tz, dateFormatString);
		setAttribute(app, metadata, "analyzed date", analyzedDateAttrName, analyzedDateString);
	}
	
	String getDateString(Date date, TimeZone tz, String dateFormatString) {
		if (date == null) {
			return analyzedDateNeverString;
		}
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormatString);
		formatter.setTimeZone(tz);
		return formatter.format(date);
	}
	
	private void setAttribute(Application app, NumericPrefixedAppMetadata metadata, String attrPurpose, String attrName, String attrValue) throws CodeCenterImportException {
		if ((attrName == null) || (attrName.length() == 0) || (attrName.equals("null"))) {
			log.warn("The " + attrPurpose + " custom attribute is not configured, so it has not been set.");
			return;
		}
		
		// Extract to method:
		List<AttributeValue> attributesValues = new ArrayList<AttributeValue>();
        AttributeValue attrValueObject = new AttributeValue();
        AttributeNameToken nameToken1 = new AttributeNameToken();
        nameToken1.setName(attrName);
        attrValueObject.setAttributeId(nameToken1);
        attrValueObject.getValues().add(attrValue);
        attributesValues.add(attrValueObject);
        
        ApplicationUpdate appUpdate = new ApplicationUpdate();
        appUpdate.setId(app.getId());
        appUpdate.getAttributeValues().addAll(attributesValues);
        
        try {
        	ccWrapper.getInternalApiWrapper().getProxy().getApplicationApi().updateApplication(appUpdate);
        } catch (SdkFault e) {
        	String msg = "Error setting value of app attr " + attrName + " on app " + app.getName() + 
        			": " + e.getMessage();
        	log.error(msg);
        	throw new CodeCenterImportException(msg);
        }
        log.info("Updated app=" + app.getName() + ", attr: " + attrName + ", value=" + attrValue);
	}
	
	NumericPrefixedAppMetadata parse(String appName) throws CodeCenterImportException {
		NumericPrefixedAppMetadata metadata = null;
		Scanner scanner = new Scanner(appName);
		
		// Distinguish between without-description and with-description formats:
		// <sealid>-<workstream>-CURRENT vs. <sealid>-<appdescription>-<workstream>-CURRENT vs. non-conforming (other)
		
		// Try: without description
		String currentMatch = scanner.findInLine(withoutDescriptionFormatPattern);
		if (currentMatch != null) {
			// This app name is "without description" format: 
			// <sealid>-<workstream>-CURRENT
			metadata = parseAppNameWithoutDescription(appName);
		} else if (scanner.findInLine(withDescriptionFormatPattern) != null) {
			// This app name is "with description" format: 
			// <sealid>-<appdescription>-<workstream>-CURRENT
			metadata = parseAppNameWithDescription(appName);
		} else {
			// This app name does not conform to either format
			throw new CodeCenterImportException("Application name '" + appName +
					"' does not conform to either of the supported formats");
		}

//		scanner.useDelimiter(separatorPattern);
//		try {
//			
//			if (!scanner.hasNext(numericPrefixPattern)) {
//				String msg = "Error parsing numeric prefix from app name " + appName;
//				log.error(msg);
//				throw new CodeCenterImportException(msg);
//			}
//			String numericPrefix = scanner.next(numericPrefixPattern);
//			metadata.setNumericPrefix(numericPrefix);
//
//			if (!scanner.hasNext(workStreamPattern)) {
//				String appNameString;
//				try {
//					appNameString = scanner.next();
//				} catch (Exception e) { // Some parsing exceptions (like there's no more input) are runtime exceptions
//					String msg = "Error parsing work stream from app name " + appName;
//					log.error(msg);
//					throw new CodeCenterImportException(msg);
//				}
//				metadata.setAppNameString(appNameString);
//			}
//			
//			String workStream;
//			try {
//				workStream = scanner.next(workStreamPattern);
//			} catch (Exception e) { // Some parsing exceptions (like there's no more input) are runtime exceptions
//				String msg = "Error parsing work stream from app name " + appName;
//				log.error(msg);
//				throw new CodeCenterImportException(msg);
//			}
//			metadata.setWorkStream(workStream);
//		
//		} finally {
//			scanner.close();
//		}
		return metadata;
	}
	
	private NumericPrefixedAppMetadata parseAppNameWithoutDescription(String fullAppName) 
			throws CodeCenterImportException {
		NumericPrefixedAppMetadata metadata = new NumericPrefixedAppMetadata();
		Scanner scanner = new Scanner(fullAppName);
		scanner.useDelimiter(separatorPattern);
		try {
			
			if (!scanner.hasNext(numericPrefixPattern)) {
				String msg = "Error parsing numeric prefix from app name " + fullAppName;
				throw new CodeCenterImportException(msg);
			}
			String numericPrefix = scanner.next(numericPrefixPattern);
			metadata.setNumericPrefix(numericPrefix);

			// parse the separator (-) after numericPrefix
			String sep = scanner.findInLine(separatorPattern);
			
			// parse the Work Stream ("PROD", "RC1", etc.)
			String workStream = scanner.findInLine(workStreamPattern);
			if (workStream == null) {
				String msg = "Error parsing work stream from app name " + fullAppName;
				throw new CodeCenterImportException(msg);
			}
			metadata.setWorkStream(workStream);
			
			// parse the separator (-) after work stream
			sep = scanner.findInLine(separatorPattern);

			// parse the Project State ("CURRENT")
			String projectState = scanner.findInLine(projectStatePattern);
			if (projectState == null) {
				String msg = "Error parsing project state from app name " + fullAppName;
				throw new CodeCenterImportException(msg);
			}
		} finally {
			scanner.close();
		}
		
		
		return metadata;
	}
	
	private NumericPrefixedAppMetadata parseAppNameWithDescription(String fullAppName)
			throws CodeCenterImportException {
		NumericPrefixedAppMetadata metadata = new NumericPrefixedAppMetadata();
		Scanner scanner = new Scanner(fullAppName);
		scanner.useDelimiter(separatorPattern);
		try {
			
			if (!scanner.hasNext(numericPrefixPattern)) {
				String msg = "Error parsing numeric prefix from app name " + fullAppName;
				throw new CodeCenterImportException(msg);
			}
			String numericPrefix = scanner.next(numericPrefixPattern);
			metadata.setNumericPrefix(numericPrefix);

			// parse the separator (-) after numericPrefix
			String sep = scanner.findInLine(separatorPattern);
			
			// parse app description
			scanner.useDelimiter(followsDescriptionPattern);
			if (scanner.hasNext()) {
				String description = scanner.next();
				metadata.setAppNameString(description);
			} else {
				String msg = "Error parsing app description from app name " + fullAppName;
				throw new CodeCenterImportException(msg);
			}
			
			// parse the separator (-) after description
			sep = scanner.findInLine(separatorPattern);
			
			// parse the Work Stream ("PROD", "RC1", etc.)
			String workStream = scanner.findInLine(workStreamPattern);
			if (workStream == null) {
				String msg = "Error parsing work stream from app name " + fullAppName;
				throw new CodeCenterImportException(msg);
			}
			metadata.setWorkStream(workStream);
			
			// parse the separator (-) after work stream
			sep = scanner.findInLine(separatorPattern);

			// parse the Project State ("CURRENT")
			String projectState = scanner.findInLine(projectStatePattern);
			if (projectState == null) {
				String msg = "Error parsing project state from app name " + fullAppName;
				throw new CodeCenterImportException(msg);
			}
		} finally {
			scanner.close();
		}
		
		
		return metadata;
	}


}
