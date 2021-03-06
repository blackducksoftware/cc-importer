package com.blackducksoftware.tools.ccimport.mocks;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.blackducksoftware.tools.ccimport.deprecatedcomp.DeprecatedComponentReplacementTable;
import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;

public class MockDeprecatedComponentReplacementTable implements DeprecatedComponentReplacementTable {
    private Map<ComponentNameVersionIds, ComponentNameVersionIds> table;

    public MockDeprecatedComponentReplacementTable(boolean useRealComponents) {
        table = new HashMap<>();
        if (useRealComponents) {
            table.put(new ComponentNameVersionIds("seamonkey169158", "697435"), new ComponentNameVersionIds("seamonkey169158", "4695232"));
        } else {
            table.put(new ComponentNameVersionIds("deprComp1", null), new ComponentNameVersionIds("replComp1", null));
            table.put(new ComponentNameVersionIds("deprComp2", null), new ComponentNameVersionIds("replComp2", null));
            table.put(new ComponentNameVersionIds("deprComp3", "deprComp3Version"), new ComponentNameVersionIds("replComp3", "replComp3Version"));
            table.put(new ComponentNameVersionIds("deprComp4", "deprComp4Version"), new ComponentNameVersionIds("replComp4", "replComp4Version"));
        }
    }

    @Override
    public ComponentNameVersionIds getReplacement(ComponentNameVersionIds deprecatedComponent) {
        return table.get(deprecatedComponent);
    }

    @Override
    public Set<ComponentNameVersionIds> getDeprecatedComponents() {
        return table.keySet();
    }

}
