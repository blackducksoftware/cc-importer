package com.blackducksoftware.tools.ccimport.mocks;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.codecenter.common.RequestVulnerabilityPojo;
import com.blackducksoftware.tools.connector.codecenter.request.IRequestManager;

public class MockRequestManager implements IRequestManager {
    private List<RequestVulnerabilityPojo> addVulns;

    private List<RequestVulnerabilityPojo> deleteVulns;

    private List<RequestVulnerabilityPojo> updateOperations;

    public MockRequestManager(Date today) {
        addVulns = new ArrayList<>();
        RequestVulnerabilityPojo vuln = new RequestVulnerabilityPojo("testVulnerabilityId", "testVulnerabilityName", "testDescription", "100", "100",
                "100", today, today, today, "testAddRequestId", null, null,
                null, null);
        addVulns.add(vuln);

        deleteVulns = new ArrayList<>();

        vuln = new RequestVulnerabilityPojo("testVulnerabilityId", "testVulnerabilityName", "testDescription", "100", "100",
                "100", today, today, today, "testDeleteRequestId", "test comments", "REMEDIATED",
                today, today);
        deleteVulns.add(vuln);

        updateOperations = new ArrayList<>();
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

    public List<RequestVulnerabilityPojo> getUpdateOperations() {
        return updateOperations;
    }

}
