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
    private Map<String, Integer> numberOfUsersByAppId;

    private static final List<ApplicationPojo> apps = new ArrayList<>();

    public MockApplicationManager(boolean returnSomeApps) {
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
    public List<ApplicationPojo> getApplications(int firstRow, int lastRow, String searchString) throws CommonFrameworkException {

        List<ApplicationPojo> apps = generateApps(searchString);
        return apps;
    }

    private List<ApplicationPojo> generateApps(String prefix) {
        List<ApplicationPojo> apps = new ArrayList<>(4);
        if (!returnSomeApps) {
            return apps; // return none
        }

        for (int i = 0; i < 4; i++) {
            String appName = prefix + "App" + i + "-PROD-CURRENT";
            ApplicationPojo app = new ApplicationPojo(appName, appName, "v100",
                    null,
                    ApprovalStatus.APPROVED, false, "testOwnerId");
            System.out.println("Mocking app: " + app);
            apps.add(app);
        }
        return apps;
    }

    @Override
    public List<ApplicationPojo> getApplications(int firstRow, int lastRow) throws CommonFrameworkException {
        List<ApplicationPojo> apps = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            apps.addAll(generateApps("" + i + "00000-"));
        }
        return apps;
    }

    @Override
    public ApplicationPojo getApplicationByNameVersion(String name, String version) throws CommonFrameworkException {
        ApplicationPojo app = new ApplicationPojo(name, name, version,
                null,
                ApprovalStatus.APPROVED, false, "u000000");
        return app;
    }

    @Override
    public ApplicationPojo getApplicationById(String id) throws CommonFrameworkException {
        // TODO Auto-generated function stub
        return new ApplicationPojo(id, id, "Unspecified",
                null,
                ApprovalStatus.APPROVED, false, "testOwnerId");
    }

    @Override
    public List<RequestPojo> getRequestsByAppId(String appId) throws CommonFrameworkException {
        // TODO Auto-generated function stub
        return null;
    }

    @Override
    public <T extends CodeCenterComponentPojo> List<T> getComponentsByAppId(Class<T> pojoClass, String appId, List<ApprovalStatus> limitToApprovalStatusValues,
            boolean recursive) throws CommonFrameworkException {
        // TODO Auto-generated function stub
        return null;
    }

    @Override
    public List<ApplicationUserPojo> getAllUsersAssignedToApplication(String appId) throws CommonFrameworkException {
        List<ApplicationUserPojo> roles = new ArrayList<>();

        int numberOfUsersToReturn;

        if (numberOfUsersByAppId.containsKey(appId)) {
            numberOfUsersToReturn = numberOfUsersByAppId.get(appId);
        } else {
            numberOfUsersToReturn = 1;
        }
        for (int i = 0; i < numberOfUsersToReturn; i++) {
            String username = "u00000" + i;
            ApplicationUserPojo role = new ApplicationUserPojo("testAppName", "Unspecified", appId,
                    username, username, "Application Developer", "appDev");
            roles.add(role);
        }

        System.out.println("getAllUsersAssignedToApplication(" + appId + "): " + roles.size());
        return roles;
    }

    @Override
    public void addUsersByIdToApplicationTeam(String appId, Set<String> userIds, Set<String> roleNames, boolean circumventLock) throws CommonFrameworkException {
        // TODO Auto-generated function stub

    }

    @Override
    public void addUsersByNameToApplicationTeam(String appId, Set<String> userNames, Set<String> roleNames, boolean circumventLock)
            throws CommonFrameworkException {
        System.out.println("addUsersByNameToApplicationTeam() called: " + appId + ": " + userNames);
        recordOperation("add", appId, userNames);
    }

    private synchronized void recordOperation(String operationName, String appId, Set<String> userNames) {
        String operation = operationName + ": " + appId + ": " + asSortedList(userNames);
        operations.add(operation);
    }

    private static <T extends Comparable<? super T>> List<T> asSortedList(Collection<T> c) {
        List<T> list = new ArrayList<T>(c);
        java.util.Collections.sort(list);
        return list;
    }

    @Override
    public void removeUserByIdFromApplicationTeam(String appId, String userId, String roleId, boolean circumventLock) throws CommonFrameworkException {
        // TODO Auto-generated function stub

    }

    @Override
    public List<UserStatus> removeUsersByNameFromApplicationAllRoles(String appId, Set<String> usernames, boolean circumventLock)
            throws CommonFrameworkException {
        recordOperation("remove", appId, usernames);
        return null;
    }

    @Override
    public List<AttachmentDetails> searchAttachments(String applicationId, String searchString) throws CommonFrameworkException {
        // TODO Auto-generated function stub
        return null;
    }

    @Override
    public File downloadAttachment(String applicationId, String filename, String targetDirPath) throws CommonFrameworkException {
        // TODO Auto-generated function stub
        return null;
    }

    @Override
    public void attachFile(String applicationId, String sourceFilePath, String description) throws CommonFrameworkException {
        // TODO Auto-generated function stub

    }

    @Override
    public void deleteAttachment(String applicationId, String filename) throws CommonFrameworkException {
        // TODO Auto-generated function stub

    }

    @Override
    public void updateAttributeValues(String appId, Set<AttributeValuePojo> changedAttrValues) throws CommonFrameworkException {
        // TODO Auto-generated function stub

    }

}
