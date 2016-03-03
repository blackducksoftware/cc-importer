package com.blackducksoftware.tools.ccimport.interceptor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.deprecatedcomp.DeprecatedComponentReplacementTable;
import com.blackducksoftware.tools.ccimport.deprecatedcomp.SqlDeprecatedComponentReplacementTable;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.commonframework.core.config.ConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.codecenter.application.ApplicationPojo;
import com.blackducksoftware.tools.connector.codecenter.common.CodeCenterComponentPojo;
import com.blackducksoftware.tools.connector.codecenter.common.RequestVulnerabilityPojo;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;
import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;

public class SalvageRemediationData implements CompChangeInterceptor {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private ICodeCenterServerWrapper ccsw = null;

    private ApplicationPojo app;

    private Map<ComponentNameVersionIds, String> addedRequestIds;

    private Map<ComponentNameVersionIds, CodeCenterComponentPojo> addedComponents;

    private DeprecatedComponentReplacementTable deprecatedComponentReplacementTable;

    /**
     * Code Center will call this constructor
     *
     * @throws CodeCenterImportException
     */
    public SalvageRemediationData() {
    }

    /**
     * Test code can call this constructor to inject the DeprecatedComponentReplacementTable
     */
    public SalvageRemediationData(DeprecatedComponentReplacementTable table) {
        deprecatedComponentReplacementTable = table;
    }

    @Override
    public void init(ConfigurationManager config, ICodeCenterServerWrapper ccsw, IProtexServerWrapper<ProtexProjectPojo> psw) throws InterceptorException {
        this.ccsw = ccsw;
        // If no one (like maybe a test) has injected a (possibly mock) replacement table, construct the standard one
        if (deprecatedComponentReplacementTable == null) {
            try {
                deprecatedComponentReplacementTable = new SqlDeprecatedComponentReplacementTable(config);
            } catch (CodeCenterImportException e) {
                throw new InterceptorException(e);
            }
        }
    }

    @Override
    public void initForApp(String appId) throws InterceptorException {
        try {
            app = ccsw.getApplicationManager().getApplicationById(appId);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException(e.getMessage());
        }
        addedRequestIds = new HashMap<>();
        addedComponents = new HashMap<>();
    }

    @Override
    public boolean preProcessAdd(String compId) throws InterceptorException {
        return true;
    }

    @Override
    public void postProcessAdd(String requestId, String compId) throws InterceptorException {

        // TODO: Possible performance problem: Now ALL added components are fetched... weren't before
        // The alternative is to use componentManager.getComponentByKbId() [which tries to
        // use ColaApi().getCatalogComponentsByKbComponent(token); and
        // ColaApi().getCatalogComponentByKbComponentRelease(token);] to get only the
        // replacements for deprecated, but I have not been able to get that to work;
        // Don't understand how kb IDs work in Code Center
        CodeCenterComponentPojo addedComp;
        try {
            addedComp = ccsw.getComponentManager().getComponentById(CodeCenterComponentPojo.class, compId);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException("Error looking up added component: " + e.getMessage());
        }

        // TODO: Custom components: kb ids will both be empty; skip over those, they will not be replacements
        if (StringUtils.isBlank(addedComp.getKbComponentId()) && StringUtils.isBlank(addedComp.getKbReleaseId())) {
            return; // can ignore added custom components; they won't be replacements
        }
        ComponentNameVersionIds kbIdPair = new ComponentNameVersionIds(addedComp.getKbComponentId(), addedComp.getKbReleaseId());
        addedComponents.put(kbIdPair, addedComp);
        addedRequestIds.put(kbIdPair, requestId);
    }

