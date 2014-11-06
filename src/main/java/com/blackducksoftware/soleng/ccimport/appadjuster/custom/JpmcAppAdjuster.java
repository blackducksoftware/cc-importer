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

public class JpmcAppAdjuster implements AppAdjuster {
	private static Logger log = LoggerFactory.getLogger(JpmcAppAdjuster.class.getName());
	
	private static final String SEAL_ID_PATTERN_STRING_PROPERTY = "jpmc.appname.pattern.sealid";
	private static final String SEPARATOR_PATTERN_STRING_PROPERTY = "jpmc.appname.pattern.separator";
	private static final String WORK_STREAM_PATTERN_STRING_PROPERTY = "jpmc.appname.pattern.workstream";
	
	private static final String DATE_FORMAT_STRING_PROPERTY = "jpmc.analyzed.date.format";
	
	private static final String SEAL_ID_ATTRNAME_PROPERTY = "jpmc.app.attribute.sealid";
	private static final String ANALYZED_DATE_ATTRNAME_PROPERTY = "jpmc.app.attribute.analyzeddate";
	private static final String WORK_STREAM_ATTRNAME_PROPERTY = "jpmc.app.attribute.workstream";
	
	private static final String SEAL_ID_PATTERN_STRING_DEFAULT = "[0-9][0-9][0-9]+";
	private static final String SEPARATOR_PATTERN_STRING_DEFAULT = "-";
	private static final String WORK_STREAM_PATTERN_STRING_DEFAULT = "(PROD|RC1|RC2|RC3|RC4|RC5)";
	
	private Pattern sealIdPattern=null;
	private Pattern separatorPattern=null;
	private Pattern workStreamPattern=null;
	
	private String sealIdAttrName=null;
	private String analyzedDateAttrName=null;
	private String workStreamAttrName=null;
	
	private String dateFormatString;
	
	private CodeCenterServerWrapper ccWrapper;
	private TimeZone tz;

	public void init(CodeCenterServerWrapper ccWrapper, CCIConfigurationManager config, TimeZone tz) {
		this.ccWrapper = ccWrapper;
		this.tz = tz;

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
		
		sealIdAttrName = config.getProperty(SEAL_ID_ATTRNAME_PROPERTY);
		analyzedDateAttrName = config.getProperty(ANALYZED_DATE_ATTRNAME_PROPERTY);
		workStreamAttrName = config.getProperty(WORK_STREAM_ATTRNAME_PROPERTY);
		
		dateFormatString = config.getProperty(DATE_FORMAT_STRING_PROPERTY);
	}
	
	public void adjustApp(Application app,
			CCIProject project) throws CodeCenterImportException {

		JpmcAppMetadata metadata = parse(app.getName());
		setAttributes(app, project, metadata);
	}
	
	private void setAttributes(Application app, CCIProject project, JpmcAppMetadata metadata) throws CodeCenterImportException {
		setAttribute(app, metadata, "SEAL ID", sealIdAttrName, metadata.getSealId());
		setAttribute(app, metadata, "work stream", workStreamAttrName, metadata.getWorkStream());
		
		String analyzedDateString = getDateString(project.getAnalyzedDateValue(), tz, dateFormatString);
		setAttribute(app, metadata, "analyzed date", analyzedDateAttrName, analyzedDateString);

	}
	
	String getDateString(Date date, TimeZone tz, String dateFormatString) {
		SimpleDateFormat formatter = new SimpleDateFormat(dateFormatString);
		formatter.setTimeZone(tz);
		return formatter.format(date);
	}
	
	private void setAttribute(Application app, JpmcAppMetadata metadata, String attrPurpose, String attrName, String attrValue) throws CodeCenterImportException {
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
