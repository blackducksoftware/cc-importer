package com.blackducksoftware.tools.ccimport.mocks;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.connector.codecenter.application.ApplicationPojo;
import com.blackducksoftware.tools.connector.codecenter.application.ApplicationUserPojo;
import com.blackducksoftware.tools.connector.codecenter.application.IApplicationManager;
import com.blackducksoftware.tools.connector.codecenter.common.AttachmentDetails;
import com.blackducksoftware.tools.connector.codecenter.common.AttributeValuePojo;
import com.blackducksoftware.tools.connector.codecenter.common.CodeCenterComponentPojo;
import com.blackducksoftware.tools.connector.codecenter.common.RequestPojo;
import com.blackducksoftware.tools.connector.codecenter.user.UserStatus;
import com.blackducksoftware.tools.connector.common.ApprovalStatus;

public class MockApplicationManager implements IApplicationManager {
	private final SortedSet<String> operations;

	private final boolean returnSomeApps;

	// private int numberOfUsersToReturn = 1;
	private final Map<String, Integer> numberOfUsersByAppId;

	private static final List<ApplicationPojo> apps = new ArrayList<>();

	public MockApplicationManager(final boolean returnSomeApps) {
		operations = new TreeSet<>();
		this.returnSomeApps = returnSomeApps;
		numberOfUsersByAppId = new HashMap<>();
		numberOfUsersByAppId.put("333333-PROD-CURRENT", 1);
		numberOfUsersByAppId.put("333333-test app-RC1-CURRENT", 1);
		numberOfUsersByAppId.put("444444-test app-PROD-CURRENT", 1);
		numberOfUsersByAppId.put("000000-App3-PROD-CURRENT", 7);
		numberOfUsersByAppId.put("100000-App2-PROD-CURRENT", 3);
		for (int i = 0; i < 4; i++) {
			numberOfUsersByAppId.put("333333-App" + i + "-PROD-CURRENT", i + 2);
		}
		for (int i = 0; i < 4; i++) {
			numberOfUsersByAppId.put("444444-App" + i + "-PROD-CURRENT", i + 2);
		}

		apps.add(new ApplicationPojo("app1", "app1", "Unspecified", null, ApprovalStatus.APPROVED, false, "ownerId"));
	}

	public synchronized SortedSet<String> getOperations() {
		return operations;
	}

	@Override
	public List<ApplicationPojo> getApplications(final int firstRow, final int lastRow, final String searchString) throws CommonFrameworkException {

		final List<ApplicationPojo> apps = generateApps(searchString);
		return apps;
	}

	private List<ApplicationPojo> generateApps(final String prefix) {
		final List<ApplicationPojo> apps = new ArrayList<>(4);
		if (!returnSomeApps) {
			return apps; // return none
		}

		for (int i = 0; i < 4; i++) {
			final String appName = prefix + "App" + i + "-PROD-CURRENT";
			final ApplicationPojo app = new ApplicationPojo(appName, appName, "v100",
					null,
					ApprovalStatus.APPROVED, false, "testOwnerId");
			System.out.println("Mocking app: " + app);
			apps.add(app);
		}
		return apps;
	}

	@Override
	public List<ApplicationPojo> getApplications(final int firstRow, final int lastRow) throws CommonFrameworkException {
		final List<ApplicationPojo> apps = new ArrayList<>();
		for (int i = 0; i < 10; i++) {
			apps.addAll(generateApps("" + i + "00000-"));
		}
		return apps;
	}

	@Override
	public ApplicationPojo getApplicationByNameVersion(final String name, final String version) throws CommonFrameworkException {
		final ApplicationPojo app = new ApplicationPojo(name, name, version,
				null,
				ApprovalStatus.APPROVED, false, "u000000");
		return app;
	}

	@Override
	public ApplicationPojo getApplicationById(final String id) throws CommonFrameworkException {
		// TODO Auto-generated function stub
		return new ApplicationPojo(id, id, "Unspecified",
				null,
				ApprovalStatus.APPROVED, false, "testOwnerId");
	}

	@Override
	public List<RequestPojo> getRequestsByAppId(final String appId) throws CommonFrameworkException {
		// TODO Auto-generated function stub
		return null;
	}

