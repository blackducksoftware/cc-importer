/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.tools.ccimport.report;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary Bean to keep track of general statistics
 *
 * We do not keep track of imports, because they are not possible to track //
 * since an import is a two-step process (create app, associate) that is always
 * attempted // and its success does not necessarily imply an actual import
 *
 * @author akamen
 *
 */
public class CCIReportSummary {

    private Integer totalProtexProjects = 0;

    private Integer totalCCApplications = 0;

    // Keep track of how many projects were skipped because they didn't match
    // the project name filter
    private Integer totalProjectsSkipped = 0;

    // Keep track of how many validates were performed/skipped
    private Integer totalValidatesPerfomed = 0;

    private Integer totalValidatesSkipped = 0;

    // Keep track of how many imports/validates failed
    private Integer totalImportsFailed = 0;

    private Integer totalValidationsFailed = 0;

    private Integer totalPlugInProcessingFailed = 0;

    // These represent how many total mismatches there are
    private Integer totalPotentialAdds = 0;

    private Integer totalPotentialDeletes = 0;

    // These represent how many went through.
    private Integer totalRequestsAdded = 0;

    private Integer totalRequestsDeleted = 0;

    // List of failed projects
    private List<String> failedImportList = new ArrayList<String>();

    private List<String> failedValidationList = new ArrayList<String>();

    private List<String> failedPlugInProcessingList = new ArrayList<String>();

    public Integer getTotalProtexProjects() {
        return totalProtexProjects;
    }

    public Integer getTotalProjectsSkipped() {
        return totalProjectsSkipped;
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

    public Integer getTotalRequestsAdded() {
        return totalRequestsAdded;
    }

    public Integer getTotalRequestsDeleted() {
        return totalRequestsDeleted;
    }

    /**
     * Increment the deleted requests
     *
     * @param totalRequestsDeleted
     */
    public void addRequestsDeleted(Integer totalRequestsDeleted) {
        this.totalRequestsDeleted += totalRequestsDeleted;
    }

    /**
     * Increment the added requests
     *
     * @param totalRequestsAdded
     */
    public void addRequestsAdded(Integer totalRequestsAdded) {
        this.totalRequestsAdded += totalRequestsAdded;
    }

    /**
     *
     * @param totalPotentialAdds
     */
    public void addTotalPotentialAdds(Integer totalPotentialAdds) {
        this.totalPotentialAdds += totalPotentialAdds;
    }

    /**
     *
     * @param totalPotentialDeletes
     */
    public void addTotalPotentialDeletes(Integer totalPotentialDeletes) {
        this.totalPotentialDeletes += totalPotentialDeletes;
    }

    public Integer getTotalPotentialAdds() {
        return totalPotentialAdds;
    }

    public Integer getTotalPotentialDeletes() {
        return totalPotentialDeletes;
    }

    public Integer getTotalImportsFailed() {
        return totalImportsFailed;
    }

    public Integer getTotalValidationsFailed() {
        return totalValidationsFailed;
    }

    public Integer getTotalPlugInProcessingFailed() {
        return totalPlugInProcessingFailed;
    }

    public List<String> getFailedImportList() {
        return failedImportList;
    }

    public List<String> getFailedValidationList() {
        return failedValidationList;
    }

    public List<String> getFailedPlugInProcessingList() {
        return failedPlugInProcessingList;
    }

    public void addToFailedImportList(String failedImport) {
        failedImportList.add(failedImport);
    }

    public void addToFailedValidationList(String failedValidation) {
        failedValidationList.add(failedValidation);
    }

    public void addToFailedPlugInProcessingList(String failedPlugInProcessing) {
        failedPlugInProcessingList.add(failedPlugInProcessing);
    }

    public Integer getTotalValidatesSkipped() {
        return totalValidatesSkipped;
    }

    // Incremental Adds
    // These are invoked once after each respective action

    public void addTotalProjectsSkipped() {
        totalProjectsSkipped++;
    }

    public Integer getTotalValidatesPerfomed() {
        return totalValidatesPerfomed;
    }

    public void addToTotalValidatesPerfomed() {
        totalValidatesPerfomed++;
    }

    public void addTotalValidationsFailed() {
        totalValidationsFailed++;
    }

    public void addTotalPlugInProcessingFailed() {
        totalPlugInProcessingFailed++;
    }

    public void addTotalImportsFailed() {
        totalImportsFailed++;
    }

    public void addToTotalValidatesSkipped() {
        totalValidatesSkipped++;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();

        sb.append("\n");
        sb.append("Total Projects Analyzed: " + totalProtexProjects);
        sb.append("\n");
        sb.append("Total Projects Skipped based on name filter: "
                + totalProjectsSkipped);
        sb.append("\n");
        sb.append("Total Validations Performed: " + totalValidatesPerfomed);
        sb.append("\n");
        sb.append("Total Validations Skipped: " + totalValidatesSkipped);
        sb.append("\n");
        sb.append("Total Imports Failed: " + totalImportsFailed);
        sb.append("\n");
        sb.append("Total Validations Failed: " + totalValidationsFailed);
        sb.append("\n");
        sb.append("Total PlugIn Processing Failed: " + totalPlugInProcessingFailed);
        sb.append("\n");
        sb.append("Total Potential Request Adds: " + totalPotentialAdds);
        sb.append("\n");
        sb.append("Total Potential Request Deletes: " + totalPotentialDeletes);
        sb.append("\n");
        sb.append("Total Requests Successfully Deleted: "
                + totalRequestsDeleted);
        sb.append("\n");
        sb.append("Total Requests Successfully Added: " + totalRequestsAdded);

        // Build the list of projects
        String listOfFailedValidations = buildList(failedValidationList);
        String listOfFailedImports = buildList(failedImportList);

        sb.append("\n");
        sb.append("List of failed validations: " + listOfFailedValidations);
        sb.append("\n");
        sb.append("List of failed imports: " + listOfFailedImports);

        return sb.toString();
    }

    /**
     * Builds a comma delimited list of project names
     *
     * @param list
     *            of project names
     * @return
     */
    private String buildList(List<String> list) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            String projectName = list.get(i);
            sb.append(projectName);
            if (i != list.size() - 1) {
                sb.append(",");
            }
        }

        return sb.toString();
    }

    public void addReportSummary(CCIReportSummary s2) {
        failedImportList.addAll(s2.getFailedImportList());
        failedValidationList.addAll(s2.getFailedValidationList());
        totalCCApplications += s2.getTotalCCApplications();
        totalImportsFailed += s2.getTotalImportsFailed();
        totalPotentialAdds += s2.getTotalPotentialAdds();
        totalPotentialDeletes += s2.getTotalPotentialDeletes();
        totalProjectsSkipped += s2.getTotalProjectsSkipped();
        totalProtexProjects += s2.getTotalProtexProjects();
        totalRequestsAdded += s2.getTotalRequestsAdded();
        totalRequestsDeleted += s2.getTotalRequestsDeleted();
        totalValidatesPerfomed += s2.getTotalValidatesPerfomed();
        totalValidatesSkipped += s2.getTotalValidatesSkipped();
        totalValidationsFailed += s2.getTotalValidationsFailed();
        totalPlugInProcessingFailed += s2.getTotalPlugInProcessingFailed();
    }

}
