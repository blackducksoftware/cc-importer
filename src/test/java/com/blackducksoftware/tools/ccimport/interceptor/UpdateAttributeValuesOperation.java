package com.blackducksoftware.tools.ccimport.interceptor;

import java.util.Set;

import com.blackducksoftware.tools.connector.codecenter.common.AttributeValuePojo;

public class UpdateAttributeValuesOperation {
    private final String compId;

    private final Set<AttributeValuePojo> changedAttrValues;

    public UpdateAttributeValuesOperation(String compId, Set<AttributeValuePojo> changedAttrValues) {
        this.compId = compId;
        this.changedAttrValues = changedAttrValues;
    }

    public String getCompId() {
        return compId;
    }

    public Set<AttributeValuePojo> getChangedAttrValues() {
        return changedAttrValues;
    }

    @Override
    public String toString() {
        return "UpdateAttributeValuesOperation [compId=" + compId + ", changedAttrValues=" + changedAttrValues + "]";
    }

}
