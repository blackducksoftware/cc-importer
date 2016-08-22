package com.blackducksoftware.tools.ccimport.mocks;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import com.blackducksoftware.tools.ccimport.interceptor.UpdateAttributeValuesOperation;
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
	private final int compIndex = 0;

	private final List<AttachmentDetails> attachments = new ArrayList<>();

	private final List<String> callsMadeDeleteAttachment = new ArrayList<>();

	private final List<String> callsMadeAttachFile = new ArrayList<>();

	private List<UpdateAttributeValuesOperation> updateAttributeValuesOperations = new ArrayList<>();

	public static final String COMPONENT_ID_PREFIX = "testCompId";

	public MockCodeCenterComponentManager() {
	}

	/**
	 * Call this when setting up a test to create a scenario where an attachment
	 * already exists.
	 *
	 * @param filename
	 */
	public void addAttachment(final String filename) {
		final AttachmentDetails att = new AttachmentDetails(
				filename, filename, "Mock "
						+ filename, new Date(), "test user", "text", 100L);
		attachments.add(att);

	}

	@Override
	public <T extends CodeCenterComponentPojo> T getComponentById(
			final Class<T> pojoClass, final String componentId, final String requestedLicenseId)
					throws CommonFrameworkException {
		final T comp = createComponentPojo(pojoClass, COMPONENT_ID_PREFIX
				+ compIndex);
		return comp;
	}

	@Override
	public <T extends CodeCenterComponentPojo> List<T> getComponents(
			final Class<T> pojoClass, final int firstRowIndex, final int lastRowIndex)
					throws CommonFrameworkException {
		final T comp = createComponentPojo(pojoClass, COMPONENT_ID_PREFIX
				+ compIndex);
		final List<T> comps = new ArrayList<>(1);
		comps.add(comp);
		return comps;
	}

	@Override
	public <T extends CodeCenterComponentPojo> T getComponentByNameVersion(
			final Class<T> pojoClass, final String componentName, final String componentVersion)
					throws CommonFrameworkException {
		final T comp = createComponentPojo(pojoClass, COMPONENT_ID_PREFIX
				+ compIndex);
		return comp;
	}

	@Override
	public <T extends CodeCenterComponentPojo> List<T> getComponentsForRequests(
			final Class<T> pojoClass, final List<RequestPojo> requests)
					throws CommonFrameworkException {
		final List<T> comps = new ArrayList<>(requests.size());
		for (final RequestPojo request : requests) {
			final T comp = createComponentPojo(pojoClass, request.getComponentId());
			comps.add(comp);
		}
		return comps;
	}

	private <T extends CodeCenterComponentPojo> T createComponentPojo(
			final Class<T> pojoClass, final String id) throws CommonFrameworkException {
		final List<AttributeValuePojo> attributeValues = new ArrayList<>();
		AttributeValuePojo attrValue = new AttributeValuePojo("testAttrId1",
				"Customer Protex Server ID", "MengerHttps");
		attributeValues.add(attrValue);
		attrValue = new AttributeValuePojo("testAttrId2",
				"Customer Protex Project ID", "Customerproject1");
		attributeValues.add(attrValue);
		attrValue = new AttributeValuePojo("testAttrId3",
				"Customer Component Copyright Statement",
				"Mock Customer Component Copyright Statement");
		attributeValues.add(attrValue);
		attrValue = new AttributeValuePojo("testAttrId4",
				"Customer Custom Acknowledgements",
				"Mock Customer Custom Acknowledgements");
		attributeValues.add(attrValue);
		attrValue = new AttributeValuePojo("testAttrId5",
				"Customer Custom Component License Text",
				"Mock Customer Custom Component License Text");
		attributeValues.add(attrValue);
		attrValue = new AttributeValuePojo("testAttrId6", "Customer CERT ID",
				"Mock Customer CERT ID");
		attributeValues.add(attrValue);

		final List<LicensePojo> licenses = new ArrayList<>(2);
		final LicensePojo license = new LicensePojo("testLicenseId",
				"Apache License 2.0", "Mock Apache 2.0 license text");
		licenses.add(license);

		T comp;
		try {
			comp = instantiatePojo(pojoClass);
		} catch (final Exception e) {
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
		if ("addedCompId".equals(id)) {
			comp.setAttributeValues(new ArrayList<AttributeValuePojo>());
		} else {
			comp.setAttributeValues(attributeValues);
		}
		comp.setLicenses(licenses);
		comp.setSubComponents(null);
		return comp;
	}

	@Override
	public <T extends CodeCenterComponentPojo> List<T> getComponentsForRequests(
			final Class<T> pojoClass, final List<RequestPojo> requests,
			final List<ApprovalStatus> limitToApprovalStatusValues)
					throws CommonFrameworkException {
		return getComponentsForRequests(pojoClass, requests);
	}

	@Override
	public List<AttachmentDetails> searchAttachments(final String componentId,
			final String searchString) throws CommonFrameworkException {
		return new ArrayList(attachments);
	}

	/**
	 * Adds an attachment of specified file, the name of file derived from path
	 */
	@Override
	public void attachFile(final String componentId, final String sourceFilePath,
			final String description) throws CommonFrameworkException {

		final File f = new File(sourceFilePath);
		final String filename = sourceFilePath;
		final AttachmentDetails att = new AttachmentDetails(
				f.getName(), filename, f.getName(),
				new Date(), "test user", "text", 100L);

		attachments.add(att);
		callsMadeAttachFile.add(filename);
		System.out.println(("Attachment added: " + f.getName()));
	}

	@Override
	public void deleteAttachment(final String componentId, final String fileName)
			throws CommonFrameworkException {
		callsMadeDeleteAttachment.add(fileName);

		System.out.println(("Removing attachment: " + fileName));
		final AttachmentDetails att = new AttachmentDetails(
				fileName, fileName, fileName,
				new Date(), "test user", "text", 100L);
		if (attachments.contains(att)) {
			System.out.println("Found attachment to remove");
			attachments.remove(att);
		}

	}

	public boolean wasCalledAddAttachment(final String filename) {
		if (callsMadeAttachFile.contains(filename)) {
			return true;
		} else {
			return false;
		}
	}

	public boolean wasCalledDeleteAttachment(final String filename) {
		if (callsMadeDeleteAttachment.contains(filename)) {
			return true;
		} else {
			return false;
		}
	}

	@Override
	public File downloadAttachment(final String componentId, final String sourceFileName,
			final String targetDirPath) throws CommonFrameworkException {

		final File target = new File(targetDirPath + File.separator + sourceFileName);
		try {
			File attachedFile = null;
			for (final AttachmentDetails att : attachments) {
				attachedFile = new File(att.getFileName());
				final String fileName = attachedFile.getName().toLowerCase();
				if (fileName.contains(sourceFileName.toLowerCase())) {
					Files.copy(attachedFile, target);

					System.out.println("Downloaded  file: " + target);
				}
			}
			return target;
		} catch (final IOException e) {
			System.err.println("Unable to download" + e.getMessage());
			return null;
		}

	}

	@Override
	public <T extends CodeCenterComponentPojo> T instantiatePojo(
			final Class<T> pojoClass) throws CommonFrameworkException {
		T componentPojo = null;
		Constructor<?> constructor = null;
		;
		try {
			constructor = pojoClass.getConstructor();
		} catch (final SecurityException e) {
			throw new CommonFrameworkException(e.getMessage());
		} catch (final NoSuchMethodException e) {
			throw new CommonFrameworkException(e.getMessage());
		}

		try {
			componentPojo = (T) constructor.newInstance();
		} catch (final IllegalArgumentException e) {
			throw new CommonFrameworkException(e.getMessage());
		} catch (final InstantiationException e) {
			throw new CommonFrameworkException(e.getMessage());
		} catch (final IllegalAccessException e) {
			throw new CommonFrameworkException(e.getMessage());
		} catch (final InvocationTargetException e) {
			throw new CommonFrameworkException(e.getMessage());
		}

		return componentPojo;
	}

	@Override
	public <T extends CodeCenterComponentPojo> T getComponentById(final Class<T> pojoClass, final String componentId) throws CommonFrameworkException {
		final T comp = createComponentPojo(pojoClass, componentId);
		return comp;
	}

	@Override
	public <T extends CodeCenterComponentPojo> void updateAttributeValues(final Class<T> pojoClass, final String compId, final Set<AttributeValuePojo> changedAttrValues)
			throws CommonFrameworkException {
		final UpdateAttributeValuesOperation op = new UpdateAttributeValuesOperation(compId, changedAttrValues);
		updateAttributeValuesOperations.add(op);

	}

	public List<UpdateAttributeValuesOperation> getUpdateAttributeValuesOperations() {
		return updateAttributeValuesOperations;
	}

	public void clearUpdateAttributeValuesOperations() {
		updateAttributeValuesOperations = new ArrayList<>();
	}

	@Override
	public void populateComponentCacheFromCatalog(final int batchSize) throws CommonFrameworkException {
		// TODO Auto-generated method stub

	}

	@Override
	public long resetComponentCache(final int sizeLimit, final int expireTime, final TimeUnit expireTimeUnits) {
		// TODO Auto-generated method stub
		return 0;
	}

}
