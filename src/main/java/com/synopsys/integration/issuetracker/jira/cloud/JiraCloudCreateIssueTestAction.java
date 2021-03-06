/**
 * issuetracker-jira
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.issuetracker.jira.cloud;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.issuetracker.common.config.IssueTrackerContext;
import com.synopsys.integration.issuetracker.common.service.IssueCreatorTestAction;
import com.synopsys.integration.issuetracker.common.service.IssueTrackerService;
import com.synopsys.integration.issuetracker.common.service.TestIssueRequestCreator;
import com.synopsys.integration.issuetracker.common.service.TransitionValidator;
import com.synopsys.integration.issuetracker.jira.cloud.util.JiraCloudTransitionHandler;
import com.synopsys.integration.issuetracker.jira.common.util.JiraTransitionHandler;
import com.synopsys.integration.jira.common.cloud.service.IssueService;
import com.synopsys.integration.jira.common.cloud.service.JiraCloudServiceFactory;
import com.synopsys.integration.jira.common.model.components.TransitionComponent;

public class JiraCloudCreateIssueTestAction extends IssueCreatorTestAction {
    private final Logger logger = LoggerFactory.getLogger(JiraCloudCreateIssueTestAction.class);
    private Gson gson;

    public JiraCloudCreateIssueTestAction(IssueTrackerService issueTrackerService, Gson gson, TestIssueRequestCreator testIssueRequestCreator) {
        super(issueTrackerService, testIssueRequestCreator);
        this.gson = gson;
    }

    @Override
    protected String getOpenTransitionFieldKey() {
        return JiraCloudProperties.KEY_OPEN_WORKFLOW_TRANSITION;
    }

    @Override
    protected String getResolveTransitionFieldKey() {
        return JiraCloudProperties.KEY_RESOLVE_WORKFLOW_TRANSITION;
    }

    @Override
    protected String getTodoStatusFieldKey() {
        return JiraTransitionHandler.TODO_STATUS_CATEGORY_KEY;
    }

    @Override
    protected String getDoneStatusFieldKey() {
        return JiraTransitionHandler.DONE_STATUS_CATEGORY_KEY;
    }

    @Override
    protected TransitionValidator<TransitionComponent> createTransitionValidator(IssueTrackerContext context) throws IntegrationException {
        JiraCloudProperties jiraProperties = createJiraProperties(context);
        JiraCloudServiceFactory jiraCloudServiceFactory = jiraProperties.createJiraServicesCloudFactory(logger, gson);
        IssueService issueService = jiraCloudServiceFactory.createIssueService();
        return new JiraCloudTransitionHandler(issueService);
    }

    @Override
    protected void safelyCleanUpIssue(IssueTrackerContext context, String issueKey) {
        try {
            JiraCloudProperties jiraProperties = createJiraProperties(context);
            JiraCloudServiceFactory jiraCloudServiceFactory = jiraProperties.createJiraServicesCloudFactory(logger, gson);
            IssueService issueService = jiraCloudServiceFactory.createIssueService();
            issueService.deleteIssue(issueKey);
        } catch (IntegrationException e) {
            logger.warn(String.format("There was a problem trying to delete the Jira Cloud distribution test issue, '%s': %s", issueKey, e.getMessage()));
            logger.debug(e.getMessage(), e);
        }
    }

    private JiraCloudProperties createJiraProperties(IssueTrackerContext context) {
        return (JiraCloudProperties) context.getIssueTrackerConfig();
    }
}
