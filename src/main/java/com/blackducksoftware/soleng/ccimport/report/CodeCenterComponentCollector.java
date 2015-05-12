package com.blackducksoftware.soleng.ccimport.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import soleng.framework.standard.codecenter.dao.ApplicationDataDao;

import com.blackducksoftware.sdk.codecenter.fault.SdkFault;

public class CodeCenterComponentCollector extends ComponentCollector {
	private static Logger log = LoggerFactory
		    .getLogger(CodeCenterComponentCollector.class.getName());
	private ApplicationDataDao dao;

	public CodeCenterComponentCollector(ApplicationDataDao dao) throws Exception {
		this.dao = dao;
		compPojoList = dao.getComponentsSorted();
	}
}
