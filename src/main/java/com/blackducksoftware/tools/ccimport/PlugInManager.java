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

    private final ICodeCenterServerWrapper ccWrapper;

    private final IProtexServerWrapper<ProtexProjectPojo> protexWrapper;

    private final CCIConfigurationManager config;

    private Object appAdjusterObject;

    private Method appAdjusterAdjustAppMethod;

    private Object componentChangeInterceptorObject;

    private Method componentChangeInterceptorInitMethod;

    private Method componentChangeInterceptorInitForAppMethod;

    private Method componentChangeInterceptorPreProcessAddMethod;

    private Method componentChangeInterceptorPostProcessAddMethod;

    private Method componentChangeInterceptorPreProcessDeleteMethod;

    /**
     * Use this contstructor to get the app adjuster object from the configuration.
     *
     * @param config
     * @param ccWrapper
     * @param protexWrapper
     * @throws CodeCenterImportException
     */
    public PlugInManager(CCIConfigurationManager config, ICodeCenterServerWrapper ccWrapper,
            IProtexServerWrapper<ProtexProjectPojo> protexWrapper)
            throws CodeCenterImportException {
        this.config = config;
        this.ccWrapper = ccWrapper;
        this.protexWrapper = protexWrapper;

        appAdjusterObject = initAppAdjusterObject();
        appAdjusterAdjustAppMethod = initAppAdjusterAdjustAppMethod();

        componentChangeInterceptorObject = initComponentChangeInterceptorObject();
        componentChangeInterceptorInitMethod = initComponentChangeInterceptorInitMethod();
        componentChangeInterceptorInitForAppMethod = initComponentChangeInterceptorInitForAppMethod();
        componentChangeInterceptorPreProcessAddMethod = initComponentChangeInterceptorPreProcessAddMethod();
        componentChangeInterceptorPostProcessAddMethod = initComponentChangeInterceptorPostProcessAddMethod();
        componentChangeInterceptorPreProcessDeleteMethod = initComponentChangeInterceptorPreProcessDeleteMethod();
    }

    /**
     * Invoke the AppAdjuster adjustApp() method.
     *
     * @param cciApp
     * @param project
     * @throws CodeCenterImportException
     */
    public void invokeAppAdjuster(CCIApplication cciApp, CCIProject project)
            throws CodeCenterImportException {
        if ((appAdjusterObject != null) && (appAdjusterAdjustAppMethod != null)) {
            try {
                appAdjusterAdjustAppMethod.invoke(appAdjusterObject, cciApp, project);
            } catch (InvocationTargetException e) {
                String msg = "Error during post-import application metadata adjustment: InvocationTargetException: "
                        + e.getTargetException().getMessage();
                throw new CodeCenterImportException(msg);
            } catch (IllegalAccessException e) {
                String msg = "Error during post-import application metadata adjustment: IllegalAccessException: "
                        + e.getMessage();
                throw new CodeCenterImportException(msg);
            }
        } else {
            log.info("No AppAdjuster configured");
        }
    }

    /**
     * Get AppAdjuster object
     *
     * @param config
     * @return
     * @throws CodeCenterImportException
     */
    Object initAppAdjusterObject()
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
    Method initAppAdjusterAdjustAppMethod()
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
    Object initComponentChangeInterceptorObject()
            throws CodeCenterImportException {

        // See if the user has configured a custom component change interceptor
        String componentChangeInterceptorClassname = config.getCompChangeInterceptorClassname();
        if (componentChangeInterceptorClassname == null) {
            log.info("No Component Change Interceptor has been configured");
            return null; // No custom app adjuster has been configured
        }
        // Get the user-configured custom app adjuster class
        Class<CompChangeInterceptor> sourceClass = null;
        try {
            sourceClass = (Class<CompChangeInterceptor>) Class
                    .forName(componentChangeInterceptorClassname);
        } catch (ClassNotFoundException e) {
            String msg = "Unable to convert name to class for custom component change interceptor: Class not found: "
                    + componentChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        }

        // Create an instance of the component change interceptor class
        Object componentChangeInterceptorObject = null;
        try {
            componentChangeInterceptorObject = sourceClass.newInstance();
        } catch (IllegalAccessException e) {
            String msg = "Unable to create instance of component change interceptor: Illegal access: "
                    + componentChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        } catch (InstantiationException e) {
            String msg = "Unable to create instance of component change interceptor: Instantiation exception: "
                    + componentChangeInterceptorClassname;
            throw new CodeCenterImportException(msg);
        }
        log.info("Instantiated " + sourceClass.getName());
        return componentChangeInterceptorObject;
    }

    /**
     * Get the Component Change Interceptor init(CCIConfigurationManager config, ICodeCenterServerWrapper ccsw,
     * IProtexServerWrapper psw) method.
     *
     * @param ccWrapper
     * @param protexWrapper
     * @param config
     * @param componentChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    Method initComponentChangeInterceptorInitMethod()
            throws CodeCenterImportException {

        if (componentChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) componentChangeInterceptorObject.getClass();

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
     * @param componentChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    Method initComponentChangeInterceptorInitForAppMethod()
            throws CodeCenterImportException {

        if (componentChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) componentChangeInterceptorObject.getClass();

        // Get the initForApp(String appName) method on the custom comp change interceptor class
        Class<?>[] argTypes = { String.class };
        Method componentChangeInterceptorInitForAppMethod = null;
        try {
            componentChangeInterceptorInitForAppMethod = sourceClass.getDeclaredMethod("initForApp",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get comp change interceptor initForApp(String appName) method: No such method exception: "
                    + sourceClass.getName();
            throw new CodeCenterImportException(msg);
        }

        return componentChangeInterceptorInitForAppMethod;
    }

    /**
     * Get the Component Change Interceptor preProcessAdd(String compId) method.
     *
     * @param componentChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    Method initComponentChangeInterceptorPreProcessAddMethod()
            throws CodeCenterImportException {

        if (componentChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) componentChangeInterceptorObject.getClass();

        // Get the preProcessAdd(String compId) method on the custom comp change interceptor class
        Class<?>[] argTypes = { String.class };
        Method componentChangeInterceptorPreProcessAddMethod = null;
        try {
            componentChangeInterceptorPreProcessAddMethod = sourceClass.getDeclaredMethod("preProcessAdd",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get comp change interceptor preProcessAdd(String compId) method: No such method exception: "
                    + sourceClass.getName();
            throw new CodeCenterImportException(msg);
        }

        return componentChangeInterceptorPreProcessAddMethod;
    }

    /**
     * Get the Component Change Interceptor postProcessAdd(String requestId, String compId) method.
     *
     * @param componentChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    Method initComponentChangeInterceptorPostProcessAddMethod()
            throws CodeCenterImportException {

        if (componentChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) componentChangeInterceptorObject.getClass();

        // Get the postProcessAdd(String compId) method on the custom comp change interceptor class
        Class<?>[] argTypes = { String.class, String.class };
        Method componentChangeInterceptorPostProcessAddMethod = null;
        try {
            componentChangeInterceptorPostProcessAddMethod = sourceClass.getDeclaredMethod("postProcessAdd",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get comp change interceptor postProcessAdd(String requestId, String compId) method: No such method exception: "
                    + sourceClass.getName();
            throw new CodeCenterImportException(msg);
        }

        return componentChangeInterceptorPostProcessAddMethod;
    }

    /**
     * Get the Component Change Interceptor preProcessDelete(String requestId, String compId) method.
     *
     * @param componentChangeInterceptorObject
     * @return
     * @throws CodeCenterImportException
     */
    Method initComponentChangeInterceptorPreProcessDeleteMethod()
            throws CodeCenterImportException {

        if (componentChangeInterceptorObject == null) {
            log.warn("Component Change Interceptor object is null");
            return null;
        }

        Class<CompChangeInterceptor> sourceClass = (Class<CompChangeInterceptor>) componentChangeInterceptorObject.getClass();

        // Get the preProcessDelete(String compId) method on the custom comp change interceptor class
        Class<?>[] argTypes = { String.class, String.class };
        Method componentChangeInterceptorPreProcessDeleteMethod = null;
        try {
            componentChangeInterceptorPreProcessDeleteMethod = sourceClass.getDeclaredMethod("preProcessDelete",
                    argTypes);
        } catch (NoSuchMethodException e) {
            String msg = "Unable to get comp change interceptor preProcessDelete(String requestId, String compId) method: No such method exception: "
                    + sourceClass.getName();
            throw new CodeCenterImportException(msg);
        }

        return componentChangeInterceptorPreProcessDeleteMethod;
    }

    /**
     * Invoke the Component Change Intercepter init() method.
     *
     * @param cciApp
     * @param project
     * @throws CodeCenterImportException
     */
    public void invokeComponentChangeIntercepterInitMethod()
            throws CodeCenterImportException {
        if ((componentChangeInterceptorObject != null) && (componentChangeInterceptorInitMethod != null)) {
            try {
                componentChangeInterceptorInitMethod.invoke(appAdjusterObject);
            } catch (InvocationTargetException e) {
                String msg = "Error invoking componentChangeInterceptorInitMethod: InvocationTargetException: "
                        + e.getTargetException().getMessage();
                throw new CodeCenterImportException(msg);
            } catch (IllegalAccessException e) {
                String msg = "Error invoking componentChangeInterceptorInitMethod: IllegalAccessException: "
                        + e.getMessage();
                throw new CodeCenterImportException(msg);
            }
        } else {
            log.info("No componentChangeInterceptor configured");
        }
    }

    public void invokeComponentChangeIntercepterInitForAppMethod(String appId)
            throws CodeCenterImportException {
        if ((componentChangeInterceptorObject != null) && (componentChangeInterceptorInitForAppMethod != null)) {
            try {
                componentChangeInterceptorInitForAppMethod.invoke(appAdjusterObject, appId);
            } catch (InvocationTargetException e) {
                String msg = "Error invoking componentChangeInterceptor InitForApp Method: InvocationTargetException: "
                        + e.getTargetException().getMessage();
                throw new CodeCenterImportException(msg);
            } catch (IllegalAccessException e) {
                String msg = "Error invoking componentChangeInterceptor InitForApp Method: IllegalAccessException: "
                        + e.getMessage();
                throw new CodeCenterImportException(msg);
            }
        } else {
            log.info("No componentChangeInterceptor configured");
        }
    }

    public void invokeComponentChangeIntercepterPreProcessAddMethod(String compId)
            throws CodeCenterImportException {
        if ((componentChangeInterceptorObject != null) && (componentChangeInterceptorPreProcessAddMethod != null)) {
            try {
                componentChangeInterceptorPreProcessAddMethod.invoke(appAdjusterObject, compId);
            } catch (InvocationTargetException e) {
                String msg = "Error invoking componentChangeInterceptor PreProcessAdd Method: InvocationTargetException: "
                        + e.getTargetException().getMessage();
                throw new CodeCenterImportException(msg);
            } catch (IllegalAccessException e) {
                String msg = "Error invoking componentChangeInterceptor PreProcessAdd Method: IllegalAccessException: "
                        + e.getMessage();
                throw new CodeCenterImportException(msg);
            }
        } else {
            log.info("No componentChangeInterceptor configured");
        }
    }

    public void invokeComponentChangeIntercepterPostProcessAddMethod(String requestId, String compId)
            throws CodeCenterImportException {
        if ((componentChangeInterceptorObject != null) && (componentChangeInterceptorPostProcessAddMethod != null)) {
            try {
                componentChangeInterceptorPostProcessAddMethod.invoke(appAdjusterObject, requestId, compId);
            } catch (InvocationTargetException e) {
                String msg = "Error invoking componentChangeInterceptor PostProcessAdd Method: InvocationTargetException: "
                        + e.getTargetException().getMessage();
                throw new CodeCenterImportException(msg);
            } catch (IllegalAccessException e) {
                String msg = "Error invoking componentChangeInterceptor PostProcessAdd Method: IllegalAccessException: "
                        + e.getMessage();
                throw new CodeCenterImportException(msg);
            }
        } else {
            log.info("No componentChangeInterceptor configured");
        }
    }

    public void invokeComponentChangeIntercepterPreProcessDeleteMethod(String deleteRequestId, String compId)
            throws CodeCenterImportException {
        if ((componentChangeInterceptorObject != null) && (componentChangeInterceptorPreProcessDeleteMethod != null)) {
            try {
                componentChangeInterceptorPreProcessDeleteMethod.invoke(appAdjusterObject, deleteRequestId, compId);
            } catch (InvocationTargetException e) {
                String msg = "Error invoking componentChangeInterceptor PreProcessDelete Method: InvocationTargetException: "
                        + e.getTargetException().getMessage();
                throw new CodeCenterImportException(msg);
            } catch (IllegalAccessException e) {
                String msg = "Error invoking componentChangeInterceptor PreProcessDelete Method: IllegalAccessException: "
                        + e.getMessage();
                throw new CodeCenterImportException(msg);
            }
        } else {
            log.info("No componentChangeInterceptor configured");
        }
    }

    public Object getAppAdjusterObject() {
        return appAdjusterObject;
    }

}
