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
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License version 2
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 *******************************************************************************/
package com.blackducksoftware.tools.ccimport.exception;

/**
 * Exception type thrown by cc-importer classes.
 *
 * @author sbillings
 *
 */
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
