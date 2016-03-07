/*******************************************************************************
 * Copyright (C) 2016 Black Duck Software, Inc.
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

import java.lang.reflect.Method;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimporter.config.CodeCenterConfigManager;
import com.blackducksoftware.tools.ccimporter.config.ProtexConfigManager;
import com.blackducksoftware.tools.commonframework.core.config.ConfigConstants.APPLICATION;
import com.blackducksoftware.tools.commonframework.core.config.server.ServerBean;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.CodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.ProtexServerWrapper;

/**
 * Main entry point for the utility.
 *
 * There are two modes: Single (Protex) Server, and Multi (Protex) Server.
 *
 * You can configure in an optional AppAdjuster, which modifies the app after
 * the import/sync is done. Currently there is one AppAdjuster available,
 * NumericPrefixedAppAdjuster, developed for a specific customer.
 *
 * @author akamen
 *
 */
public class CCIProjectImporterHarness {
    private static final Logger log = LoggerFactory
            .getLogger(CCIProjectImporterHarness.class.getName());

    private static CodeCenterConfigManager ccConfigManager;

    // Used in the case of single-server mode.
    private static ProtexConfigManager protexConfigManager;

    public static void main(String args[]) {
        if (args.length == 0) {
            log.error("Missing arguments!");
            CodeCenterConfigManager.usage();
            System.exit(-1);
        } else if (args.length == 1) {
            log.info("Configuration file recognized: " + args[0]);
            ccConfigManager = new CodeCenterConfigManager(args[0]);
            protexConfigManager = new ProtexConfigManager(args[0]);
        } else if ((args.length == 3) && (!args[0].startsWith("-"))
                && (!args[2].startsWith("-")) && ("--project".equals(args[1]))) {
            // Special case: <config file> --project <comma-separated project
            // list, or project;version list>
            log.info("Configuration file followed by project list recognized: "
                    + args[0]);
            ccConfigManager = new CodeCenterConfigManager(args[0]);
            protexConfigManager = new ProtexConfigManager(args[0]);
            String projectListString = args[2];
            ccConfigManager.setProjectList(projectListString);
            protexConfigManager.setProjectList(projectListString);
        } else {
            ccConfigManager = new CodeCenterConfigManager(args);
            protexConfigManager = new ProtexConfigManager(args);
        }

        log.info("Running Code Center Importer version: "
                + ccConfigManager.getVersion());

        try {
            CodeCenterServerWrapper codeCenterServerWrapper = createCodeCenterServerWrapper(ccConfigManager);

            /**
             * Here we determine whether we do single or multi-protex support.
             * By simply checking the server list size we have our answer
             * quickly The default number for this tool will be two. One for
             * Protex and one for CC. Anything more than two suggests multiple
             * servers.
             */
            List<ServerBean> servers = ccConfigManager.getServerList();
            CCIProcessor processor = null;
            if (servers.size() > 2) {
                log.info("Multi-Protex mode started.");
                processor = new CCIMultiServerProcessor(ccConfigManager,
                        protexConfigManager, codeCenterServerWrapper);
            } else {
                log.info("Single-Protex mode started");
                ProtexServerWrapper<ProtexProjectPojo> protexServerWrapper = createProtexServerWrapper(protexConfigManager);
                Object appAdjusterObject = PlugInManager.getAppAdjusterObject(ccConfigManager);
                Method appAdjusterMethod = PlugInManager.getAppAdjusterMethod(
                        codeCenterServerWrapper, protexServerWrapper,
                        ccConfigManager, appAdjusterObject);
                processor = new CCISingleServerTaskProcessor(ccConfigManager,
                        protexConfigManager, codeCenterServerWrapper,
                        protexServerWrapper, new SyncProjectTaskFactoryImpl(
                                ccConfigManager, codeCenterServerWrapper,
                                protexServerWrapper, appAdjusterObject,
                                appAdjusterMethod));
            }

            if (ccConfigManager.isRunReport()) {
                log.info("Generate Report mode activated");
                processor.runReport();
            } else {
                processor.performSynchronize();
                if (processor instanceof CCISingleServerTaskProcessor) {
                    System.out.println("\nConsolidated summary: " + ((CCISingleServerTaskProcessor) (processor)).getAggregatedResults());
                }
            }

            log.info("All finished.");
        } catch (Exception e) {
            log.error("General failure: " + e.getMessage());
        }
    }

    private static ProtexServerWrapper<ProtexProjectPojo> createProtexServerWrapper(
            ProtexConfigManager configManager) throws Exception {
        ProtexServerWrapper<ProtexProjectPojo> protexWrapper;
        try {
            // Always just one code center
            ServerBean ccBean = configManager.getServerBean(APPLICATION.CODECENTER);
            if (ccBean == null) {
                throw new Exception(
                        "No valid Protex server configurations found");
            }

            log.info("Using Protex URL [{}]", ccBean.getServerName());

            protexWrapper = new ProtexServerWrapper<>(ccBean, configManager,
                    true);

        } catch (Exception e) {
            throw new Exception("Unable to establish Protex connection: "
                    + e.getMessage());
        }
        return protexWrapper;
    }

    private static CodeCenterServerWrapper createCodeCenterServerWrapper(
            CodeCenterConfigManager configManager) throws Exception {
        CodeCenterServerWrapper codeCenterWrapper;
        try {
            // Always just one code center
            ServerBean ccBean = configManager.getServerBean(APPLICATION.CODECENTER);
            if (ccBean == null) {
                throw new Exception(
                        "No valid Code Center server configurations found");
            }

            log.info("Using Code Center URL [{}]", ccBean.getServerName());

            codeCenterWrapper = new CodeCenterServerWrapper(configManager);

        } catch (Exception e) {
            throw new Exception("Unable to establish Code Center connection: "
                    + e.getMessage());
        }
        return codeCenterWrapper;
    }

}
