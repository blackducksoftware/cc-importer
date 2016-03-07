/*******************************************************************************
 * Copyright (C) 2015 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License version 2 only
 * as published by the Free Software Foundation.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.
 *******************************************************************************/

package com.blackducksoftware.tools.ccimport.appadjuster;

import java.util.TimeZone;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.model.CCIApplication;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public class MockAppAdjuster implements AppAdjuster {
    private int adjustAppCalledCount = 0;

    @Override
    public void init(ICodeCenterServerWrapper ccWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexWrapper,
            CCIConfigurationManager config, TimeZone tz) {
    }

    @Override
    public void adjustApp(CCIApplication app, CCIProject project)
            throws CodeCenterImportException {
        adjustAppCalledCount++;
    }

    public int getAdjustAppCalledCount() {
        return adjustAppCalledCount;
    }
}
