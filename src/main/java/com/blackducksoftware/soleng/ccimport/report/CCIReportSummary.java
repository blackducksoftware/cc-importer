package com.blackducksoftware.soleng.ccimport.report;

/**
 * Bean to hold info to be read out
 * @author akamen
 *
 */
public class CCIReportSummary {

	private Integer totalProtexProjects = 0;
	private Integer totalCCApplications = 0;
	
	
	public Integer getTotalProtexProjects() {
		return totalProtexProjects;
	}
	public void setTotalProtexProjects(Integer totalProtexProjects) {
		this.totalProtexProjects = totalProtexProjects;
	}
	public Integer getTotalCCApplications() {
		return totalCCApplications;
	}
	public void setTotalCCApplications(Integer totalCCApplications) {
		this.totalCCApplications = totalCCApplications;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("Total Protex projects: " + totalProtexProjects);
		sb.append("\n");
		sb.append("Total CC Applications: " + totalCCApplications);
		
		return sb.toString();
	}
}
