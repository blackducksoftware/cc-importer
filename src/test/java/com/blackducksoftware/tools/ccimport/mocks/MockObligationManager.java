package com.blackducksoftware.tools.ccimport.mocks;

import java.util.ArrayList;
import java.util.List;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.protex.obligation.IObligationManager;
import com.blackducksoftware.tools.connector.protex.obligation.ObligationPojo;

public class MockObligationManager implements IObligationManager {

    @Override
    public List<ObligationPojo> getObligationsByLicenseId(String licenseId)
            throws CommonFrameworkException {
        List<ObligationPojo> obligations = new ArrayList<>();
        ObligationPojo obligation = new ObligationPojo("testObligationId",
                "testObligationName", "testObligationDescription",
                "testObligationCategoryId", "testObligationCategoryName");
        obligations.add(obligation);
        return obligations;
    }

}
