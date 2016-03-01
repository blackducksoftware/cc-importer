package com.blackducksoftware.tools.ccimport.deprecatedcomp;

import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;

public interface DeprecatedComponentReplacementTable {
    ComponentNameVersionIds getReplacement(ComponentNameVersionIds deprecatedComponent);
}
