/**
Copyright (C)2014 Black Duck Software Inc.
http://www.blackducksoftware.com/
All rights reserved. **/

package com.blackducksoftware.tools.ccimport.appadjuster.custom;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class NumericPrefixedAppListFile {
	List<String> appNames = new ArrayList<String>();
	
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
