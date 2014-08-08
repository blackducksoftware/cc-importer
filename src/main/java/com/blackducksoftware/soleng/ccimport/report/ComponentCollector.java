package com.blackducksoftware.soleng.ccimport.report;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;

import org.apache.commons.collections.CollectionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.sdk.codecenter.fault.SdkFault;
import com.blackducksoftware.soleng.ccreport.ComponentPojo;
import com.blackducksoftware.soleng.ccreport.datasource.ApplicationDataDao;

public abstract class ComponentCollector {
	private static Logger log = LoggerFactory
		    .getLogger(ComponentCollector.class.getName());
	protected SortedSet<ComponentPojo> compPojoList=null;
	
	public int compareTo(Object o) {
		if (o == null) {
			return -2;
		}
		if (! (o instanceof ComponentCollector)) {
			return -1;
		}
		ComponentCollector otherComponentCollector = (ComponentCollector)o;
		
		log.info("Comparing two Component lists of size " + this.size() +
				" and " + otherComponentCollector.size());
		
		if (this.size() != otherComponentCollector.size()) {
			return 1;
		}
		
		SortedSet<ComponentPojo> otherSortedSet = otherComponentCollector.getSortedSet();
		Iterator<ComponentPojo> otherIterator = otherSortedSet.iterator();
		
		for (ComponentPojo thisComp : this.compPojoList) {
			if (!otherIterator.hasNext()) {
				return 2;
			}
			ComponentPojo otherComp = otherIterator.next();
			
			System.out.println("thisComp: " + thisComp.getName() + " / " + thisComp.getVersion());
			System.out.println("otherComp: " + otherComp.getName() + " / " + otherComp.getVersion());
			
			if (!thisComp.getName().equals(otherComp.getName())) {
				return 3;
			}
			if (!thisComp.getVersion().equals(otherComp.getVersion())) {
				return 4;
			}
		}
		return 0;
	}
	
	public SortedSet getSortedSet() {
		return compPojoList;
	}
	
	public int size() {
		return compPojoList.size();
	}
	
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (ComponentPojo comp : compPojoList) {
			builder.append("Comp ID: " + comp.getId() + "; ");
			builder.append("name: " + comp.getName() + "; ");
			builder.append("version: " + comp.getVersion());
			builder.append("\n");
		}
		return builder.toString();
	}
}
