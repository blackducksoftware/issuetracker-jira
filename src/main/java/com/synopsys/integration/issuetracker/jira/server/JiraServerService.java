/**
 * issuetracker-jira
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.issuetracker.jira.server;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.issuetracker.common.config.IssueConfig;
import com.synopsys.integration.issuetracker.common.exception.IssueTrackerException;
import com.synopsys.integration.issuetracker.common.message.IssueTrackerRequest;
import com.synopsys.integration.issuetracker.common.message.IssueTrackerResponse;
import com.synopsys.integration.issuetracker.common.service.IssueTrackerService;
import com.synopsys.integration.issuetracker.jira.common.JiraConstants;
import com.synopsys.integration.issuetracker.jira.common.util.JiraContentValidator;
import com.synopsys.integration.issuetracker.jira.server.util.JiraServerIssueHandler;
import com.synopsys.integration.issuetracker.jira.server.util.JiraServerIssuePropertyHandler;
import com.synopsys.integration.issuetracker.jira.server.util.JiraServerTransitionHandler;
import com.synopsys.integration.jira.common.rest.service.IssueMetaDataService;
import com.synopsys.integration.jira.common.rest.service.IssuePropertyService;
import com.synopsys.integration.jira.common.rest.service.IssueTypeService;
import com.synopsys.integration.jira.common.rest.service.PluginManagerService;
import com.synopsys.integration.jira.common.server.service.IssueSearchService;
import com.synopsys.integration.jira.common.server.service.IssueService;
import com.synopsys.integration.jira.common.server.service.JiraServerServiceFactory;
import com.synopsys.integration.jira.common.server.service.ProjectService;
import com.synopsys.integration.jira.common.server.service.UserSearchService;

public class JiraServerService extends IssueTrackerService<JiraServerContext> {
    private Logger logger = LoggerFactory.getLogger(JiraServerService.class);

    public JiraServerService(Gson gson) {
        super(gson);
    }

    @Override
    public IssueTrackerResponse sendRequests(JiraServerContext context, List<IssueTrackerRequest> requests) throws IntegrationException {
        if (null == context) {
            throw new IssueTrackerException("Context missing. Cannot determine Jira Server instance.");
        }
        if (null == requests || requests.isEmpty()) {
            throw new IssueTrackerException("Requests missing. Require at least one request.");
        }
        JiraServerProperties jiraProperties = context.getIssueTrackerConfig();
        JiraServerServiceFactory jiraServerServiceFactory = jiraProperties.createJiraServicesServerFactory(logger, getGson());
        PluginManagerService jiraAppService = jiraServerServiceFactory.createPluginManagerService();
        logger.debug("Verifying the required application is installed on the Jira server...");
        boolean missingApp = !jiraAppService.getInstalledApp(jiraProperties.getUsername(), jiraProperties.getPassword(), JiraConstants.JIRA_APP_KEY).isPresent();
        if (missingApp) {
            throw new IssueTrackerException("Please configure the Jira Server plugin for your server instance via the global Jira Server channel settings.");
        }

        ProjectService projectService = jiraServerServiceFactory.createProjectService();
        UserSearchService userSearchService = jiraServerServiceFactory.createUserSearchService();
        IssueTypeService issueTypeService = jiraServerServiceFactory.createIssueTypeService();
        IssueMetaDataService issueMetaDataService = jiraServerServiceFactory.createIssueMetadataService();

        JiraServerIssueConfigValidator jiraIssueConfigValidator = new JiraServerIssueConfigValidator(projectService, userSearchService, issueTypeService, issueMetaDataService);
        IssueConfig validIssueConfig = jiraIssueConfigValidator.createValidIssueConfig(context);

        IssueService issueService = jiraServerServiceFactory.createIssueService();
        IssuePropertyService issuePropertyService = jiraServerServiceFactory.createIssuePropertyService();
        IssueSearchService issueSearchService = jiraServerServiceFactory.createIssueSearchService();
        JiraContentValidator jiraContentValidator = new JiraContentValidator();
        JiraServerTransitionHandler jiraTransitionHandler = new JiraServerTransitionHandler(issueService);
        JiraServerIssuePropertyHandler jiraIssuePropertyHandler = new JiraServerIssuePropertyHandler(issueSearchService, issuePropertyService);
        JiraServerIssueHandler jiraIssueHandler = new JiraServerIssueHandler(issueService, jiraProperties, getGson(), jiraTransitionHandler, jiraIssuePropertyHandler, jiraContentValidator);
        return jiraIssueHandler.createOrUpdateIssues(validIssueConfig, requests);
    }
}
