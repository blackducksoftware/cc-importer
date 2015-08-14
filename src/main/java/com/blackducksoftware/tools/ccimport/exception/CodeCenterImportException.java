/**
 * Copyright (C)2011 Black Duck Software Inc.
 * http://www.blackducksoftware.com/
 * All rights reserved.
 * 
 * This software is the confidential and proprietary information of
 * Black Duck Software ("Confidential Information"). You shall not
 * disclose such Confidential Information and shall use it only in
 * accordance with the terms set forth in the Black Duck App Exchange
 * Terms of Use located at:
 * http://www.blackducksoftware.com/legal/appexchange
 * IF YOU DO NOT AGREE TO THE THESE TERMS OR ANY SPECIAL TERMS, 
 * DO NOT ACCESS OR USE THIS SITE OR THE SOFTWARE.
 * 
 * @author Dave Meurer, dmeurer@blackducksoftware.com
 */
package com.blackducksoftware.tools.ccimport.exception;

public class CodeCenterImportException extends Exception {

	private static final long serialVersionUID = 1L;

	public CodeCenterImportException() {
		super();
	}

	public CodeCenterImportException(String msg) {
		super(msg);
	}

	public CodeCenterImportException(Throwable cause) {
		super(cause);
	}

	public CodeCenterImportException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
