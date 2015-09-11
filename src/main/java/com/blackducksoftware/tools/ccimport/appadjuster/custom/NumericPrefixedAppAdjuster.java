/*******************************************************************************
 * Copyright (C) 2015 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *******************************************************************************/

package com.blackducksoftware.tools.ccimport.appadjuster.custom;

import java.io.IOException;
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
import com.blackducksoftware.sdk.codecenter.attribute.data.AttributeNameToken;
import com.blackducksoftware.sdk.codecenter.common.data.AttributeValue;
import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.sdk.protex.project.Project;
import com.blackducksoftware.tools.ccimport.appadjuster.AppAdjuster;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.model.CCIApplication;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.connector.protex.ProtexServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;

/**
 * A custom appAdjuster for ccimporter for application names with a numeric
 * prefix. Application name formats: <numericPrefix>-<workstream>-<state>
 * <numericPrefix>-<appdescription>-<workstream>-<state>
 *
 * For each application: If it was just created, append it to the "new apps"
 * file Update the application's attribute values: numeric prefix workstream
 * state AppEdit URL Last analyzed date
 *
 * @author sbillings
 *
 */
public class NumericPrefixedAppAdjuster implements AppAdjuster {
    private static final String NEW_APP_LIST_FILENAME_CMDLINE_ARG = "--new-app-list-filename";

    private final Logger log = LoggerFactory.getLogger(this.getClass()
	    .getName());

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

    private static final String APPEDIT_URL_ATTRNAME_PROPERTY = "numprefixed.app.attribute.appediturl";
    private static final String APPEDIT_URL_VALUE_PROPERTY = "numprefixed.app.value.appediturl";

    public static final String NEW_APP_LIST_FILENAME_PROPERTY = "numprefixed.new.app.list.filename";

    public static final String UPDATE_APPEDITURL_ON_OLD_APPS_PROPERTY = "numprefixed.update.appediturl.on.old.apps";

    // These patterns are used to determine whether or not the app name includes
    // the app description.
    // For example:
    // <numericPrefix>-<workstream>-<state> vs.
    // <numericPrefix>-<appdescription>-<workstream>-<state>
    // They also ensure we only work on app names that conform to one of those
    // formats
    private Pattern withoutDescriptionFormatPattern;
    private Pattern withDescriptionFormatPattern;

    // These patterns are used to extract individual parts of the app name
    private Pattern numericPrefixPattern;
    private Pattern separatorPattern;
    private Pattern workStreamPattern;
    private Pattern projectStatePattern;

    // Used to find end of app description part of app name
    private Pattern followsDescriptionPattern;

    private String numericPrefixAttrName;
    private String analyzedDateAttrName;
    private String workStreamAttrName;
    private String projectStateAttrName;
    private String projectStateValue;

    private String appEditUrlAttrName;
    private String appEditUrlValue;

    private String dateFormatString;

    private CodeCenterServerWrapper ccWrapper;
    private ProtexServerWrapper<ProtexProjectPojo> protexWrapper;

    private TimeZone tz;

    private String analyzedDateNeverString;

    private String newAppListFilename;
    private NumericPrefixedAppListFile newAppList;

    private boolean updateAppEditUrlOnOldApps = false;

