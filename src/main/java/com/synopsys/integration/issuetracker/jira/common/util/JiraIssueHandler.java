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
package com.synopsys.integration.issuetracker.jira.common.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.issuetracker.common.IssueOperation;
import com.synopsys.integration.issuetracker.common.config.IssueConfig;
import com.synopsys.integration.issuetracker.common.exception.IssueTrackerException;
import com.synopsys.integration.issuetracker.common.exception.IssueTrackerFieldException;
import com.synopsys.integration.issuetracker.common.message.IssueContentModel;
import com.synopsys.integration.issuetracker.common.message.IssueTrackerRequest;
import com.synopsys.integration.issuetracker.common.service.IssueHandler;
import com.synopsys.integration.issuetracker.jira.common.JiraIssueSearchProperties;
import com.synopsys.integration.jira.common.cloud.builder.IssueRequestModelFieldsBuilder;
import com.synopsys.integration.jira.common.model.request.builder.IssueRequestModelFieldsMapBuilder;
import com.synopsys.integration.jira.common.model.response.IssueResponseModel;
import com.synopsys.integration.rest.exception.IntegrationRestException;

public abstract class JiraIssueHandler extends IssueHandler<IssueResponseModel> {
    public static final String DESCRIPTION_CONTINUED_TEXT = "(description continued...)";
    public static final String DESCRIPTION_TRUNCATED_TEXT = "... (Comments are disabled.  Description data will be lost. See project information for more data.)";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Gson gson;
    private final JiraTransitionHandler jiraTransitionHelper;
    private final JiraIssuePropertyHandler jiraIssuePropertyHelper;

    public JiraIssueHandler(Gson gson, JiraTransitionHandler jiraTransitionHandler, JiraIssuePropertyHandler<?> jiraIssuePropertyHandler, JiraContentValidator contentValidator) {
        super(contentValidator);
        this.gson = gson;
        this.jiraTransitionHelper = jiraTransitionHandler;
        this.jiraIssuePropertyHelper = jiraIssuePropertyHandler;
    }

    public abstract IssueResponseModel createIssue(String issueCreator, String issueType, String projectName, IssueRequestModelFieldsMapBuilder fieldsBuilder) throws IntegrationException;

    public abstract String getIssueCreatorFieldKey();

    @Override
    protected Optional<IssueResponseModel> createIssue(IssueConfig issueConfig, IssueTrackerRequest request)
        throws IntegrationException {
        JiraIssueSearchProperties issueProperties = request.getIssueSearchProperties();
        IssueContentModel contentModel = request.getRequestContent();

        IssueContentModel issueContentModel = contentModel;
        if (!contentModel.getDescriptionComments().isEmpty() && !issueConfig.getCommentOnIssues()) {
            String description = createDescriptionText(contentModel.getDescription());
            issueContentModel = IssueContentModel.of(contentModel.getTitle(), description, Collections.emptyList());
        }

        IssueRequestModelFieldsBuilder fieldsBuilder = createFieldsBuilder(issueContentModel);
        fieldsBuilder.setProject(issueConfig.getProjectId());
        fieldsBuilder.setIssueType(issueConfig.getIssueType());
        String issueCreator = issueConfig.getIssueCreator();

        try {
            IssueResponseModel issue = createIssue(issueCreator, issueConfig.getIssueType(), issueConfig.getProjectName(), fieldsBuilder);
            logger.debug("Created new Jira Cloud issue: {}", issue.getKey());
            String issueKey = issue.getKey();
            addIssueProperties(issueKey, issueProperties);
            if (issueConfig.getCommentOnIssues()) {
                addComment(issueKey, "This issue was automatically created by Alert.");
                for (String additionalComment : issueContentModel.getDescriptionComments()) {
                    String comment = String.format("%s \n %s", DESCRIPTION_CONTINUED_TEXT, additionalComment);
                    addComment(issueKey, comment);
                }
            }
            return Optional.ofNullable(issue);
        } catch (IntegrationRestException e) {
            logger.error("Error creating issue", improveRestException(e, issueCreator));
        }
        return Optional.empty();
    }

