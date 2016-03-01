package com.blackducksoftware.tools.ccimport.deprecatedcomp;

import java.util.HashMap;
import java.util.Map;

import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;

public class MockDeprecatedComponentReplacementTable implements DeprecatedComponentReplacementTable {
    private Map<ComponentNameVersionIds, ComponentNameVersionIds> table;

    public MockDeprecatedComponentReplacementTable() {
        table = new HashMap<>();
        table.put(new ComponentNameVersionIds("deprComp1", null), new ComponentNameVersionIds("replComp1", null));
        table.put(new ComponentNameVersionIds("deprComp2", null), new ComponentNameVersionIds("replComp2", null));
        table.put(new ComponentNameVersionIds("deprComp3", "deprComp3Version"), new ComponentNameVersionIds("replComp3", "replComp3Version"));
        table.put(new ComponentNameVersionIds("deprComp4", "deprComp4Version"), new ComponentNameVersionIds("replComp4", "replComp4Version"));
    }

    @Override
    public ComponentNameVersionIds getReplacement(ComponentNameVersionIds deprecatedComponent) {
        return table.get(deprecatedComponent);
    }

}
