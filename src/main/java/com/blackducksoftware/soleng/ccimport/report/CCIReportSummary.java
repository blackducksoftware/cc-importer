package com.blackducksoftware.soleng.ccimport.report;

/**
 * Bean to hold info to be read out
 * @author akamen
 *
 */
public class CCIReportSummary {

	private Integer totalProtexProjects = 0;
	private Integer totalCCApplications = 0;
	
	// These represent how many total mismatches there are 
	private Integer totalPotentialAdds = 0;
	private Integer totalPotentialDeletes = 0;
	
	// These represent how many went through. 
	private Integer totalRequestsAdded = 0;
	private Integer totalRequestsDeleted = 0;
	
	// Keep track of how many imports/validates failed
	private Integer totalImportsFailed = 0;
	private Integer totalValidationsFailed = 0;
	
	
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
	
	public Integer getTotalRequestsAdded()
	{
	    return totalRequestsAdded;
	}
	public Integer getTotalRequestsDeleted()
	{
	    return totalRequestsDeleted;
	}
	
	/**
	 * Increment the deleted requests
	 * @param totalRequestsDeleted
	 */
	public void addRequestsDeleted(Integer totalRequestsDeleted)
	{
	    this.totalRequestsDeleted += totalRequestsDeleted;
	}
	/**
	 * Increment the added requests
	 * @param totalRequestsAdded
	 */
	public void addRequestsAdded(Integer totalRequestsAdded)
	{
	    this.totalRequestsAdded += totalRequestsAdded;
	}
	/**
	 * 
	 * @param totalPotentialAdds
	 */
	public void addTotalPotentialAdds(Integer totalPotentialAdds)
	{
	    this.totalPotentialAdds += totalPotentialAdds;
	}
        /**
         * 
         * @param totalPotentialDeletes
         */
	public void addTotalPotentialDeletes(Integer totalPotentialDeletes)
	{
	    this.totalPotentialDeletes += totalPotentialDeletes;
	}

	public Integer getTotalPotentialAdds()
	{
	    return totalPotentialAdds;
	}
	public Integer getTotalPotentialDeletes()
	{
	    return totalPotentialDeletes;
	}
	
	public Integer getTotalImportsFailed()
	{
	    return totalImportsFailed;
	}

	public Integer getTotalValidationsFailed()
	{
	    return totalValidationsFailed;
	}
	
	public void addTotalValidationsFailed(Integer totalValidationsFailed)
	{
	    this.totalValidationsFailed += totalValidationsFailed;
	}
	public void addTotalImportsFailed(Integer totalImportsFailed)
	{
	    this.totalImportsFailed += totalImportsFailed;
	}
	
	public String toString()
	{
		StringBuilder sb = new StringBuilder();
		
		sb.append("\n");
		sb.append("Total Imports Failed: " + this.totalImportsFailed);
		sb.append("\n");
		sb.append("Total Validations Failed: " + this.totalValidationsFailed);
		sb.append("\n");
		sb.append("Total Potential Adds: " + totalPotentialAdds);
		sb.append("\n");
		sb.append("Total Potential Deletes: " + totalPotentialDeletes);
		sb.append("\n");
		sb.append("Total Requests Successfully Deleted: " + totalRequestsDeleted);
		sb.append("\n");
		sb.append("Total Requests Successfully Added: " + totalRequestsAdded);
		
		return sb.toString();
	}
	
}
