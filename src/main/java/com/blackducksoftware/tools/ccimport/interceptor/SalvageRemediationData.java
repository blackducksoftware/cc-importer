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
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.codecenter.common.CodeCenterComponentPojo;
import com.blackducksoftware.tools.connector.codecenter.common.RequestVulnerabilityPojo;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;
import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;

/**
 * This component change interceptor salvages remediation data from deprecated components.
 *
 * If an about-to-be-deleted component is a deprecated component, and the replacement component was just added, for
 * each vulnerability that exists on both the deprecated/deleted component and the replacement/added component, this
 * interceptor copies the remediation data (target remediation date, actual remediation date, status, and comment) from
 * the deprecated component's request/vulnerability to the replacement component's request/vulnerability.
 *
 * @author sbillings
 *
 */
public class SalvageRemediationData implements CompChangeInterceptor {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    private ICodeCenterServerWrapper ccsw = null;

    private Map<ComponentNameVersionIds, String> addedRequestIds;

    private Map<ComponentNameVersionIds, CodeCenterComponentPojo> addedComponents;

    private DeprecatedComponentReplacementTable deprecatedComponentReplacementTable;

    private boolean checkCompDeprecatedFlag = true;

    private Map<String, String> equivalentVulnerabilityIds;

    /**
     * Code Center will call this constructor
     *
     * @throws CodeCenterImportException
     */
    public SalvageRemediationData() {
    }

    /**
     * Test code can call this constructor to inject the DeprecatedComponentReplacementTable and control behavior
     */
    public SalvageRemediationData(DeprecatedComponentReplacementTable table, boolean checkCompDeprecatedFlag, Map<String, String> equivalentVulnerabilityIds) {
        log.info("Constructor: Using given DeprecatedComponentReplacementTable");
        deprecatedComponentReplacementTable = table;
        this.checkCompDeprecatedFlag = checkCompDeprecatedFlag;
        this.equivalentVulnerabilityIds = equivalentVulnerabilityIds;
    }

    @Override
    public void init(CCIConfigurationManager config, ICodeCenterServerWrapper ccsw, IProtexServerWrapper<ProtexProjectPojo> psw) throws InterceptorException {
        log.info("init()");
        this.ccsw = ccsw;
        // If no one (like maybe a test) has injected a (possibly mock) replacement table, construct the standard one
        if (deprecatedComponentReplacementTable == null) {
            log.info("Constructing default DeprecatedComponentReplacementTable");
            try {
                deprecatedComponentReplacementTable = new SqlDeprecatedComponentReplacementTable(config);
            } catch (CodeCenterImportException e) {
                throw new InterceptorException(e);
            }
        }
        // This provides a way to, via configuration, disable checking of the deprecated flag on component
        // so all that matters is whether or not it appears in the appropriate replacement table.
        // It is necessary to set this to false when testing using mock deprecated component replacement
        // tables.
        String checkCompDeprecatedFlagString = config.getOptionalProperty("check.component.deprecated.flag");
        if ("false".equalsIgnoreCase(checkCompDeprecatedFlagString)) {
            checkCompDeprecatedFlag = false;
        }
    }

    @Override
    public void initForApp(String appId) throws InterceptorException {
        log.info("initForApp(): Initializing for app ID: " + appId);
        addedRequestIds = new HashMap<>();
        addedComponents = new HashMap<>();
    }

    @Override
    public boolean preProcessAdd(String compId) throws InterceptorException {
        log.info("preProcessAdd(): compId: " + compId);
        return true;
    }

    @Override
    public void postProcessAdd(String requestId, String compId) throws InterceptorException {
        log.info("postProcessAdd(): requestId: " + requestId + "; compId: " + compId);

        // This will impact performance: Now ALL added components are fetched... weren't before
        // The alternative is to use componentManager.getComponentByKbId() [which tries to
        // use ColaApi().getCatalogComponentsByKbComponent(token); and
        // ColaApi().getCatalogComponentByKbComponentRelease(token);] to get only the
        // replacements for deprecated, but I have not been able to get that to work;
        // Don't understand how kb IDs work in Code Center
        CodeCenterComponentPojo addedComp = loadComponent(compId);

        // Custom components: kb ids will both be empty; skip over those, they will not be replacements
        if (StringUtils.isBlank(addedComp.getKbComponentId()) && StringUtils.isBlank(addedComp.getKbReleaseId())) {
            log.info("Skipping custom component");
            return; // can ignore added custom components; they won't be replacements
        }
        ComponentNameVersionIds kbIdPair = new ComponentNameVersionIds(addedComp.getKbComponentId(), addedComp.getKbReleaseId());
        addedComponents.put(kbIdPair, addedComp);
        addedRequestIds.put(kbIdPair, requestId);
    }

