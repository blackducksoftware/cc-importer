package com.blackducksoftware.tools.ccimporter.model;

import java.util.ArrayList;
import java.util.List;

import com.blackducksoftware.tools.ccimporter.model.CCIProject;

public class CCIProjectList {
	private boolean userSpecifiedSubset=false;
	private List<CCIProject> list;
	private List<CCIProject> invalidList; // list of user specified but invalid projects
	
	public CCIProjectList() {
		list = new ArrayList<CCIProject>(1000);;
		invalidList = new ArrayList<CCIProject>(10);
	}
	
	public CCIProjectList(List<CCIProject> list) {
		this.list = list;
		invalidList = new ArrayList<CCIProject>(10);
	}
	
	public void setList(List<CCIProject> list) {
		this.list = list;
	}
	
	public void addProject(CCIProject project) {
		list.add(project);
	}
	
	public void addInvalidProject(String projectName, String projectVersion) {
		CCIProject project = new CCIProject();
		project.setProjectName(projectName);
		project.setProjectVersion(projectVersion);
		invalidList.add(project);
	}

	public boolean isUserSpecifiedSubset() {
		return userSpecifiedSubset;
	}
	public void setUserSpecifiedSubset(boolean userSpecifiedSubset) {
		this.userSpecifiedSubset = userSpecifiedSubset;
	}
	public List<CCIProject> getList() {
		return list;
	}
	
	
	public List<CCIProject> getInvalidList() {
		return invalidList;
	}

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
