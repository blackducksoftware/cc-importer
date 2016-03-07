package com.blackducksoftware.tools.ccimport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.TimeZone;

import com.blackducksoftware.tools.ccimport.appadjuster.AppAdjuster;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.interceptor.CompChangeInterceptor;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.model.CCIApplication;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public class PlugInManager {
    static Object getAppAdjusterObject(CCIConfigurationManager config)
            throws CodeCenterImportException {

        // See if the user has configured a custom app adjuster
        String appAdjusterClassname = config.getAppAdjusterClassname();
        if (appAdjusterClassname == null) {
            return null; // No custom app adjuster has been configured
        }
        // Get the user-configured custom app adjuster class
        Class<AppAdjuster> sourceClass = null;
        try {
            sourceClass = (Class<AppAdjuster>) Class
                    .forName(appAdjusterClassname);
        } catch (ClassNotFoundException e) {
            String msg = "Unable to convert name to class for custom app adjuster: Class not found: "
                    + appAdjusterClassname;
            throw new CodeCenterImportException(msg);
        }

        // Create an instance of the custom app adjuster class
        Object appAdjusterObject = null;
        try {
            appAdjusterObject = sourceClass.newInstance();
        } catch (IllegalAccessException e) {
            String msg = "Unable to create instance of app adjuster: Illegal access: "
                    + appAdjusterClassname;
            throw new CodeCenterImportException(msg);
        } catch (InstantiationException e) {
            String msg = "Unable to create instance of app adjuster: Instantiation exception: "
                    + appAdjusterClassname;
            throw new CodeCenterImportException(msg);
        }

        return appAdjusterObject;
    }

    /**
     * If a custom app adjuster (to modify app metadata after sync) has been
     * configured, initialize it.
     *
     * @param config
     * @throws CodeCenterImportException
     */
    static Method getAppAdjusterMethod(ICodeCenterServerWrapper ccWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexWrapper,
            CCIConfigurationManager config, Object appAdjusterObject)
            throws CodeCenterImportException {

        // See if the user has configured a custom app adjuster
        String appAdjusterClassname = config.getAppAdjusterClassname();
        if (appAdjusterClassname == null) {
            return null; // No custom app adjuster has been configured
        }
        // Get the user-configured custom app adjuster class
        Class<AppAdjuster> sourceClass = null;
        try {
            sourceClass = (Class<AppAdjuster>) Class
                    .forName(appAdjusterClassname);
        } catch (ClassNotFoundException e) {
            String msg = "Unable to convert name to class for custom app adjuster: Class not found: "
                    + appAdjusterClassname;
            throw new CodeCenterImportException(msg);
        }

        // Get the init method on the custom app adjuster class
        Method initMethod = null;
        Class<?>[] initMethodArgTypes = { ICodeCenterServerWrapper.class,
                IProtexServerWrapper.class, CCIConfigurationManager.class,
                TimeZone.class };
        try {
            initMethod = sourceClass.getDeclaredMethod("init",
                    initMethodArgTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get app adjuster init method: No such method exception: "
                    + appAdjusterClassname;
            throw new CodeCenterImportException(msg);
        }

        // Get the adjustApp method on the custom app adjuster class
        Class<?>[] adjustAppMethodArgTypes = { CCIApplication.class,
                CCIProject.class };
        Method appAdjusterMethod = null;
        try {
            appAdjusterMethod = sourceClass.getDeclaredMethod("adjustApp",
                    adjustAppMethodArgTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get app adjuster method: No such method exception: "
                    + appAdjusterClassname;
            throw new CodeCenterImportException(msg);
        }

        TimeZone tz = TimeZone.getTimeZone(config.getTimeZone());

        // Call the init method to initialize the custom app adjuster
        try {
            initMethod.invoke(appAdjusterObject, ccWrapper, protexWrapper,
                    config, tz);
        } catch (InvocationTargetException e) {
            String msg = "Error initializing custom app adjuster: InvocationTargetException: "
                    + e.getTargetException().getMessage();
            throw new CodeCenterImportException(msg);
        } catch (IllegalAccessException e) {
            String msg = "Error initializing custom app adjuster: IllegalAccessException: "
                    + e.getMessage();
            throw new CodeCenterImportException(msg);
        }

        return appAdjusterMethod;
    }

    static Object getCompChangeInterceptorObject(CCIConfigurationManager config)
            throws CodeCenterImportException {

        // See if the user has configured a custom component change interceptor
        String compChangeInterceptorClassname = config.getCompChangeInterceptorClassname();
        if (compChangeInterceptorClassname == null) {
            return null; // No custom app adjuster has been configured
        }
        // Get the user-configured custom app adjuster class
        Class<CompChangeInterceptor> sourceClass = null;
        try {
            sourceClass = (Class<CompChangeInterceptor>) Class
                    .forName(compChangeInterceptorClassname);
        } catch (ClassNotFoundException e) {
            String msg = "Unable to convert name to class for custom component change interceptor: Class not found: "
                    + compChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        }

        // Create an instance of the component change interceptor class
        Object compChangeInterceptorObject = null;
        try {
            compChangeInterceptorObject = sourceClass.newInstance();
        } catch (IllegalAccessException e) {
            String msg = "Unable to create instance of component change interceptor: Illegal access: "
                    + compChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        } catch (InstantiationException e) {
            String msg = "Unable to create instance of component change interceptor: Instantiation exception: "
                    + compChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        }

        return compChangeInterceptorObject;
    }

    static Method getCompChangeInterceptorInitMethod(ICodeCenterServerWrapper ccWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexWrapper,
            CCIConfigurationManager config, Object compChangeInterceptorObject)
            throws CodeCenterImportException {

        // See if the user has configured a custom comp change interceptor
        String compChangeInterceptorClassname = config.getCompChangeInterceptorClassname();
        if (compChangeInterceptorClassname == null) {
            return null; // No custom comp change interceptor has been configured
        }
        // Get the user-configured custom comp change interceptor class
        Class<CompChangeInterceptor> sourceClass = null;
        try {
            sourceClass = (Class<CompChangeInterceptor>) Class
                    .forName(compChangeInterceptorClassname);
        } catch (ClassNotFoundException e) {
            String msg = "Unable to convert name to class for custom comp change interceptor: Class not found: "
                    + compChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        }

        // Get the init method on the custom comp change interceptor class
        Method initMethod = null;
        Class<?>[] initMethodArgTypes = { CCIConfigurationManager.class, ICodeCenterServerWrapper.class,
                IProtexServerWrapper.class };
        try {
            initMethod = sourceClass.getDeclaredMethod("init",
                    initMethodArgTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get component change interceptor init method: No such method exception: "
                    + compChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        }
        return initMethod;
    }

    static Method getCompChangeInterceptorInitForAppMethod(ICodeCenterServerWrapper ccWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexWrapper,
            CCIConfigurationManager config, Object compChangeInterceptorObject)
            throws CodeCenterImportException {

        // See if the user has configured a custom comp change interceptor
        String compChangeInterceptorClassname = config.getCompChangeInterceptorClassname();
        if (compChangeInterceptorClassname == null) {
            return null; // No custom comp change interceptor has been configured
        }
        // Get the user-configured custom comp change interceptor class
        Class<CompChangeInterceptor> sourceClass = null;
        try {
            sourceClass = (Class<CompChangeInterceptor>) Class
                    .forName(compChangeInterceptorClassname);
        } catch (ClassNotFoundException e) {
            String msg = "Unable to convert name to class for custom comp change interceptor: Class not found: "
                    + compChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        }

        // Get the initForApp(String appName) method on the custom comp change interceptor class
        Class<?>[] adjustAppMethodArgTypes = { String.class };
        Method compChangeInterceptorInitForAppMethod = null;
        try {
            compChangeInterceptorInitForAppMethod = sourceClass.getDeclaredMethod("initForApp",
                    adjustAppMethodArgTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get comp change interceptor initForApp(String appName) method: No such method exception: "
                    + compChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        }

        return compChangeInterceptorInitForAppMethod;
    }
}