    @Override
    public boolean preProcessDelete(String deleteRequestId, String compId) throws InterceptorException {
        log.info("preProcessDelete(): deleteRequestId: " + deleteRequestId + "; compId: " + compId);
        // Get the component, so we can check its deprecated flag and its KB ID pair
        CodeCenterComponentPojo deleteComp = loadComponent(compId);
        // If not deprecated, return true
        if (checkCompDeprecatedFlag && !deleteComp.isDeprecated()) {
            log.info("This delete component is not deprecated; nothing to do");
            return true; // nothing to do
        }

        // Look up KB ID pair in replacement table to get replacement KB ID pair
        log.info("Looking up replacement component");
        ComponentNameVersionIds deprecatedKbComponent = new ComponentNameVersionIds(deleteComp.getKbComponentId(), deleteComp.getKbReleaseId());
        ComponentNameVersionIds replacementKbComponent = deprecatedComponentReplacementTable.getReplacement(deprecatedKbComponent);

        if (replacementKbComponent == null) {
            if (checkCompDeprecatedFlag) {
                throw new InterceptorException("Component " + deleteComp + " is deprecated, but replacement not found");
            } else {
                log.info("No replacement found for component... it must not be deprecated; nothing to do: " + deleteComp);
                return true; // nothing to do
            }
        }
        log.info("Replacement component KB IDs: " + replacementKbComponent);

        // Using kbCompId+kbVersionId, look for the replacement component in the set of just-added components
        if (!addedComponents.containsKey(replacementKbComponent)) {
            // This about-to-be-deleted component's replacement is NOT in the just-added list
            log.info("This about-to-be-deleted component is not in the just-added component list; nothing to do");
            return true;
        }

        salvageRequestRemediationData(deleteRequestId, deleteComp, addedRequestIds.get(replacementKbComponent), addedComponents.get(replacementKbComponent));

        return true;
    }

    private CodeCenterComponentPojo loadComponent(String compId) throws InterceptorException {
        CodeCenterComponentPojo comp;
        try {
            comp = ccsw.getComponentManager().getComponentById(CodeCenterComponentPojo.class, compId);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException(e);
        }
        log.info("Loaded comp: " + comp);
        return comp;
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
        log.info("Potentially salvaging remediation metadata from request ID " + deleteRequestId +
                ", deleted comp: " + deleteComp + ", to add comp: " + addComp);

        // In this map we'll put add-side vulnerabilities that yet have no remediation data
        Map<String, RequestVulnerabilityPojo> addVulnsNeedingRemData = collectAddVulnsNeedingRemData(addRequestId);

        // Get the vulnerabilities for this delete request
        log.info("Getting the vulnerabilities for this delete request");
        List<RequestVulnerabilityPojo> deleteVulns = loadVulnerabilitiesByRequestId(deleteRequestId);

        // For each deleteVuln: if possible/appropriate, copy rem data to added replacement
        for (RequestVulnerabilityPojo deleteVuln : deleteVulns) {
            log.debug("delete vuln: " + deleteVuln);
            String vulnerabilityIdEquivalentToDeleteVulnerability = getEquivalentVulnerability(deleteVuln.getVulnerabilityId());
            if (addVulnsNeedingRemData.containsKey(vulnerabilityIdEquivalentToDeleteVulnerability)) {
                log.debug("This vuln on the add side has no remediation data; copying from delete request to add request");
                copyRemediationData(deleteVuln, addVulnsNeedingRemData.get(vulnerabilityIdEquivalentToDeleteVulnerability));
            }
        }
    }

    private List<RequestVulnerabilityPojo> loadVulnerabilitiesByRequestId(String deleteRequestId) throws InterceptorException {
        List<RequestVulnerabilityPojo> deleteVulns;
        try {
            deleteVulns = ccsw.getRequestManager().getVulnerabilitiesByRequestId(deleteRequestId);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException(e);
        }
        return deleteVulns;
    }

    private Map<String, RequestVulnerabilityPojo> collectAddVulnsNeedingRemData(String addRequestId) throws InterceptorException {
        Map<String, RequestVulnerabilityPojo> addVulnsNeedingRemData = new HashMap<>();
        log.info("Getting the vulnerabilities for this add request");
        List<RequestVulnerabilityPojo> addVulns = loadVulnerabilitiesByRequestId(addRequestId);
        for (RequestVulnerabilityPojo addVuln : addVulns) {
            log.debug("add vuln: " + addVuln);
            if (!addVuln.isRemediationDataSet()) {
                log.debug("This add vuln has no remediation data, so is a candidate destination for salvaged remediation data");
                addVulnsNeedingRemData.put(addVuln.getVulnerabilityId(), addVuln);
            }
        }
        return addVulnsNeedingRemData;
    }

    /**
     * Map the given vulnerability ID to its equivalent (if equivalents provided, and there's a match).
     *
     * @param originalVulnerabilityId
     * @return
     */
    private String getEquivalentVulnerability(String originalVulnerabilityId) {
        if (equivalentVulnerabilityIds == null) {
            return originalVulnerabilityId;
        }
        if (equivalentVulnerabilityIds.containsKey(originalVulnerabilityId)) {
            String equivalentVulnerabilityId = equivalentVulnerabilityIds.get(originalVulnerabilityId);
            log.debug("Mapping vulnerability ID " + originalVulnerabilityId + " to " + equivalentVulnerabilityId);
            return equivalentVulnerabilityId;
        }
        return originalVulnerabilityId;
    }

    private void copyRemediationData(RequestVulnerabilityPojo fromVuln, RequestVulnerabilityPojo toVuln) throws InterceptorException {
        toVuln.setActualRemediationDate(fromVuln.getActualRemediationDate());
        toVuln.setTargetRemediationDate(fromVuln.getTargetRemediationDate());
        toVuln.setComments(fromVuln.getComments());
        toVuln.setReviewStatusName(fromVuln.getReviewStatusName());
        log.debug("Vulnerability update: " + toVuln);
        try {
            ccsw.getRequestManager().updateRequestVulnerability(toVuln);
        } catch (CommonFrameworkException e) {
            throw new InterceptorException(e);
        }
    }

}
