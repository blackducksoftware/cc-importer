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

/**
 *
 */
package com.blackducksoftware.tools.ccimporter.config;

import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The Protex configuration manager
 *
 * @author Ari Kamen
 * @date Jun 27, 2014
 *
 */
public class ProtexConfigManager extends CCIConfigurationManager {
    private final Logger log = LoggerFactory.getLogger(this.getClass()
	    .getName());

    public ProtexConfigManager(String[] args) {
	super();
	initCommandLine(args, APPLICATION.PROTEX);
    }

    public ProtexConfigManager(String fileLocation) {
	super(fileLocation, APPLICATION.PROTEX);
	log.info("Importing user configuration from file for Protex.");
	initConfigFile();
    }

    public ProtexConfigManager(Properties props) {
	super(props, APPLICATION.PROTEX);
	log.info("Importing user configuration from file for Protex.");
	initConfigFile();
    }
}
