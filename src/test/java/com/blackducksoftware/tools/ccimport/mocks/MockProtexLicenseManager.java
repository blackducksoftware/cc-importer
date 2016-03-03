package com.blackducksoftware.tools.ccimport.mocks;

import java.util.ArrayList;
import java.util.List;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.common.ILicenseManager;
import com.blackducksoftware.tools.connector.protex.license.ProtexLicensePojo;
import com.blackducksoftware.tools.connector.protex.license.ProtexLicensePojo.ApprovalState;

public class MockProtexLicenseManager implements
        ILicenseManager<ProtexLicensePojo> {
    private final ProtexLicensePojo license;

    public MockProtexLicenseManager() {
        license = createTestPojo();
    }

    @Override
    public ProtexLicensePojo getLicenseByName(String licenseName)
            throws CommonFrameworkException {
        return license;
    }

    @Override
    public ProtexLicensePojo getLicenseById(String licenseId)
            throws CommonFrameworkException {

        return license;
    }

    private ProtexLicensePojo createTestPojo() {
        List<String> obligationIds = new ArrayList<>(3);
        obligationIds.add("obligation1");
        obligationIds.add("obligation2");
        obligationIds.add("obligation3");
        return new ProtexLicensePojo("testLicenseId", "Test License", null,
                null, null, ApprovalState.NOT_REVIEWED, "Test License Text",
                obligationIds);
    }

}
