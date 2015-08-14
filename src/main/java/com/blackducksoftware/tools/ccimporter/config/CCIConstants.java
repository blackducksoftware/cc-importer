/**
 * Copyright (C)2014 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms set forth in the Black Duck App Exchange
 * Terms of Use located at:
 * http://www.blackducksoftware.com/legal/appexchange
 * IF YOU DO NOT AGREE TO THE THESE TERMS OR ANY SPECIAL TERMS, 
 * DO NOT ACCESS OR USE THIS SITE OR THE SOFTWARE.
 * 
 * @author Niles Madison, namdison@blackducksoftware.com
 */
package com.blackducksoftware.tools.ccimporter.config;

public class CCIConstants {
	
    	public static final String DESCRIPTION = "Application created by the Code Center Importer version: ";
    	
	public static final String CONFIG_FILE_PROPERTY = "-configFile";

	/*
	 * Protex properties
	 */
	public static final String PROTEX_SERVER_URL_PROPERTY = "env.protex.server";
	public static final String PROTEX_USER_NAME_PROPERTY = "env.protex.user";
	public static final String PROTEX_PASSWORD_PROPERTY = "env.protex.password";

	/*
	 * Code Center properties
	 */
	public static final String CODECENTER_SERVER_NAME_PROPERTY = "env.cc.server";
	public static final String CODECENTER_USER_NAME_PROPERTY = "env.cc.user";
	public static final String CODECENTER_PASSWORD_PROPERTY = "env.cc.password";

	// Protex server property
	public static final String PROTEX_NAME_PROPERTY = "cc.protex.name";

	// Decault application version property
	public static final String VERSION_PROPERTY = "cc.default.app.version";

	// Workflow property
	public static final String WORKFLOW_PROPERTY = "cc.workflow";

	// Owner property
	public static final String OWNER_PROPERTY = "cc.owner";

	// Project
	public static final String PROJECT_PROPERTY = "protex.project.list";
	public static final String PROJECT_FILTER_PROPERTY = "protex.project.name.filter";
	
	public static final String APP_ADJUSTER_CLASSNAME_PROPERTY = "app.adjuster.classname";
	public static final String APP_ADJUSTER_ONLY_IF_BOM_EDITS_PROPERTY = "app.adjuster.only.if.bomedits";
	
	public static final String NUM_THREADS_PROPERTY = "num.threads";
	
	// Validate Step
	public static final String VALIDATE_APPLICATION_PROPERTY = "validate.application";
	public static final String RE_VALIDATE_AFTER_CHANGING_BOM_PROPERTY = "revalidate.after.changing.bom";
	
	public static final String ATTEMPT_TO_FIX_INVALID_ASSOCIATION_PROPERTY = "attempt.to.fix.invalid.association";

	// Option to make the validation "smart" - see Readme
	public static final String VALIDATE_SMART_APPLICATION_PROPERTY = "validate.application.smart";
	
	// TODO: Temporary workaround, remove after API is established
	public static final String VALIDATE_SMART_HOST_NAME_PROPERTY = "validate.smart.host.name";
	public static final String VALIDATE_SMART_TIMEZONE_PROPERTY = "validate.smart.timezone";
	
	// Part of validate, should the requests be added?
	protected static final String SUBMIT_PROPERTY = "cc.submit.request";
	protected static final String ADD_REQUESTS = "validate.requests.add";
	
	// Part of validate, should CC requests be deleted?
	public static final String DELETE_REQUESTS = "validate.requests.delete";

	// Option to run the tool in report mode only.
	public static final String RUN_REPORT_PROPERTY = "generate.report";
}
