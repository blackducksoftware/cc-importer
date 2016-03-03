package com.blackducksoftware.tools.ccimport.mocks;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.common.ProjectPojo;
import com.blackducksoftware.tools.connector.common.ApprovalStatus;
import com.blackducksoftware.tools.connector.common.ILicenseManager;
import com.blackducksoftware.tools.connector.protex.common.ComponentNameVersionIds;
import com.blackducksoftware.tools.connector.protex.common.ProtexComponentPojo;
import com.blackducksoftware.tools.connector.protex.common.ProtexComponentType;
import com.blackducksoftware.tools.connector.protex.component.IProtexComponentManager;
import com.blackducksoftware.tools.connector.protex.license.ProtexLicensePojo;

public class MockProtexComponentManager implements IProtexComponentManager {
    private final ILicenseManager<ProtexLicensePojo> licMgr;

    public MockProtexComponentManager(ILicenseManager<ProtexLicensePojo> licMgr) {
        this.licMgr = licMgr;
    }

    @Override
    public <T extends ProtexComponentPojo> T getComponentByNameVersionIds(
            Class<T> pojoClass, ComponentNameVersionIds nameVersionIds)
            throws CommonFrameworkException {
        return createMockComponent(pojoClass);
    }

    @Override
    public <T extends ProtexComponentPojo> T getComponentByNameVersion(
            Class<T> pojoClass, String componentName, String componentVersion)
            throws CommonFrameworkException {
        return createMockComponent(pojoClass);
    }

    @Override
    public <T extends ProtexComponentPojo> List<T> getComponentsByNameVersionIds(
            Class<T> pojoClass, List<ComponentNameVersionIds> nameVersionIdsList)
            throws CommonFrameworkException {

        List<T> comps = new ArrayList<>(1);
        T comp = createMockComponent(pojoClass);
        comps.add(comp);
        return comps;
    }

    private <T extends ProtexComponentPojo> T createMockComponent(
            Class<T> pojoClass) throws CommonFrameworkException {
        ComponentNameVersionIds nameVersionIds = new ComponentNameVersionIds(
                "jqueryjs527713", "3038508");
        List<ProtexLicensePojo> licenses = new ArrayList<>();
        ProtexLicensePojo license = licMgr.getLicenseById("mit2");
        T comp = instantiatePojo(pojoClass);
        comp.setName("jQuery JavaScript Library");
        comp.setVersion("1.9.1");
        comp.setApprovalStatus(ApprovalStatus.APPROVED);
        comp.setHomepage("http://jquery.com/");
        comp.setDeprecated(false);
        comp.setNameVersionIds(nameVersionIds);
        comp.setLicenses(licenses);
        comp.setType(ProtexComponentType.STANDARD);
        comp.setDescription("");
        comp.setPrimaryLicenseId(license.getId());
        comp.setPrimaryLicenseName(license.getName());

        return comp;
    }

    @Override
    public <T extends ProtexComponentPojo> T instantiatePojo(Class<T> pojoClass)
            throws CommonFrameworkException {
        T componentPojo = null;
        Constructor<?> constructor = null;
        ;
        try {
            constructor = pojoClass.getConstructor();
        } catch (SecurityException e) {
            throw new CommonFrameworkException(e.getMessage());
        } catch (NoSuchMethodException e) {
            throw new CommonFrameworkException(e.getMessage());
        }

        try {
            componentPojo = (T) constructor.newInstance();
        } catch (IllegalArgumentException e) {
            throw new CommonFrameworkException(e.getMessage());
        } catch (InstantiationException e) {
            throw new CommonFrameworkException(e.getMessage());
        } catch (IllegalAccessException e) {
            throw new CommonFrameworkException(e.getMessage());
        } catch (InvocationTargetException e) {
            throw new CommonFrameworkException(e.getMessage());
        }

        return componentPojo;
    }

}