	@Override
	public <T extends CodeCenterComponentPojo> List<T> getComponentsByAppId(final Class<T> pojoClass, final String appId, final List<ApprovalStatus> limitToApprovalStatusValues,
			final boolean recursive) throws CommonFrameworkException {
		// TODO Auto-generated function stub
		return null;
	}

	@Override
	public List<ApplicationUserPojo> getAllUsersAssignedToApplication(final String appId) throws CommonFrameworkException {
		final List<ApplicationUserPojo> roles = new ArrayList<>();

		int numberOfUsersToReturn;

		if (numberOfUsersByAppId.containsKey(appId)) {
			numberOfUsersToReturn = numberOfUsersByAppId.get(appId);
		} else {
			numberOfUsersToReturn = 1;
		}
		for (int i = 0; i < numberOfUsersToReturn; i++) {
			final String username = "u00000" + i;
			final ApplicationUserPojo role = new ApplicationUserPojo("testAppName", "Unspecified", appId,
					username, username, "Application Developer", "appDev");
			roles.add(role);
		}

		System.out.println("getAllUsersAssignedToApplication(" + appId + "): " + roles.size());
		return roles;
	}

	@Override
	public void addUsersByIdToApplicationTeam(final String appId, final Set<String> userIds, final Set<String> roleNames, final boolean circumventLock) throws CommonFrameworkException {
		// TODO Auto-generated function stub

	}

	@Override
	public void addUsersByNameToApplicationTeam(final String appId, final Set<String> userNames, final Set<String> roleNames, final boolean circumventLock)
			throws CommonFrameworkException {
		System.out.println("addUsersByNameToApplicationTeam() called: " + appId + ": " + userNames);
		recordOperation("add", appId, userNames);
	}

	private synchronized void recordOperation(final String operationName, final String appId, final Set<String> userNames) {
		final String operation = operationName + ": " + appId + ": " + asSortedList(userNames);
		operations.add(operation);
	}

	private static <T extends Comparable<? super T>> List<T> asSortedList(final Collection<T> c) {
		final List<T> list = new ArrayList<T>(c);
		java.util.Collections.sort(list);
		return list;
	}

	@Override
	public void removeUserByIdFromApplicationTeam(final String appId, final String userId, final String roleId, final boolean circumventLock) throws CommonFrameworkException {
		// TODO Auto-generated function stub

	}

	@Override
	public List<UserStatus> removeUsersByNameFromApplicationAllRoles(final String appId, final Set<String> usernames, final boolean circumventLock)
			throws CommonFrameworkException {
		recordOperation("remove", appId, usernames);
		return null;
	}

	@Override
	public List<AttachmentDetails> searchAttachments(final String applicationId, final String searchString) throws CommonFrameworkException {
		// TODO Auto-generated function stub
		return null;
	}

	@Override
	public File downloadAttachment(final String applicationId, final String filename, final String targetDirPath) throws CommonFrameworkException {
		// TODO Auto-generated function stub
		return null;
	}

	@Override
	public void attachFile(final String applicationId, final String sourceFilePath, final String description) throws CommonFrameworkException {
		// TODO Auto-generated function stub

	}

	@Override
	public void deleteAttachment(final String applicationId, final String filename) throws CommonFrameworkException {
		// TODO Auto-generated function stub

	}

	@Override
	public void updateAttributeValues(final String appId, final Set<AttributeValuePojo> changedAttrValues) throws CommonFrameworkException {
		// TODO Auto-generated function stub

	}

	@Override
	public List<ApplicationPojo> getAllApplications() throws CommonFrameworkException {
		// TODO Auto-generated function stub
		return null;
	}

	@Override
	public List<ApplicationPojo> getAllApplications(final int chunkSize) throws CommonFrameworkException {
		// TODO Auto-generated function stub
		return null;
	}

	@Override
	public void removeApplicationFromCacheById(final String id) throws CommonFrameworkException {
		// TODO Auto-generated method stub

	}

	@Override
	public void removeApplicationFromCacheByNameVersion(final String name, final String version)
			throws CommonFrameworkException {
		// TODO Auto-generated method stub

	}

}
