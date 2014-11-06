/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.soleng.ccimport.appadjuster.custom;

import static org.junit.Assert.assertEquals;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
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
import com.blackducksoftware.soleng.ccimporter.model.CCIProject;

public class NumericPrefixedAppAdjuster implements AppAdjuster {
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
	
	private Pattern numericPrefixPattern=null;
	private Pattern separatorPattern=null;
	private Pattern workStreamPattern=null;
	
	private String numericPrefixAttrName=null;
	private String analyzedDateAttrName=null;
	private String workStreamAttrName=null;
	
	private String dateFormatString;
	
	private CodeCenterServerWrapper ccWrapper;
	private TimeZone tz;
	
	private String analyzedDateNeverString=null;

	public void init(CodeCenterServerWrapper ccWrapper, CCIConfigurationManager config, TimeZone tz) {
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
		
		String analyzedDateNeverString = config.getOptionalProperty(ANALYZED_DATE_NEVER_ANALYZED);
		if (analyzedDateNeverString == null) {
			analyzedDateNeverString = ANALYZED_DATE_NEVER_ANALYZED_DEFAULT;
		}
		this.analyzedDateNeverString = analyzedDateNeverString;
		
		numericPrefixPattern = Pattern.compile(numericPrefixPatternString);
		separatorPattern = Pattern.compile(separatorPatternString);
		workStreamPattern = Pattern.compile(workStreamPatternString);
		
		numericPrefixAttrName = config.getProperty(NUMERIC_PREFIX_ATTRNAME_PROPERTY);
		analyzedDateAttrName = config.getProperty(ANALYZED_DATE_ATTRNAME_PROPERTY);
		workStreamAttrName = config.getProperty(WORK_STREAM_ATTRNAME_PROPERTY);
		
		dateFormatString = config.getProperty(DATE_FORMAT_STRING_PROPERTY);
	}
	
	public void adjustApp(Application app,
			CCIProject project) throws CodeCenterImportException {

		NumericPrefixedAppMetadata metadata = parse(app.getName());
		setAttributes(app, project, metadata);
	}
	
	private void setAttributes(Application app, CCIProject project, NumericPrefixedAppMetadata metadata) throws CodeCenterImportException {
		setAttribute(app, metadata, "numeric prefix", numericPrefixAttrName, metadata.getNumericPrefix());
		setAttribute(app, metadata, "work stream", workStreamAttrName, metadata.getWorkStream());
		
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
        	throw new CodeCenterImportException("Error setting value of app attr " + attrName + " on app " + app.getName() + 
        			": " + e.getMessage());
        }
        log.info("Updated app=" + app.getName() + ", attr: " + attrName + ", value=" + attrValue);
	}
	
	NumericPrefixedAppMetadata parse(String appName) throws CodeCenterImportException {
		NumericPrefixedAppMetadata metadata = new NumericPrefixedAppMetadata();
		Scanner scanner = new Scanner(appName);
		scanner.useDelimiter(separatorPattern);
		try {
			
			if (!scanner.hasNext(numericPrefixPattern)) {
				throw new CodeCenterImportException("Error parsing numeric prefix from app name " + appName);
			}
			String numericPrefix = scanner.next(numericPrefixPattern);
			metadata.setNumericPrefix(numericPrefix);

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