    private String createDescriptionText(String description) {
        String truncatedDescription = StringUtils.substring(description, 0, description.length() - DESCRIPTION_TRUNCATED_TEXT.length());
        return StringUtils.join(truncatedDescription, DESCRIPTION_TRUNCATED_TEXT);
    }

    @Override
    protected boolean transitionIssue(IssueResponseModel issueModel, IssueConfig issueConfig, IssueOperation operation) throws IntegrationException {
        return jiraTransitionHelper.transitionIssueIfNecessary(issueModel.getKey(), issueConfig, operation);
    }

    private IssueTrackerException improveRestException(IntegrationRestException restException, String issueCreatorEmail) {
        JsonObject responseContent = gson.fromJson(restException.getHttpResponseContent(), JsonObject.class);
        List<String> responseErrors = new ArrayList<>();
        if (null != responseContent) {
            if (responseContent.has("errors")) {
                JsonObject errors = responseContent.get("errors").getAsJsonObject();
                if (errors.has("reporter")) {
                    JsonElement reporterErrorMessage = errors.get("reporter");
                    if (null != reporterErrorMessage) {
                        return IssueTrackerFieldException.singleFieldError(
                            getIssueCreatorFieldKey(),
                            String.format("There was a problem assigning '%s' to the issue. Please ensure that the user is assigned to the project and has permission to transition issues. Error: %s", issueCreatorEmail, reporterErrorMessage)
                        );
                    }
                } else {
                    Set<Map.Entry<String, JsonElement>> entries = errors.entrySet();
                    List<String> fieldErrors = entries.stream()
                                                   .map(entry -> String.format("Field '%s' has error %s", entry.getKey(), entry.getValue()))
                                                   .collect(Collectors.toList());
                    responseErrors.addAll(fieldErrors);
                }

            }
            if (responseContent.has("errorMessages")) {
                JsonArray errorMessages = responseContent.get("errorMessages").getAsJsonArray();
                for (JsonElement errorMessage : errorMessages) {
                    responseErrors.add(errorMessage.getAsString());
                }
            }
        }

        String message = restException.getMessage();
        if (!responseErrors.isEmpty()) {
            message += " | Details: " + StringUtils.join(responseErrors, ", ");
        }

        return new IssueTrackerException(message, restException);
    }

    private void addIssueProperties(String issueKey, JiraIssueSearchProperties issueProperties) throws IntegrationException {
        jiraIssuePropertyHelper.addPropertiesToIssue(issueKey, issueProperties);
    }

    private IssueRequestModelFieldsBuilder createFieldsBuilder(IssueContentModel contentModel) {
        IssueRequestModelFieldsBuilder fieldsBuilder = new IssueRequestModelFieldsBuilder();
        fieldsBuilder.setSummary(contentModel.getTitle());
        fieldsBuilder.setDescription(contentModel.getDescription());

        return fieldsBuilder;
    }

    @Override
    protected void logIssueAction(String issueTrackerProjectName, IssueTrackerRequest request) {
        JiraIssueSearchProperties issueProperties = request.getIssueSearchProperties();
        String issueTrackerProjectVersion = issueProperties.getSubTopicValue() != null ? issueProperties.getSubTopicValue() : "unknown";
        String arbitraryItemSubComponent = issueProperties.getSubComponentValue() != null ? issueProperties.getSubTopicValue() : "unknown";
        logger.debug("Attempting the {} action on the project {}. Provider: {}, Provider Url: {}, Provider Project: {}[{}]. Category: {}, Component: {}, SubComponent: {}.",
            request.getOperation().name(), issueTrackerProjectName, issueProperties.getProvider(), issueProperties.getProviderUrl(), issueProperties.getTopicValue(), issueTrackerProjectVersion, issueProperties.getCategory(),
            issueProperties.getComponentValue(),
            arbitraryItemSubComponent);
    }
}
