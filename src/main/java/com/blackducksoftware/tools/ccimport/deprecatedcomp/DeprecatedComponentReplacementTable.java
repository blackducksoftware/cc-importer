package com.blackducksoftware.tools.ccimport.deprecatedcomp;

import java.util.Set;

import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;

public interface DeprecatedComponentReplacementTable {
    ComponentNameVersionIds getReplacement(ComponentNameVersionIds deprecatedComponent);

    Set<ComponentNameVersionIds> getDeprecatedComponents();
}
