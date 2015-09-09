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

package com.blackducksoftware.tools.ccimport.appadjuster.custom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NumericPrefixedAppListFile {
    private final List<String> appNames = new ArrayList<String>();

    public void addApp(String appName) {
	appNames.add(appName);
    }

    public void save(String filename) throws IOException {
	File file = new File(filename);

	// if file doesnt exists, then create it
	if (!file.exists()) {
	    file.createNewFile();
	}

	FileWriter fw = new FileWriter(file.getAbsoluteFile());
	BufferedWriter bw = new BufferedWriter(fw);
	for (String appName : appNames) {
	    bw.write(toLine(appName));
	}
	bw.close();
    }

    @Override
    public String toString() {
	StringBuilder sb = new StringBuilder();
	for (String appName : appNames) {
	    sb.append(toLine(appName));
	}
	return sb.toString();
    }

    private static String toLine(String s) {
	return s + "\n";
    }
}
