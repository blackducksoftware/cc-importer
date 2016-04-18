package com.blackducksoftware.tools.ccimport.mocks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.codecenter.common.RequestVulnerabilityPojo;
import com.blackducksoftware.tools.connector.codecenter.common.VulnerabilitySeverity;
import com.blackducksoftware.tools.connector.codecenter.request.IRequestManager;

public class MockRequestManager implements IRequestManager {
    private List<RequestVulnerabilityPojo> addVulns;

    private List<RequestVulnerabilityPojo> deleteVulns;

    private List<RequestVulnerabilityPojo> updateOperations;

    private List<RequestVulnerabilityPojo> updateUnreviewedAsNullOperations;

    private List<String> createRequestOperations;

    private List<String> deleteRequestOperations;

    private Date today;

    public MockRequestManager(Date today) {
        this.today = today;

        initMockOperations();
    }

    public void initMockOperations() {
        addVulns = new ArrayList<>();
        RequestVulnerabilityPojo vuln = new RequestVulnerabilityPojo("testVulnerabilityId", "testVulnerabilityName", "testDescription",
                VulnerabilitySeverity.HIGH,
                "100", "100",
                "100", today, today, today, "testAddRequestId", null, null,
                null, null);
        addVulns.add(vuln);

        deleteVulns = new ArrayList<>();

        vuln = new RequestVulnerabilityPojo("testVulnerabilityId", "testVulnerabilityName", "testDescription",
                VulnerabilitySeverity.HIGH,
                "100", "100",
                "100", today, today, today, "testDeleteRequestId", "test comments", "REMEDIATED",
                today, today);
        deleteVulns.add(vuln);

        updateOperations = new ArrayList<>();
        updateUnreviewedAsNullOperations = new ArrayList<>();
        createRequestOperations = new ArrayList<>();
        deleteRequestOperations = new ArrayList<>();
    }

    @Override
    public List<RequestVulnerabilityPojo> getVulnerabilitiesByRequestId(String requestId) throws CommonFrameworkException {
        if ("addRequestId".equals(requestId)) {
            return addVulns;
        } else {
            return deleteVulns;
        }
    }

    @Override
    public void updateRequestVulnerability(RequestVulnerabilityPojo updatedRequestVulnerability) throws CommonFrameworkException {
        System.out.println("updateRequestVulnerability(): " + updatedRequestVulnerability);
        updateOperations.add(updatedRequestVulnerability);
    }

    @Override
    public void updateRequestVulnerability(RequestVulnerabilityPojo updatedRequestVulnerability, boolean setUnreviewedAsNull) throws CommonFrameworkException {
        System.out.println("updateRequestVulnerability(): " + updatedRequestVulnerability);
        if (setUnreviewedAsNull) {
            updateUnreviewedAsNullOperations.add(updatedRequestVulnerability);
        } else {
            updateOperations.add(updatedRequestVulnerability);
        }
    }

    public List<RequestVulnerabilityPojo> getUpdateOperations() {
        return updateOperations;
    }

    public List<RequestVulnerabilityPojo> getUpdateUnreviewedAsNullOperations() {
        return updateUnreviewedAsNullOperations;
    }

    @Override
    public String createRequest(String appId, String compId, String licenseId, boolean submit) throws CommonFrameworkException {
        System.out.println("MockRequestManager.createRequest() called");
        createRequestOperations.add(appId + "|" + compId + "|" + licenseId + "|" + submit);
        return "mockRequestId";
    }

    @Override
    public void deleteRequest(String appId, String requestId) throws CommonFrameworkException {
        System.out.println("MockRequestManager.deleteRequest() called");
        deleteRequestOperations.add(appId + "|" + requestId);
    }

    public List<String> getCreateRequestOperations() {
        return createRequestOperations;
    }

    public List<String> getDeleteRequestOperations() {
        return deleteRequestOperations;
    }

}