    @Override
    public void init(CodeCenterServerWrapper ccWrapper,
	    ProtexServerWrapper<ProtexProjectPojo> protexWrapper,
	    CCIConfigurationManager config, TimeZone tz)
	    throws CodeCenterImportException {
	this.ccWrapper = ccWrapper;
	this.protexWrapper = protexWrapper;
	this.tz = tz;

	String numericPrefixPatternString = config
		.getOptionalProperty(NUMERIC_PREFIX_PATTERN_STRING_PROPERTY);
	if (numericPrefixPatternString == null) {
	    numericPrefixPatternString = NUMERIC_PREFIX_PATTERN_STRING_DEFAULT;
	}

	String separatorPatternString = config
		.getOptionalProperty(SEPARATOR_PATTERN_STRING_PROPERTY);
	if (separatorPatternString == null) {
	    separatorPatternString = SEPARATOR_PATTERN_STRING_DEFAULT;
	}

	String workStreamPatternString = config
		.getOptionalProperty(WORK_STREAM_PATTERN_STRING_PROPERTY);
	if (workStreamPatternString == null) {
	    workStreamPatternString = WORK_STREAM_PATTERN_STRING_DEFAULT;
	}

	String followsDescriptionPatternString = config
		.getOptionalProperty(FOLLOWS_DESCRIPTION_PATTERN_STRING_PROPERTY);
	if (followsDescriptionPatternString == null) {
	    followsDescriptionPatternString = FOLLOWS_DESCRIPTION_PATTERN_STRING_DEFAULT;
	}

	// withoutDescriptionFormatPattern
	String withoutDescriptionFormatPatternString = config
		.getOptionalProperty(WITHOUT_DESCRIPTION_FORMAT_PATTERN_STRING_PROPERTY);
	if (withoutDescriptionFormatPatternString == null) {
	    withoutDescriptionFormatPatternString = WITHOUT_DESCRIPTION_FORMAT_PATTERN_STRING_DEFAULT;
	}

	// withDescriptionFormatPattern
	String withDescriptionFormatPatternString = config
		.getOptionalProperty(WITH_DESCRIPTION_FORMAT_PATTERN_STRING_PROPERTY);
	if (withDescriptionFormatPatternString == null) {
	    withDescriptionFormatPatternString = WITH_DESCRIPTION_FORMAT_PATTERN_STRING_DEFAULT;
	}

	// projectStatePattern
	String projectStatePatternString = config
		.getOptionalProperty(PROJECT_STATE_PATTERN_STRING_PROPERTY);
	if (projectStatePatternString == null) {
	    projectStatePatternString = PROJECT_STATE_PATTERN_STRING_DEFAULT;
	}

	String analyzedDateNeverString = config
		.getOptionalProperty(ANALYZED_DATE_NEVER_ANALYZED);
	if (analyzedDateNeverString == null) {
	    analyzedDateNeverString = ANALYZED_DATE_NEVER_ANALYZED_DEFAULT;
	}
	this.analyzedDateNeverString = analyzedDateNeverString;

	deriveNewAppListFilename(config);

	numericPrefixPattern = Pattern.compile(numericPrefixPatternString);
	separatorPattern = Pattern.compile(separatorPatternString);
	workStreamPattern = Pattern.compile(workStreamPatternString);

	withoutDescriptionFormatPattern = Pattern
		.compile(withoutDescriptionFormatPatternString);
	withDescriptionFormatPattern = Pattern
		.compile(withDescriptionFormatPatternString);
	projectStatePattern = Pattern.compile(projectStatePatternString);
	followsDescriptionPattern = Pattern
		.compile(followsDescriptionPatternString);

	numericPrefixAttrName = config
		.getProperty(NUMERIC_PREFIX_ATTRNAME_PROPERTY);
	analyzedDateAttrName = config
		.getProperty(ANALYZED_DATE_ATTRNAME_PROPERTY);
	workStreamAttrName = config.getProperty(WORK_STREAM_ATTRNAME_PROPERTY);
	projectStateAttrName = config
		.getProperty(PROJECT_STATE_ATTRNAME_PROPERTY);

	String projectStateValue = config
		.getOptionalProperty(PROJECT_STATE_VALUE_PROPERTY);
	if (projectStateValue == null) {
	    projectStateValue = PROJECT_STATE_VALUE_DEFAULT;
	}
	this.projectStateValue = projectStateValue;

	appEditUrlAttrName = config
		.getOptionalProperty(APPEDIT_URL_ATTRNAME_PROPERTY);
	if (appEditUrlAttrName != null) {
	    appEditUrlValue = config.getProperty(APPEDIT_URL_VALUE_PROPERTY);
	}

	dateFormatString = config.getProperty(DATE_FORMAT_STRING_PROPERTY);

	String updateAppEditUrlOnOldAppsString = config
		.getOptionalProperty(UPDATE_APPEDITURL_ON_OLD_APPS_PROPERTY);
	if ("true".equalsIgnoreCase(updateAppEditUrlOnOldAppsString)) {
	    updateAppEditUrlOnOldApps = true;
	}
    }

