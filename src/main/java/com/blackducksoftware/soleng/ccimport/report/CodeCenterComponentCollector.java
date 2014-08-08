package com.blackducksoftware.soleng.ccimport.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.soleng.ccreport.ComponentPojo;
import com.blackducksoftware.soleng.ccreport.datasource.ApplicationDataDao;

public class CodeCenterComponentCollector extends ComponentCollector {
	private static Logger log = LoggerFactory
		    .getLogger(CodeCenterComponentCollector.class.getName());
	private ApplicationDataDao dao;

	public CodeCenterComponentCollector(ApplicationDataDao dao, String appId) throws SdkFault {
		this.dao = dao;
		compPojoList = dao.getComponents(appId);
		System.out.println("CodeCenterComponentCollector.getComponentList() has been implemented");
	}
}
