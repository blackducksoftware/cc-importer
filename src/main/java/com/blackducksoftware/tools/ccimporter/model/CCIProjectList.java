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
package com.blackducksoftware.tools.ccimporter.model;

import java.util.ArrayList;
import java.util.List;

/**
 * A project list, with an associated list of invalid projects.
 *
 * @author sbillings
 *
 */
public class CCIProjectList {
    private boolean userSpecifiedSubset = false;
    private List<CCIProject> list;
    private final List<CCIProject> invalidList; // list of user specified but
						// invalid
						// projects

    /**
     * Construct an empty list.
     */
    public CCIProjectList() {
	list = new ArrayList<CCIProject>(1000);
	invalidList = new ArrayList<CCIProject>(10);
    }

    /**
     * Construct a list from the given list of valid projects.
     *
     * @param list
     */
    public CCIProjectList(List<CCIProject> list) {
	this.list = list;
	invalidList = new ArrayList<CCIProject>(10);
    }

    /**
     * Set the list of valid projects.
     *
     * @param list
     */
    public void setList(List<CCIProject> list) {
	this.list = list;
    }

    /**
     * Add a project to the list.
     *
     * @param project
     */
    public void addProject(CCIProject project) {
	list.add(project);
    }

    /**
     * Add the given project to the invalid project list.
     *
     * @param projectName
     * @param projectVersion
     */
    public void addInvalidProject(String projectName, String projectVersion) {
	CCIProject project = new CCIProject();
	project.setProjectName(projectName);
	project.setProjectVersion(projectVersion);
	invalidList.add(project);
    }

    /**
     * Is this list a user-specified subset of projects (vs all projects).
     *
     * @return
     */
    public boolean isUserSpecifiedSubset() {
	return userSpecifiedSubset;
    }

    /**
     * Set the boolean that indicates whether this list is a user-specified
     * subset of projects (vs all projects).
     * 
     * @param userSpecifiedSubset
     */
    public void setUserSpecifiedSubset(boolean userSpecifiedSubset) {
	this.userSpecifiedSubset = userSpecifiedSubset;
    }

    /**
     * Get the list of projects.
     *
     * @return
     */
    public List<CCIProject> getList() {
	return list;
    }

    /**
     * Get the associated list of invalid projects.
     *
     * @return
     */
    public List<CCIProject> getInvalidList() {
	return invalidList;
    }

    /**
     * Get the list size.
     *
     * @return
     */
    public int size() {
	return list.size();
    }

    @Override
    public String toString() {
	StringBuilder builder = new StringBuilder();
	for (CCIProject project : getList()) {
	    builder.append("Project name: " + project.getProjectName() + "; ");
	    builder.append("version: " + project.getProjectVersion() + "; ");
	    builder.append("ID: " + project.getProjectKey());
	    builder.append("\n");
	}
	return builder.toString();
    }
}