    private void deriveNewAppListFilename(CCIConfigurationManager config)
	    throws CodeCenterImportException {
	newAppListFilename = null;

	// try to get the "new app" list filename from the cmd line first
	String[] args = config.getCmdLineArgs();
	if (args != null) {
	    for (int i = 0; i < args.length; i++) {
		if ((NEW_APP_LIST_FILENAME_CMDLINE_ARG.equals(args[i]))
			&& (args.length > (i + 1))) {
		    newAppListFilename = args[i + 1];
		    break;
		}
	    }
	}

	// if not set on cmd line, try config properties
	if (newAppListFilename == null) {
	    newAppListFilename = config
		    .getOptionalProperty(NEW_APP_LIST_FILENAME_PROPERTY);
	}

	// if it was set, start an empty "new app" list
	if (newAppListFilename != null) {
	    log.info("A list of created applications will be written to: "
		    + newAppListFilename);
	    newAppList = new NumericPrefixedAppListFile();
	    try {
		newAppList.save(newAppListFilename);
	    } catch (IOException e) {
		String msg = "Unable to save new application list to file ("
			+ newAppListFilename + "): " + e.getMessage();
		log.error(msg);
		throw new CodeCenterImportException(msg);
	    }
	}
    }

    @Override
    public void adjustApp(CCIApplication app, CCIProject project)
	    throws CodeCenterImportException {

	if (app.isJustCreated()) {
	    if (newAppList != null) {

		// Make other threads wait while we add to, and write out (to
		// file), newAppList
		synchronized (newAppList) {
		    log.info("Adding app " + app.getApp().getName()
			    + " to list of created applications");
		    newAppList.addApp(app.getApp().getName());
		    try {
			newAppList.save(newAppListFilename); // save
							     // current
							     // list, in
							     // case this
							     // is the last
							     // one
		    } catch (IOException e) {
			String msg = "Unable to save new application list to file ("
				+ newAppListFilename + "): " + e.getMessage();
			log.error(msg);
			throw new CodeCenterImportException(msg);
		    }
		}
	    }
	}
	NumericPrefixedAppMetadata metadata = parse(app.getApp().getName());
	setAttributes(app.getApp(), project, metadata, app.isJustCreated());
    }

    private void setAttributes(Application app, CCIProject project,
	    NumericPrefixedAppMetadata metadata, boolean newApp)
	    throws CodeCenterImportException {

	if (newApp) {
	    setAttribute(app, metadata, "numeric prefix",
		    numericPrefixAttrName, metadata.getNumericPrefix());
	    setAttribute(app, metadata, "work stream", workStreamAttrName,
		    metadata.getWorkStream());
	    setAttribute(app, metadata, "project state", projectStateAttrName,
		    projectStateValue);
	}

	if (newApp || updateAppEditUrlOnOldApps) {
	    setAttribute(app, metadata, "AppEdit URL", appEditUrlAttrName,
		    appEditUrlValue + "?appId=" + app.getId().getId());
	}

	String analyzedDateString = getDateString(getLastAnalyzedDate(project),
		tz, dateFormatString);
	setAttribute(app, metadata, "analyzed date", analyzedDateAttrName,
		analyzedDateString);
    }

    private Date getLastAnalyzedDate(CCIProject project)
	    throws CodeCenterImportException {
	Project sdkProject;
	try {
	    sdkProject = protexWrapper.getInternalApiWrapper().getProjectApi()
		    .getProjectByName(project.getProjectName());
	} catch (com.blackducksoftware.sdk.fault.SdkFault e) {
	    throw new CodeCenterImportException("Error getting project: "
		    + project.getProjectName()
		    + " in order to get lastAnalyzedDate: " + e.getMessage());
	}
	return sdkProject.getLastAnalyzedDate();
    }

    String getDateString(Date date, TimeZone tz, String dateFormatString) {
	if (date == null) {
	    return analyzedDateNeverString;
	}
	SimpleDateFormat formatter = new SimpleDateFormat(dateFormatString);
	formatter.setTimeZone(tz);
	return formatter.format(date);
    }

    private void setAttribute(Application app,
	    NumericPrefixedAppMetadata metadata, String attrPurpose,
	    String attrName, String attrValue) throws CodeCenterImportException {
	if ((attrName == null) || (attrName.length() == 0)
		|| (attrName.equals("null"))) {
	    log.warn("The "
		    + attrPurpose
		    + " custom attribute is not configured, so it has not been set.");
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
	    ccWrapper.getInternalApiWrapper().getProxy().getApplicationApi()
		    .updateApplication(appUpdate);
	} catch (SdkFault e) {
	    String msg = "Error setting value of app attr " + attrName
		    + " on app " + app.getName() + ": " + e.getMessage();
	    log.error(msg);
	    throw new CodeCenterImportException(msg);
	}
	log.info("Updated app=" + app.getName() + ", attr: " + attrName
		+ ", value=" + attrValue);
    }

