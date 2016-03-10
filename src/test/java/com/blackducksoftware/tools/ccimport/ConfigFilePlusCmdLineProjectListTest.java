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
package com.blackducksoftware.tools.ccimport;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Test;

import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.core.config.ConfigConstants.APPLICATION;

public class ConfigFilePlusCmdLineProjectListTest {

    @Test
    public void test() {

        String[] args = {
                "src/test/resources/importer_test_all_projects.properties",
                "--project", "p1,p2" };

        CodeCenterConfigManager ccConfigManager = new CodeCenterConfigManager(
                args[0]);
        ProtexConfigManager protexConfigManager = new ProtexConfigManager(
                args[0]);
        String projectListString = args[2];
        ccConfigManager.setProjectList(projectListString);
        protexConfigManager.setProjectList(projectListString);

        assertEquals("testowner", ccConfigManager.getOwner());
        assertEquals("testprotexusername", protexConfigManager.getServerBean(APPLICATION.PROTEX)
                .getUserName());
        List<CCIProject> projects = protexConfigManager.getProjectList();
        boolean foundP1 = false;
        boolean foundP2 = false;
        for (CCIProject project : projects) {
            if ("p1".equals(project.getProjectName())) {
                foundP1 = true;
            } else if ("p2".equals(project.getProjectName())) {
                foundP2 = true;
            }
        }
        assertTrue(foundP1);
        assertTrue(foundP2);
    }

}
