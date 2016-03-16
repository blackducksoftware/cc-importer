package com.blackducksoftware.tools.ccimport.mocks;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

import com.blackducksoftware.tools.commonframework.core.exception.CommonFrameworkException;
import com.blackducksoftware.tools.commonframework.standard.protex.ProtexProjectPojo;
import com.blackducksoftware.tools.connector.codecenter.protexservers.IProtexServerManager;
import com.blackducksoftware.tools.connector.codecenter.protexservers.NamedProtexServer;
import com.blackducksoftware.tools.connector.protex.IProtexServerWrapper;

public class MockProtexServerManager implements IProtexServerManager {
    private final Logger log = Logger.getLogger(this.getClass());

    public static String JUNK_APPEND_VALUE = ":junkValue";

    private final Map<String, NamedProtexServer> protexServerCache = new HashMap<>();

    private int protexServerGetCount = 0;

    private int validateCount = 0;

    @Override
    public void validateServers(boolean cacheFailed) throws CommonFrameworkException {
        log.info("Mocking validation of protex servers");

        for (NamedProtexServer namedProtex : protexServerCache.values())
        {
            connectToServer(namedProtex);
        }

        validateCount++;
    }

    /**
     * Fake connection here, but can throw an Exception to emulate a failed connection
     * 
     * @param namedProtex
     * @throws CommonFrameworkException
     *             - If URL contains junk value
     */
    private void connectToServer(NamedProtexServer namedProtex) throws CommonFrameworkException
    {
        String url = namedProtex.getUrl();
        if (url.contains(JUNK_APPEND_VALUE))
        {
            throw new CommonFrameworkException("Unable to connect to url: " + url);
        }
    }

    @Override
    public IProtexServerWrapper<ProtexProjectPojo> getProtexServerWrapper(
            String serverName) throws CommonFrameworkException {
        protexServerGetCount++;

        log.info("Mocking connection to Protex server " + serverName);
        IProtexServerWrapper<ProtexProjectPojo> psw = new MockProtexServerWrapper();
        return psw;
    }

    public int getProtexServerGetCount() {
        return protexServerGetCount;
    }

    public void setProtexServerGetCount(int protexServerGetCount) {
        this.protexServerGetCount = protexServerGetCount;
    }

    public int getValidateCount() {
        return validateCount;
    }

    public void setValidateCount(int validateCount) {
        this.validateCount = validateCount;
    }

    @Override
    public List<String> getAllProtexNames() throws CommonFrameworkException {
        List<String> map = new ArrayList<String>();

        for (String key : protexServerCache.keySet())
        {
            map.add(key);
        }

        return map;
    }

    @Override
    public NamedProtexServer getNamedProtexServer(String key) throws CommonFrameworkException {
        return protexServerCache.get(key);
    }

    @Override
    public void setNamedProtexServer(NamedProtexServer server, String key) throws CommonFrameworkException {
        connectToServer(server);
        protexServerCache.put(key, server);
    }

}
