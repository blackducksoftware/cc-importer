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
package com.blackducksoftware.tools.ccimport.report;

import java.util.Iterator;
import java.util.SortedSet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.commonframework.standard.codecenter.pojo.ComponentPojo;

public abstract class ComponentCollector {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());
    protected SortedSet<ComponentPojo> compPojoList = null;

    public String getDiffString(Object o) {
	String diffString = null; // Null means they ComponentCollections are
				  // identical
	if (o == null) {
	    return "ComponentCollector.getDiffString(): The given object reference is null";
	}
	if (!(o instanceof ComponentCollector)) {
	    return "Compared component list should be of type ComponentCollector, but is type "
		    + o.getClass().getName();
	}
	ComponentCollector otherComponentCollector = (ComponentCollector) o;

	log.info("Comparing two component lists of size " + size() + " and "
		+ otherComponentCollector.size());

	SortedSet<ComponentPojo> otherSortedSet = otherComponentCollector
		.getSortedSet();
	Iterator<ComponentPojo> otherIterator = otherSortedSet.iterator();

	ComponentPojo otherComp;
	for (ComponentPojo thisComp : compPojoList) {

	    if (!otherIterator.hasNext()) {
		diffString = "The other component list has no more components, but this one has more, starting with: "
			+ thisComp.getName() + ":" + thisComp.getVersion();
		log.warn(diffString);
		return diffString;
	    }
	    otherComp = otherIterator.next();

	    if (!thisComp.getName().equals(otherComp.getName())) {
		diffString = "This component list component name is "
			+ thisComp.getName()
			+ ", but the other's component name is "
			+ otherComp.getName()
			+ " (this might be OK, it's the ID that must match)";
		log.debug(diffString);
		// Keep going; name mismatches can be OK. It's the KB ID that
		// counts
	    }

	    if (!thisComp.getKbComponentId().equals(
		    otherComp.getKbComponentId())) {
		diffString = "This component list KB ID for "
			+ thisComp.getName() + " is "
			+ thisComp.getKbComponentId()
			+ ", but the other's KB ID for " + otherComp.getName()
			+ " is " + otherComp.getKbComponentId();
		log.warn(diffString);
		return diffString;
	    }

	    if (!thisComp.getVersion().equals(otherComp.getVersion())) {
		diffString = "This component list component version for component "
			+ thisComp.getName()
			+ " is "
			+ thisComp.getVersion()
			+ ", but the other's component version is "
			+ otherComp.getVersion();
		log.warn(diffString);
		return diffString;
	    }
	}

	if (otherIterator.hasNext()) {
	    otherComp = otherIterator.next();
	    diffString = "This component list has no more components, but the other has more, starting with: "
		    + otherComp.getName() + ":" + otherComp.getVersion();
	    log.warn(diffString);
	    return diffString;
	}
	return null; // means compared ComponentCollectors are identical
    }

    public SortedSet<ComponentPojo> getSortedSet() {
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
