package com.blackducksoftware.tools.ccimport.interceptor;

import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.deprecatedcomp.DeprecatedComponentReplacementTable;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.codecenter.common.AttributeValuePojo;
import com.blackducksoftware.tools.connector.codecenter.common.CodeCenterComponentPojo;

/**
 * This component change interceptor salvages remediation data associated with requests that point to deprecated
 * components
 * AND custom attribute values on the same deprecated components.
 *
 * If an about-to-be-deleted component is a deprecated component, and the replacement component was just added: <br>
 * 1. For each vulnerability that exists on both the deprecated/deleted component and the replacement/added component,
 * this
 * interceptor copies the remediation data (target remediation date, actual remediation date, status, and comment) from
 * the deprecated component's request/vulnerability to the replacement component's request/vulnerability. <br>
 * 2. For each deprecated/deleted component: For each custom attribute value: If the corresponding custom attribute
 * on the added/replacement component is not set or empty: Copy the custom attribute value from the deprecated/deleted
 * component to the added/replacement component.
 *
 * @author sbillings
 *
 */
public class SalvageRemediationDataAndCompCustAttrData extends SalvageRemediationData {
    private final Logger log = LoggerFactory.getLogger(this.getClass().getName());

    /**
     * Code Center will call this constructor
     *
     * @throws CodeCenterImportException
     */
    public SalvageRemediationDataAndCompCustAttrData() {
        super();
    }

    /**
     * Test code can call this constructor to inject the DeprecatedComponentReplacementTable and control behavior
     */
    public SalvageRemediationDataAndCompCustAttrData(DeprecatedComponentReplacementTable table, boolean checkCompDeprecatedFlag,
            Map<String, String> equivalentVulnerabilityIds) {
        super(table, checkCompDeprecatedFlag, equivalentVulnerabilityIds);
    }

    /**
     * Copy custom attribute values from a deleted/deprecated component to the corresponding added/replaced component,
     * without
     * overwriting any values set on the added/replacement component.
     */
    @Override
    protected void salvageComponentCustomAttributeData(CodeCenterComponentPojo deleteComponent, CodeCenterComponentPojo addedComponent)
            throws InterceptorException {
        log.info("Salvaging custom attribute data from component " + deleteComponent.getName() + " / " + deleteComponent.getVersion());
        Set<AttributeValuePojo> changedAttrValues = new TreeSet<>();

        Map<String, AttributeValuePojo> deleteCompAttrValues = deleteComponent.getAttributeValues();
        if (deleteCompAttrValues == null) {
            log.debug("NOT salvaging attribute values from deleted/deprecated component to added/replacmenet component because there are no custom attribute values on the deleted/deprecated component");
            return;
        }
        Map<String, AttributeValuePojo> addedCompAttrValues = addedComponent.getAttributeValues();

        for (String attrName : deleteCompAttrValues.keySet()) {
            log.debug("Delete component: Attribute ID: " + deleteCompAttrValues.get(attrName).getAttrId() + ": " + attrName + ": "
                    + deleteCompAttrValues.get(attrName).getValue());

            AttributeValuePojo addedCompAttrValue = null;
            if (addedCompAttrValues != null) {
                addedCompAttrValue = addedCompAttrValues.get(attrName);
            }
            if (addedCompAttrValue == null) {
                log.info("Added component: Attribute ID: " + deleteCompAttrValues.get(attrName).getAttrId() + ": " + attrName + ": <null>");
            } else {
                log.info("Added component: Attribute ID: " + deleteCompAttrValues.get(attrName).getAttrId() + ": " + attrName + ": "
                        + addedCompAttrValue.getValue());
            }

            if (StringUtils.isBlank(deleteCompAttrValues.get(attrName).getValue())) {
                log.debug("NOT copying this attribute value from deleted/deprecated component to added/replacmenet component because there is no value on the deleted/deprecated component");
                continue;
            }
            if ((addedCompAttrValue != null) && !StringUtils.isBlank(addedCompAttrValue.getValue())) {
                log.debug("NOT copying this attribute value from deleted/deprecated component to added/replacmenet component because there is already a value on the added/replacement component");
                continue;
            }
            AttributeValuePojo salvagedAttrValue = new AttributeValuePojo(deleteCompAttrValues.get(attrName).getAttrId(), attrName, deleteCompAttrValues.get(
                    attrName).getValue());
            changedAttrValues.add(salvagedAttrValue);
        }

        if (changedAttrValues.size() > 0) {
            try {
                ccsw.getComponentManager().updateAttributeValues(CodeCenterComponentPojo.class, addedComponent.getId(),
                        changedAttrValues);
            } catch (CommonFrameworkException e) {
                String msg = "Error updating attribute values for replacement component ID " + addedComponent.getId() + ": " + e.getMessage();
                log.error(msg);
                throw new InterceptorException(msg);
            }
        }

    }
}
