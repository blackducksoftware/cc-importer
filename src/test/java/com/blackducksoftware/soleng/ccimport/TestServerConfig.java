package com.blackducksoftware.soleng.ccimport;

import net.jmatrix.eproperties.EProperties;
import soleng.framework.core.config.ConfigurationFile;

public class TestServerConfig {

	private static String ccServerName = null;
	
	private static String ccUsername = null;
	private static String ccPassword = null;
	
	private static String ccSuperUsername = null;
	private static String ccSuperPassword = null;
	
	private static String ccUsername2 = null;
	private static String ccPassword2 = null;
	
	private static String ccUserRole1 = null;
	private static String ccUserRole2 = null;
	private static String ccWorkflow = null;
	private static String ccCustomAttributeTextfield = null;
	private static String ccCustomAttributeTextfield2 = null;
	
//	private static String ccVulnerabilityStatusInProgress = null;
//	private static String ccVulnerabilityStatusRemediated = null;
	
//	private static String ccComponentName = null;
//	private static String ccComponentVersion = null;
//	private static String ccComponentVulnerability1 = null;
//	private static String ccComponentVulnerability2 = null;
	
	private static String protexServerName = null;
	private static String protexUsername = null;
	private static String protexPassword = null;
	
	private static String protexUsername2 = null;
	private static String protexPassword2 = null;
	
	private static String protexServerNameInCc = null;
	
	public static String getCcServerName() {
		ensureTestServerConfigInitialized();
		return ccServerName;
	}
	
	public static String getCcUsername() {
		ensureTestServerConfigInitialized();
		return ccUsername;
	}
	
	public static String getCcPassword() {
		ensureTestServerConfigInitialized();
		return ccPassword;
	}
	
	public static String getCcSuperUsername() {
		ensureTestServerConfigInitialized();
		return ccSuperUsername;
	}
	
	public static String getCcSuperPassword() {
		ensureTestServerConfigInitialized();
		return ccSuperPassword;
	}
	
	public static String getCcUsername2() {
		ensureTestServerConfigInitialized();
		return ccUsername2;
	}

	public static String getCcPassword2() {
		ensureTestServerConfigInitialized();
		return ccPassword2;
	}

	public static String getCcUserRole1() {
		ensureTestServerConfigInitialized();
		return ccUserRole1;
	}
	
	public static String getCcUserRole2() {
		ensureTestServerConfigInitialized();
		return ccUserRole2;
	}
	
	public static String getCcWorkflow() {
		ensureTestServerConfigInitialized();
		return ccWorkflow;
	}
	
	public static String getCcCustomAttributeTextfield() {
		ensureTestServerConfigInitialized();
		return ccCustomAttributeTextfield;
	}
	
//	public static String getCcVulnerabilityStatusInProgress() {
//		ensureTestServerConfigInitialized();
//		return ccVulnerabilityStatusInProgress;
//	}
//	
//	public static String getCcVulnerabilityStatusRemediated() {
//		return ccVulnerabilityStatusRemediated;
//	}
//
//	public static String getCcComponentName() {
//		return ccComponentName;
//	}
//
//	public static String getCcComponentVersion() {
//		return ccComponentVersion;
//	}
//
//	public static String getCcComponentVulnerability1() {
//		return ccComponentVulnerability1;
//	}
//
//	public static String getCcComponentVulnerability2() {
//		return ccComponentVulnerability2;
//	}

	public static String getCcCustomAttributeTextfield2() {
		ensureTestServerConfigInitialized();
		return ccCustomAttributeTextfield2;
	}

	private static void ensureTestServerConfigInitialized() {
		if (ccServerName == null) {
			loadTestServerConfig();
		}
	}
	
	public static String getProtexServerName() {
		ensureTestServerConfigInitialized();
		return protexServerName;
	}
	
	public static String getProtexUsername() {
		ensureTestServerConfigInitialized();
		return protexUsername;
	}
	
	public static String getProtexPassword() {
		ensureTestServerConfigInitialized();
		return protexPassword;
	}
	
	public static String getProtexUsername2() {
		ensureTestServerConfigInitialized();
		return protexUsername2;
	}
	
	public static String getProtexPassword2() {
		ensureTestServerConfigInitialized();
		return protexPassword2;
	}
	
	
	public static String getProtexServerNameInCc() {
		ensureTestServerConfigInitialized();
		return protexServerNameInCc;
	}

	private static void loadTestServerConfig() {
		ConfigurationFile configFile = new ConfigurationFile("src/test/resources/_configureme/test_server_config.properties");
		EProperties props = new EProperties();
		configFile.copyProperties(props);
		ccServerName = props.getProperty("cc.server.name");
		ccUsername = props.getProperty("cc.user.name");
		ccPassword = props.getProperty("cc.password");
		
		ccSuperUsername = props.getProperty("cc.super.user.name");
		ccSuperPassword = props.getProperty("cc.super.password");
		
		ccUsername2 = props.getProperty("cc.user.name.2");
		ccPassword2 = props.getProperty("cc.password.2");
		
		ccUserRole1 = props.getProperty("cc.user.role.1");
		ccUserRole2 = props.getProperty("cc.user.role.2");
		ccWorkflow = props.getProperty("cc.workflow");
		ccCustomAttributeTextfield = props.getProperty("cc.custom.attribute.textfield");
		ccCustomAttributeTextfield2 = props.getProperty("cc.custom.attribute.textfield.2");
		
//		ccVulnerabilityStatusInProgress = props.getProperty("cc.vulnerability.status.inprogress");
//		ccVulnerabilityStatusRemediated = props.getProperty("cc.vulnerability.status.remediated");
		
//		ccComponentName = props.getProperty("cc.component.name");
//		ccComponentVersion = props.getProperty("cc.component.version");
//		ccComponentVulnerability1 = props.getProperty("cc.component.vulnerability.1");
//		ccComponentVulnerability2 = props.getProperty("cc.component.vulnerability.2");
		
		
		protexServerName = props.getProperty("protex.server.name");
		protexUsername = props.getProperty("protex.user.name");
		protexPassword = props.getProperty("protex.password");
		
		protexUsername2 = props.getProperty("protex.user.name.2");
		protexPassword2 = props.getProperty("protex.password.2");
		
		protexServerNameInCc = props.getProperty("protex.server.name.in.cc");
	}
}
