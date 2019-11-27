package com.synopsys.integration.issuetracker.jira.cloud;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.integration.issuetracker.common.exception.IssueTrackerException;
import com.synopsys.integration.jira.common.cloud.configuration.JiraCloudRestConfig;
import com.synopsys.integration.jira.common.cloud.service.JiraCloudServiceFactory;

public class JiraCloudPropertiesTest {
    @Test
    public void testBuildConfigException() {
        try {
            JiraCloudProperties properties = new JiraCloudProperties(null, null, null);
            assertNull(properties.getUrl());
            assertNull(properties.getAccessToken());
            assertNull(properties.getUsername());
            properties.createJiraServerConfig();
            fail();
        } catch (IssueTrackerException ex) {
            ex.printStackTrace();
        }
    }

    @Test
    public void testBuildConfig() {
        try {
            final String url = "http://localhost:2990";
            final String token = "token";
            final String user = "user";
            JiraCloudProperties properties = new JiraCloudProperties(url, token, user);
            assertEquals(url, properties.getUrl());
            assertEquals(token, properties.getAccessToken());
            assertEquals(user, properties.getUsername());
            JiraCloudRestConfig config = properties.createJiraServerConfig();
            assertNotNull(config);
        } catch (IssueTrackerException ex) {
            ex.printStackTrace();
            fail();
        }
    }

    @Test
    public void testServerServiceFactory() {
        try {
            JiraCloudProperties properties = new JiraCloudProperties("http://localhost:2990", "token", "user");
            JiraCloudServiceFactory serviceFactory = properties.createJiraServicesCloudFactory(LoggerFactory.getLogger(getClass()), new Gson());
            assertNotNull(serviceFactory);
        } catch (IssueTrackerException ex) {
            ex.printStackTrace();
            fail();
        }
    }
}
