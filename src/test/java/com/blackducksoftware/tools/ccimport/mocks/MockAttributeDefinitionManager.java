package com.blackducksoftware.tools.ccimport.mocks;

import org.apache.log4j.Logger;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.codecenter.attribute.AttributeDefinitionPojo;
import com.blackducksoftware.tools.connector.codecenter.attribute.AttributeGroupType;
import com.blackducksoftware.tools.connector.codecenter.attribute.AttributeValueType;
import com.blackducksoftware.tools.connector.codecenter.attribute.IAttributeDefinitionManager;

public class MockAttributeDefinitionManager implements
        IAttributeDefinitionManager {
    private final Logger log = Logger.getLogger(this.getClass());

    private final AttributeDefinitionPojo attrDef;

    private int attributeValueTypeGetCount = 0;

    public MockAttributeDefinitionManager() {
        attrDef = new AttributeDefinitionPojo("attrId", "Attribute1", "text",
                "Mock Attribute", "Enter something");
    }

    @Override
    public AttributeValueType getAttributeValueTypeWithinGroup(
            AttributeGroupType groupType, String attrName)
            throws CommonFrameworkException {
        attributeValueTypeGetCount++;

        return AttributeValueType.STRING;
    }

    @Override
    public boolean validateAttributeTypeName(AttributeGroupType groupType,
            String attrName) {
        return true;
    }

    @Override
    public AttributeDefinitionPojo getAttributeDefinitionById(String attributeId)
            throws CommonFrameworkException {
        return attrDef;
    }

    @Override
    public AttributeDefinitionPojo getAttributeDefinitionByName(
            String attributeName) throws CommonFrameworkException {
        return attrDef;
    }

    public int getAttributeValueTypeGetCount() {
        return attributeValueTypeGetCount;
    }

    public void setAttributeValueTypeGetCount(int attributeValueTypeGetCount) {
        this.attributeValueTypeGetCount = attributeValueTypeGetCount;
    }

}
