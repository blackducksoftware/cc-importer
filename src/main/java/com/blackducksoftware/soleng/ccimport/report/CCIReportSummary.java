package com.blackducksoftware.soleng.ccimport.report;

import java.util.ArrayList;
import java.util.List;

/**
 * Bean to hold info to be read out
 * 
 * @author akamen
 * 
 */
public class CCIReportSummary
{

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

    // List of failed projects
    private List<String> failedImportList = new ArrayList<String>();
    private List<String> failedValidationList = new ArrayList<String>();

    public Integer getTotalProtexProjects()
    {
	return totalProtexProjects;
    }

    public void setTotalProtexProjects(Integer totalProtexProjects)
    {
	this.totalProtexProjects = totalProtexProjects;
    }

    public Integer getTotalCCApplications()
    {
	return totalCCApplications;
    }

    public void setTotalCCApplications(Integer totalCCApplications)
    {
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
     * 
     * @param totalRequestsDeleted
     */
    public void addRequestsDeleted(Integer totalRequestsDeleted)
    {
	this.totalRequestsDeleted += totalRequestsDeleted;
    }

    /**
     * Increment the added requests
     * 
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

    public List<String> getFailedImportList()
    {
	return failedImportList;
    }

    public List<String> getFailedValidationList()
    {
	return failedValidationList;
    }

    public void addToFailedImportList(String failedImport)
    {
	failedImportList.add(failedImport);
    }
    
    public void addToFailedValidationList(String failedValidation)
    {
	failedValidationList.add(failedValidation);
    }

    public String toString()
    {
	StringBuilder sb = new StringBuilder();

	sb.append("\n");
	sb.append("Total Projects Analyzed: " + totalProtexProjects);
	sb.append("\n");
	sb.append("Total Imports Failed: " + totalImportsFailed);
	sb.append("\n");
	sb.append("Total Validations Failed: " + totalValidationsFailed);
	sb.append("\n");
	sb.append("Total Potential Adds: " + totalPotentialAdds);
	sb.append("\n");
	sb.append("Total Potential Deletes: " + totalPotentialDeletes);
	sb.append("\n");
	sb.append("Total Requests Successfully Deleted: "
		+ totalRequestsDeleted);
	sb.append("\n");
	sb.append("Total Requests Successfully Added: " + totalRequestsAdded);

	// Build the list of projects
	String listOfFailedValidations = buildList(failedValidationList);
	String listOfFailedImports = buildList(failedValidationList);
	
	sb.append("\n");
	sb.append("List of failed validations: " + listOfFailedValidations);
	sb.append("\n");
	sb.append("List of failed imports: " + listOfFailedImports);
	
	return sb.toString();
    }

    /**
     * Builds a comma delimited list of project names
     * @param list of project names
     * @return
     */
    private String buildList(List<String> list)
    {
	StringBuilder sb= new StringBuilder();
	for(String projectName : list)
	{
	    sb.append(projectName);
	    if(list.iterator().hasNext())
		sb.append(",");
	}
	
	return sb.toString();
    }
}