    @Override
    public boolean preProcessDelete(String deleteRequestId, String compId) throws InterceptorException {

        // Get the component, so we can check its deprecated flag and its KB ID pair
        CodeCenterComponentPojo deleteComp;
        try {
            deleteComp = ccsw.getComponentManager().getComponentById(CodeCenterComponentPojo.class, compId);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException(e);
        }
        // If not deprecated, return true
        if (!deleteComp.isDeprecated()) {
            return true; // nothing to do
        }

        // Look up KB ID pair in replacement table to get replacement KB ID pair
        ComponentNameVersionIds deprecatedKbComponent = new ComponentNameVersionIds(deleteComp.getKbComponentId(), deleteComp.getKbReleaseId());
        ComponentNameVersionIds replacementKbComponent = deprecatedComponentReplacementTable.getReplacement(deprecatedKbComponent);

        if (replacementKbComponent == null) {
            throw new InterceptorException("Component " + deleteComp + " is deprecated, but replacement not found");
        }

        // Using kbCompId+kbVersionId, look for the replacement component in the set of just-added components
        if (!addedComponents.containsKey(replacementKbComponent)) {
            // This about-to-be-deleted component's replacement is NOT in the just-added list
            return true;
        }

        salvageRequestRemediationData(deleteRequestId, deleteComp, addedRequestIds.get(replacementKbComponent), addedComponents.get(replacementKbComponent));

        return true;
    }

    /**
     * Copy remediation data for any vulns common to the given deleted and added comps from deleted to added.
     * For each vulnerability on the given about-to-be-deleted component that also exists on the given just-added
     * component: If there is no remediation data on the just-added component side, copy the remediation data
     * from the about-to-be-deleted side to the just-added side.
     *
     * @param deleteRequestId
     * @param deleteComp
     * @param addComp
     * @throws InterceptorException
     */
    private void salvageRequestRemediationData(String deleteRequestId, CodeCenterComponentPojo deleteComp, String addRequestId, CodeCenterComponentPojo addComp)
            throws InterceptorException {
        System.out.println("*** Potentially salvaging remediation metadata from request ID " + deleteRequestId +
                ", deleted comp: " + deleteComp + ", to add comp: " + addComp);

        // In this map we'll put add-side vulnerabilities that yet have no remediation data
        Map<String, RequestVulnerabilityPojo> addVulnsNeedingRemData = new HashMap<>();
        // Get the vulnerabilities for this add request
        List<RequestVulnerabilityPojo> addVulns;
        try {
            addVulns = ccsw.getRequestManager().getVulnerabilitiesByRequestId(addRequestId);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException(e);
        }
        for (RequestVulnerabilityPojo addVuln : addVulns) {
            System.out.println("*** add vuln: " + addVuln);
            if (!hasRemediationData(addVuln)) {
                addVulnsNeedingRemData.put(addVuln.getVulnerabilityId(), addVuln);
            }
        }

        // Get the vulnerabilities for this delete request
        List<RequestVulnerabilityPojo> deleteVulns;
        try {
            deleteVulns = ccsw.getRequestManager().getVulnerabilitiesByRequestId(deleteRequestId);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException(e);
        }
        for (RequestVulnerabilityPojo deleteVuln : deleteVulns) {
            System.out.println("*** delete vuln: " + deleteVuln);
            if (addVulnsNeedingRemData.containsKey(deleteVuln.getVulnerabilityId())) {
                copyRemediationData(deleteVuln, addVulnsNeedingRemData.get(deleteVuln.getVulnerabilityId()));
            }
        }
    }

    private void copyRemediationData(RequestVulnerabilityPojo fromVuln, RequestVulnerabilityPojo toVuln) throws InterceptorException {
        toVuln.setActualRemediationDate(fromVuln.getActualRemediationDate());
        toVuln.setTargetRemediationDate(fromVuln.getTargetRemediationDate());
        toVuln.setComments(fromVuln.getComments());
        toVuln.setReviewStatusName(fromVuln.getReviewStatusName());
        try {
            ccsw.getRequestManager().updateRequestVulnerability(toVuln);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException(e);
        }
    }

    private boolean hasRemediationData(RequestVulnerabilityPojo vuln) {
        if ((vuln.getActualRemediationDate() == null) &&
                (vuln.getTargetRemediationDate() == null) &&
                StringUtils.isBlank(vuln.getComments()) &&
                StringUtils.isBlank(vuln.getReviewStatusName())) {
            return false;
        } else {
            return true;
        }
    }
}