    NumericPrefixedAppMetadata parse(String appName)
	    throws CodeCenterImportException {
	NumericPrefixedAppMetadata metadata = null;
	Scanner scanner = new Scanner(appName);

	// Distinguish between without-description and with-description formats:
	// <sealid>-<workstream>-CURRENT vs.
	// <sealid>-<appdescription>-<workstream>-CURRENT vs. non-conforming
	// (other)

	try {
	    String currentMatch = scanner
		    .findInLine(withoutDescriptionFormatPattern);
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
		throw new CodeCenterImportException(
			"Application name '"
				+ appName
				+ "' does not conform to either of the supported formats");
	    }
	} finally {
	    scanner.close();
	}

	return metadata;
    }

    private NumericPrefixedAppMetadata parseAppNameWithoutDescription(
	    String fullAppName) throws CodeCenterImportException {
	NumericPrefixedAppMetadata metadata = new NumericPrefixedAppMetadata();
	Scanner scanner = new Scanner(fullAppName);
	scanner.useDelimiter(separatorPattern);
	try {

	    if (!scanner.hasNext(numericPrefixPattern)) {
		String msg = "Error parsing numeric prefix from app name "
			+ fullAppName;
		throw new CodeCenterImportException(msg);
	    }
	    String numericPrefix = scanner.next(numericPrefixPattern);
	    metadata.setNumericPrefix(numericPrefix);

	    // parse the separator (-) after numericPrefix
	    scanner.findInLine(separatorPattern);

	    // parse the Work Stream ("PROD", "RC1", etc.)
	    String workStream = scanner.findInLine(workStreamPattern);
	    if (workStream == null) {
		String msg = "Error parsing work stream from app name "
			+ fullAppName;
		throw new CodeCenterImportException(msg);
	    }
	    metadata.setWorkStream(workStream);

	    // parse the separator (-) after work stream
	    scanner.findInLine(separatorPattern);

	    // parse the Project State ("CURRENT")
	    String projectState = scanner.findInLine(projectStatePattern);
	    if (projectState == null) {
		String msg = "Error parsing project state from app name "
			+ fullAppName;
		throw new CodeCenterImportException(msg);
	    }
	} finally {
	    scanner.close();
	}

	return metadata;
    }

    private NumericPrefixedAppMetadata parseAppNameWithDescription(
	    String fullAppName) throws CodeCenterImportException {
	NumericPrefixedAppMetadata metadata = new NumericPrefixedAppMetadata();
	Scanner scanner = new Scanner(fullAppName);
	scanner.useDelimiter(separatorPattern);
	try {

	    if (!scanner.hasNext(numericPrefixPattern)) {
		String msg = "Error parsing numeric prefix from app name "
			+ fullAppName;
		throw new CodeCenterImportException(msg);
	    }
	    String numericPrefix = scanner.next(numericPrefixPattern);
	    metadata.setNumericPrefix(numericPrefix);

	    // parse the separator (-) after numericPrefix
	    scanner.findInLine(separatorPattern);

	    // parse app description
	    scanner.useDelimiter(followsDescriptionPattern);
	    if (scanner.hasNext()) {
		String description = scanner.next();
		metadata.setAppNameString(description);
	    } else {
		String msg = "Error parsing app description from app name "
			+ fullAppName;
		throw new CodeCenterImportException(msg);
	    }

	    // parse the separator (-) after description
	    scanner.findInLine(separatorPattern);

	    // parse the Work Stream ("PROD", "RC1", etc.)
	    String workStream = scanner.findInLine(workStreamPattern);
	    if (workStream == null) {
		String msg = "Error parsing work stream from app name "
			+ fullAppName;
		throw new CodeCenterImportException(msg);
	    }
	    metadata.setWorkStream(workStream);

	    // parse the separator (-) after work stream
	    scanner.findInLine(separatorPattern);

	    // parse the Project State ("CURRENT")
	    String projectState = scanner.findInLine(projectStatePattern);
	    if (projectState == null) {
		String msg = "Error parsing project state from app name "
			+ fullAppName;
		throw new CodeCenterImportException(msg);
	    }
	} finally {
	    scanner.close();
	}

	return metadata;
    }

}