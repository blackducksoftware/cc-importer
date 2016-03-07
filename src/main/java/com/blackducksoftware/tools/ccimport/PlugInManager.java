package com.blackducksoftware.tools.ccimport;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.TimeZone;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.blackducksoftware.tools.ccimport.appadjuster.AppAdjuster;
import com.blackducksoftware.tools.ccimport.exception.CodeCenterImportException;
import com.blackducksoftware.tools.ccimport.interceptor.CompChangeInterceptor;
import com.blackducksoftware.tools.ccimporter.config.CCIConfigurationManager;
import com.blackducksoftware.tools.ccimporter.model.CCIApplication;
import com.blackducksoftware.tools.ccimporter.model.CCIProject;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.ICodeCenterServerWrapper;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

/**
 * Checks for and instantiates optional configured plugins: AppAdjuster, Component Change Interceptor.
 *
 * @author sbillings
 *
 */
public class PlugInManager {
    private static final Logger log = LoggerFactory.getLogger(PlugInManager.class.getName());

    /**
     * Get AppAdjuster object
     *
     * @param config
     * @return
     * @throws CodeCenterImportException
     */
    static Object getAppAdjusterObject(CCIConfigurationManager config)
            throws CodeCenterImportException {

        // See if the user has configured a custom app adjuster
        String appAdjusterClassname = config.getAppAdjusterClassname();
        if (appAdjusterClassname == null) {
            log.info("No App Adjuster has been configured");
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
     * configured, initialize it and return its adjustApp(CCIApplication app, CCIProject project) method.
     *
     * @param config
     * @throws CodeCenterImportException
     */
    static Method getAppAdjusterMethod(ICodeCenterServerWrapper ccWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexWrapper,
            CCIConfigurationManager config, Object appAdjusterObject)
            throws CodeCenterImportException {

        if (appAdjusterObject == null) {
            log.warn("App Adjuster object is null");
            return null;
        }
        Class<AppAdjuster> sourceClass = (Class<AppAdjuster>) appAdjusterObject.getClass();

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
                    + appAdjusterObject.getClass().getName();
            throw new CodeCenterImportException(msg);
        }

        // Get the adjustApp method on the custom app adjuster class
        Class<?>[] argTypes = { CCIApplication.class,
                CCIProject.class };
        Method appAdjusterMethod = null;
        try {
            appAdjusterMethod = sourceClass.getDeclaredMethod("adjustApp",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get app adjuster adjustApp(CCIApplication app, CCIProject project) method: No such method exception: "
                    + appAdjusterObject.getClass().getName();
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

    /**
     * Get the Component Change Interceptor
     *
     * @param config
     * @return
     * @throws CodeCenterImportException
     */
    static Object getCompChangeInterceptorObject(CCIConfigurationManager config)
            throws CodeCenterImportException {

        // See if the user has configured a custom component change interceptor
        String compChangeInterceptorClassname = config.getCompChangeInterceptorClassname();
        if (compChangeInterceptorClassname == null) {
            log.info("No Component Change Interceptor has been configured");
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
        log.info("Instantiated " + sourceClass.getName());
        return compChangeInterceptorObject;
    }

    /**
     * Get the Component Change Interceptor init(CCIConfigurationManager config, ICodeCenterServerWrapper ccsw,
     * IProtexServerWrapper psw) method.
     *
     * @param ccWrapper
     * @param protexWrapper
     * @param config
     * @param compChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    static Method getCompChangeInterceptorInitMethod(ICodeCenterServerWrapper ccWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexWrapper,
            CCIConfigurationManager config, Object compChangeInterceptorObject)
            throws CodeCenterImportException {

        if (compChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) compChangeInterceptorObject.getClass();

        // Get the init method on the custom comp change interceptor class
        Method initMethod = null;
        Class<?>[] argTypes = { CCIConfigurationManager.class, ICodeCenterServerWrapper.class,
                IProtexServerWrapper.class };
        try {
            initMethod = sourceClass.getDeclaredMethod("init",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get component change interceptor init(CCIConfigurationManager config, ICodeCenterServerWrapper ccsw, IProtexServerWrapper psw) method: No such method exception: "
                    + sourceClass.getName();
            throw new CodeCenterImportException(msg);
        }
        return initMethod;
    }

    /**
     * Get the Component Change Interceptor initForApp(String appName) method.
     *
     * @param compChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    static Method getCompChangeInterceptorInitForAppMethod(Object compChangeInterceptorObject)
            throws CodeCenterImportException {

        if (compChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) compChangeInterceptorObject.getClass();

        // Get the initForApp(String appName) method on the custom comp change interceptor class
        Class<?>[] argTypes = { String.class };
        Method compChangeInterceptorInitForAppMethod = null;
        try {
            compChangeInterceptorInitForAppMethod = sourceClass.getDeclaredMethod("initForApp",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get comp change interceptor initForApp(String appName) method: No such method exception: "
                    + sourceClass.getName();
            throw new CodeCenterImportException(msg);
        }

        return compChangeInterceptorInitForAppMethod;
    }

    /**
     * Get the Component Change Interceptor preProcessAdd(String compId) method.
     *
     * @param compChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    static Method getCompChangeInterceptorPreProcessAddMethod(Object compChangeInterceptorObject)
            throws CodeCenterImportException {

        if (compChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) compChangeInterceptorObject.getClass();

        // Get the preProcessAdd(String compId) method on the custom comp change interceptor class
        Class<?>[] argTypes = { String.class };
        Method compChangeInterceptorPreProcessAddMethod = null;
        try {
            compChangeInterceptorPreProcessAddMethod = sourceClass.getDeclaredMethod("preProcessAdd",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get comp change interceptor preProcessAdd(String compId) method: No such method exception: "
                    + sourceClass.getName();
            throw new CodeCenterImportException(msg);
        }

        return compChangeInterceptorPreProcessAddMethod;
    }

    /**
     * Get the Component Change Interceptor postProcessAdd(String requestId, String compId) method.
     *
     * @param compChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    static Method getCompChangeInterceptorPostProcessAddMethod(Object compChangeInterceptorObject)
            throws CodeCenterImportException {

        if (compChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) compChangeInterceptorObject.getClass();

        // Get the postProcessAdd(String compId) method on the custom comp change interceptor class
        Class<?>[] argTypes = { String.class, String.class };
        Method compChangeInterceptorPostProcessAddMethod = null;
        try {
            compChangeInterceptorPostProcessAddMethod = sourceClass.getDeclaredMethod("postProcessAdd",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get comp change interceptor postProcessAdd(String requestId, String compId) method: No such method exception: "
                    + sourceClass.getName();
            throw new CodeCenterImportException(msg);
        }

        return compChangeInterceptorPostProcessAddMethod;
    }

    /**
     * Get the Component Change Interceptor preProcessDelete(String requestId, String compId) method.
     *
     * @param compChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    static Method getCompChangeInterceptorPreProcessDeleteMethod(Object compChangeInterceptorObject)
            throws CodeCenterImportException {

        if (compChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) compChangeInterceptorObject.getClass();

        // Get the preProcessDelete(String compId) method on the custom comp change interceptor class
        Class<?>[] argTypes = { String.class, String.class };
        Method compChangeInterceptorPreProcessDeleteMethod = null;
        try {
            compChangeInterceptorPreProcessDeleteMethod = sourceClass.getDeclaredMethod("preProcessDelete",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get comp change interceptor preProcessDelete(String requestId, String compId) method: No such method exception: "
                    + sourceClass.getName();
            throw new CodeCenterImportException(msg);
        }

        return compChangeInterceptorPreProcessDeleteMethod;
    }
}
