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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.tools.ccimporter.config;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * CC Configuration Manager class for CCI.
 *
 * @author akamen
 *
 */
public class CodeCenterConfigManager extends CCIConfigurationManager {
    private Logger log = LoggerFactory.getLogger(this.getClass().getName());

    public CodeCenterConfigManager(String[] args) {
	super();
	initCommandLine(args, APPLICATION.CODECENTER);
    }

    public CodeCenterConfigManager(String fileLocation) {
	super(fileLocation, APPLICATION.CODECENTER);
	log.info("Importing user configuration from file...");
	initConfigFile();
    }

    public CodeCenterConfigManager(Properties props) {
	super(props, APPLICATION.CODECENTER);
	log.info("Importing user configuration from file...");
	initConfigFile();
    }
}