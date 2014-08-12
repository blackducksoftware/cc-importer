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
	
	public String getDiffString(Object o) {
		String diffString = null; // Null means they ComponentCollections are identical
		if (o == null) {
			return "Compared component lists are not of the same class: " + this.getClass().getName() + " and " + o.getClass().getName();
		}
		if (! (o instanceof ComponentCollector)) {
			return "Compared component list should be of type ComponentCollector, but is type " + o.getClass().getName();
		}
		ComponentCollector otherComponentCollector = (ComponentCollector)o;
		
		log.info("Comparing two component lists of size " + this.size() +
				" and " + otherComponentCollector.size());
		
		SortedSet<ComponentPojo> otherSortedSet = otherComponentCollector.getSortedSet();
		Iterator<ComponentPojo> otherIterator = otherSortedSet.iterator();
		
		ComponentPojo otherComp;
		for (ComponentPojo thisComp : this.compPojoList) {
			
			if (!otherIterator.hasNext()) {
				diffString = "The other component list has no more components, but this one has more, starting with: " +
						thisComp.getName() + ":" + thisComp.getVersion();
				log.warn(diffString);
				return diffString;
			}
			otherComp = otherIterator.next();
			
			if (!thisComp.getName().equals(otherComp.getName())) {
				diffString = "This component list component name is " + thisComp.getName() + 
						", but the other's component name is " + otherComp.getName() +
						" (this might be OK, it's the ID that must match)";
				log.debug(diffString);
				// Keep going; name mismatches can be OK. It's the KB ID that counts
			}
			
			if (!thisComp.getKbComponentId().equals(otherComp.getKbComponentId())) {
				diffString = "This component list KB ID for " + thisComp.getName() +
						" is " + thisComp.getKbComponentId() + 
						", but the other's KB ID for " + otherComp.getName() +
						" is " + otherComp.getKbComponentId();
				log.warn(diffString);
				return diffString;
			}
			
			if (!thisComp.getVersion().equals(otherComp.getVersion())) {
				diffString = "This component list component version for component " + thisComp.getName() +
						" is " + thisComp.getVersion() + 
						", but the other's component version is " + otherComp.getVersion();
				log.warn(diffString);
				return diffString;
			}
		}
		
		if (otherIterator.hasNext()) {
			otherComp = otherIterator.next();
			diffString = "This component list has no more components, but the other has more, starting with: " +
					otherComp.getName() + ":" + otherComp.getVersion();
			log.warn(diffString);
			return diffString;
		}
		return null; // means compared ComponentCollectors are identical
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
			builder.append("name: " + comp.getName() + "; ");
			builder.append("; version: " + comp.getVersion());
			builder.append("; KB ID: " + comp.getKbComponentId() + "; ");
			builder.append("\n");
		}
		return builder.toString();
	}
}
