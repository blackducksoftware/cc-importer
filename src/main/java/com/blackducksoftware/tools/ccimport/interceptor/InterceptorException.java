package com.blackducksoftware.tools.ccimport.interceptor;

import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;

public class InterceptorException extends CodeCenterImportException {
    private static final long serialVersionUID = 1L;

    public InterceptorException() {
        super();
    }

    public InterceptorException(String msg) {
        super(msg);
    }

    public InterceptorException(Throwable cause) {
        super(cause);
    }

    public InterceptorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
