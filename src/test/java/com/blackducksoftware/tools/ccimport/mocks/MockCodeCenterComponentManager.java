package com.blackducksoftware.tools.ccimport.mocks;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.codecenter.common.AttachmentDetails;
import com.blackducksoftware.tools.connector.codecenter.common.AttributeValuePojo;
import com.blackducksoftware.tools.connector.codecenter.common.CodeCenterComponentPojo;
import com.blackducksoftware.tools.connector.codecenter.common.RequestPojo;
import com.blackducksoftware.tools.connector.codecenter.component.ICodeCenterComponentManager;
import com.blackducksoftware.tools.connector.common.ApprovalStatus;
import com.blackducksoftware.tools.connector.common.LicensePojo;
import com.google.common.io.Files;

public class MockCodeCenterComponentManager implements
        ICodeCenterComponentManager {
    private int compIndex = 0;

    private List<AttachmentDetails> attachments = new ArrayList<>();

    private List<String> callsMadeDeleteAttachment = new ArrayList<>();

    private List<String> callsMadeAttachFile = new ArrayList<>();

    public static final String COMPONENT_ID_PREFIX = "testCompId";

    public MockCodeCenterComponentManager() {
    }

    /**
     * Call this when setting up a test to create a scenario where an attachment
     * already exists.
     *
     * @param filename
     */
    public void addAttachment(String filename) {
        AttachmentDetails att = new AttachmentDetails(
                filename, filename, "Mock "
                        + filename, new Date(), "test user", "text", 100L);
        attachments.add(att);

    }

    @Override
    public <T extends CodeCenterComponentPojo> T getComponentById(
            Class<T> pojoClass, String componentId, String requestedLicenseId)
            throws CommonFrameworkException {
        T comp = createComponentPojo(pojoClass, COMPONENT_ID_PREFIX
                + compIndex);
        return comp;
    }

    @Override
    public <T extends CodeCenterComponentPojo> List<T> getComponents(
            Class<T> pojoClass, int firstRowIndex, int lastRowIndex)
            throws CommonFrameworkException {
        T comp = createComponentPojo(pojoClass, COMPONENT_ID_PREFIX
                + compIndex);
        List<T> comps = new ArrayList<>(1);
        comps.add(comp);
        return comps;
    }

    @Override
    public <T extends CodeCenterComponentPojo> T getComponentByNameVersion(
            Class<T> pojoClass, String componentName, String componentVersion)
            throws CommonFrameworkException {
        T comp = createComponentPojo(pojoClass, COMPONENT_ID_PREFIX
                + compIndex);
        return comp;
    }

    @Override
    public <T extends CodeCenterComponentPojo> List<T> getComponentsForRequests(
            Class<T> pojoClass, List<RequestPojo> requests)
            throws CommonFrameworkException {
        List<T> comps = new ArrayList<>(requests.size());
        for (RequestPojo request : requests) {
            T comp = createComponentPojo(pojoClass, request.getComponentId());
            comps.add(comp);
        }
        return comps;
    }

    private <T extends CodeCenterComponentPojo> T createComponentPojo(
            Class<T> pojoClass, String id) throws CommonFrameworkException {
        List<AttributeValuePojo> attributeValues = new ArrayList<>();
        AttributeValuePojo attrValue = new AttributeValuePojo("testAttrId1",
                "Siemens Protex Server ID", "MengerHttps");
        attributeValues.add(attrValue);
        attrValue = new AttributeValuePojo("testAttrId2",
                "Siemens Protex Project ID", "siemensproject1");
        attributeValues.add(attrValue);
        attrValue = new AttributeValuePojo("testAttrId3",
                "Siemens Component Copyright Statement",
                "Mock Siemens Component Copyright Statement");
        attributeValues.add(attrValue);
        attrValue = new AttributeValuePojo("testAttrId4",
                "Siemens Custom Acknowledgements",
                "Mock Siemens Custom Acknowledgements");
        attributeValues.add(attrValue);
        attrValue = new AttributeValuePojo("testAttrId5",
                "Siemens Custom Component License Text",
                "Mock Siemens Custom Component License Text");
        attributeValues.add(attrValue);
        attrValue = new AttributeValuePojo("testAttrId5", "Siemens CERT ID",
                "Mock Siemens CERT ID");
        attributeValues.add(attrValue);

        List<LicensePojo> licenses = new ArrayList<>(2);
        LicensePojo license = new LicensePojo("testLicenseId",
                "Apache License 2.0", "Mock Apache 2.0 license text");
        licenses.add(license);

        T comp;
        try {
            comp = instantiatePojo(pojoClass);
        } catch (Exception e) {
            // TODO Auto-generated catch block
            throw new CommonFrameworkException(
                    "Error instantiating component POJO: " + e.getMessage());
        }
        comp.setId(id);
        comp.setName("Test Component " + id);
        comp.setVersion("TestVersion");
        comp.setApprovalStatus(ApprovalStatus.APPROVED);
        comp.setHomepage("www.blackducksoftware.com");
        comp.setIntendedAudiences("Test Intended Audiences");
        if ("addedCompId".equals(id)) {
            comp.setKbComponentId("replComp3");
            comp.setKbReleaseId("replComp3Version");
        } else {
            comp.setKbComponentId("deprComp3");
            comp.setKbReleaseId("deprComp3Version");
        }
        comp.setApplicationComponent(false);
        comp.setApplicationId(null);
        comp.setDeprecated(true);
        comp.setAttributeValues(attributeValues);
        comp.setLicenses(licenses);
        comp.setSubComponents(null);
        return comp;
    }

    @Override
    public <T extends CodeCenterComponentPojo> List<T> getComponentsForRequests(
            Class<T> pojoClass, List<RequestPojo> requests,
            List<ApprovalStatus> limitToApprovalStatusValues)
            throws CommonFrameworkException {
        return getComponentsForRequests(pojoClass, requests);
    }

    @Override
    public List<AttachmentDetails> searchAttachments(String componentId,
            String searchString) throws CommonFrameworkException {
        return new ArrayList(attachments);
    }

    /**
     * Adds an attachment of specified file, the name of file derived from path
     */
    @Override
    public void attachFile(String componentId, String sourceFilePath,
            String description) throws CommonFrameworkException {

        File f = new File(sourceFilePath);
        String filename = sourceFilePath;
        AttachmentDetails att = new AttachmentDetails(
                f.getName(), filename, f.getName(),
                new Date(), "test user", "text", 100L);

        attachments.add(att);
        callsMadeAttachFile.add(filename);
        System.out.println(("Attachment added: " + f.getName()));
    }

    @Override
    public void deleteAttachment(String componentId, String fileName)
            throws CommonFrameworkException {
        callsMadeDeleteAttachment.add(fileName);

        System.out.println(("Removing attachment: " + fileName));
        AttachmentDetails att = new AttachmentDetails(
                fileName, fileName, fileName,
                new Date(), "test user", "text", 100L);
        if (attachments.contains(att)) {
            System.out.println("Found attachment to remove");
            attachments.remove(att);
        }

    }

    public boolean wasCalledAddAttachment(String filename) {
        if (callsMadeAttachFile.contains(filename)) {
            return true;
        } else {
            return false;
        }
    }

    public boolean wasCalledDeleteAttachment(String filename) {
        if (callsMadeDeleteAttachment.contains(filename)) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public File downloadAttachment(String componentId, String sourceFileName,
            String targetDirPath) throws CommonFrameworkException {

        File target = new File(targetDirPath + File.separator + sourceFileName);
        try {
            File attachedFile = null;
            for (AttachmentDetails att : attachments) {
                attachedFile = new File(att.getFileName());
                String fileName = attachedFile.getName().toLowerCase();
                if (fileName.contains(sourceFileName.toLowerCase())) {
                    Files.copy(attachedFile, target);

                    System.out.println("Downloaded  file: " + target);
                }
            }
            return target;
        } catch (IOException e) {
            System.err.println("Unable to download" + e.getMessage());
            return null;
        }

    }

    @Override
    public <T extends CodeCenterComponentPojo> T instantiatePojo(
            Class<T> pojoClass) throws CommonFrameworkException {
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

    @Override
    public <T extends CodeCenterComponentPojo> T getComponentById(Class<T> pojoClass, String componentId) throws CommonFrameworkException {
        T comp = createComponentPojo(pojoClass, componentId);
        return comp;
    }

}
