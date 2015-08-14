package com.blackducksoftware.tools.ccimport.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.tools.commonframework.standard.codecenter.dao.ApplicationDao;

public class CodeCenterComponentCollector extends ComponentCollector {
	private static Logger log = LoggerFactory
		    .getLogger(CodeCenterComponentCollector.class.getName());
	private ApplicationDao dao;

	public CodeCenterComponentCollector(ApplicationDao dao) throws Exception {
		this.dao = dao;
		compPojoList = dao.getComponentsSorted();
	}
}
