package com.synopsys.integration.issuetracker.jira.cloud;

import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.issuetracker.common.config.IssueConfig;
import com.synopsys.integration.issuetracker.common.config.IssueTrackerContext;
import com.synopsys.integration.issuetracker.common.exception.IssueTrackerFieldException;
import com.synopsys.integration.jira.common.cloud.service.ProjectService;
import com.synopsys.integration.jira.common.cloud.service.UserSearchService;
import com.synopsys.integration.jira.common.model.components.ProjectComponent;
import com.synopsys.integration.jira.common.model.response.IssueTypeResponseModel;
import com.synopsys.integration.jira.common.model.response.PageOfProjectsResponseModel;
import com.synopsys.integration.jira.common.model.response.UserDetailsResponseModel;
import com.synopsys.integration.jira.common.rest.service.IssueMetaDataService;
import com.synopsys.integration.jira.common.rest.service.IssueTypeService;

public class JiraCloudIssueConfigValidatorTest {

    @Test
    public void validateSuccessTest() throws IntegrationException {
        ProjectService projectService = Mockito.mock(ProjectService.class);
        UserSearchService userSearchService = Mockito.mock(UserSearchService.class);
        IssueTypeService issueTypeService = Mockito.mock(IssueTypeService.class);
        IssueMetaDataService issueMetaDataService = Mockito.mock(IssueMetaDataService.class);
        Mockito.when(issueMetaDataService.doesProjectContainIssueType(Mockito.anyString(), Mockito.anyString())).thenReturn(Boolean.TRUE);
        JiraCloudIssueConfigValidator jiraIssueConfigValidator = new JiraCloudIssueConfigValidator(projectService, userSearchService, issueTypeService, issueMetaDataService);

        String resolveTransitionString = "Resolve";
        String projectName = "ProjectName";
        String issueTypeString = "IssueType";
        String issueCreatorString = "IssueCreator";

        IssueConfig issueConfig = new IssueConfig();
        issueConfig.setCommentOnIssues(true);
        issueConfig.setResolveTransition(resolveTransitionString);
        issueConfig.setProjectName(projectName);
        issueConfig.setIssueType(issueTypeString);
        issueConfig.setIssueCreator(issueCreatorString);

        IssueTypeResponseModel issue = Mockito.mock(IssueTypeResponseModel.class);
        Mockito.when(issue.getName()).thenReturn(issueTypeString);
        List<IssueTypeResponseModel> issueResponses = new ArrayList<>(1);
        issueResponses.add(issue);
        Mockito.when(issueTypeService.getAllIssueTypes()).thenReturn(issueResponses);

        UserDetailsResponseModel user = Mockito.mock(UserDetailsResponseModel.class);
        Mockito.when(user.getEmailAddress()).thenReturn(issueCreatorString);
        List<UserDetailsResponseModel> userResponses = new ArrayList<>(1);
        userResponses.add(user);
        Mockito.when(userSearchService.findUser(Mockito.anyString())).thenReturn(userResponses);

        ProjectComponent projectComponent = Mockito.mock(ProjectComponent.class);
        Mockito.when(projectComponent.getName()).thenReturn(projectName);
        PageOfProjectsResponseModel projectResponse = Mockito.mock(PageOfProjectsResponseModel.class);
        List<ProjectComponent> projectComponents = new ArrayList<>(1);
        projectComponents.add(projectComponent);
        Mockito.when(projectResponse.getProjects()).thenReturn(projectComponents);
        Mockito.when(projectService.getProjectsByName(Mockito.anyString())).thenReturn(projectResponse);

        try {
            IssueTrackerContext context = new IssueTrackerContext(null, issueConfig);
            jiraIssueConfigValidator.createValidIssueConfig(context);
            Assertions.assertEquals(resolveTransitionString, issueConfig.getResolveTransition().orElse(""));
            Assertions.assertEquals(projectName, issueConfig.getProjectName());
            Assertions.assertEquals(issueCreatorString, issueConfig.getIssueCreator());
            Assertions.assertEquals(issueTypeString, issueConfig.getIssueType());
        } catch (IssueTrackerFieldException e) {
            fail();
        }
    }

    @Test
    public void validateMissingRequiredTest() throws IntegrationException {
        ProjectService projectService = Mockito.mock(ProjectService.class);
        UserSearchService userSearchService = Mockito.mock(UserSearchService.class);
        IssueTypeService issueTypeService = Mockito.mock(IssueTypeService.class);
        IssueMetaDataService issueMetaDataService = Mockito.mock(IssueMetaDataService.class);
        JiraCloudIssueConfigValidator jiraIssueConfigValidator = new JiraCloudIssueConfigValidator(projectService, userSearchService, issueTypeService, issueMetaDataService);

        String resolveTransitionString = "Resolve";
        String issueTypeString = "IssueType";

        IssueConfig issueConfig = new IssueConfig();
        issueConfig.setCommentOnIssues(true);
        issueConfig.setResolveTransition(resolveTransitionString);
        issueConfig.setIssueType(issueTypeString);

        IssueTypeResponseModel issue = Mockito.mock(IssueTypeResponseModel.class);
        Mockito.when(issue.getName()).thenReturn(issueTypeString);
        List<IssueTypeResponseModel> issueResponses = new ArrayList<>(1);
        issueResponses.add(issue);
        Mockito.when(issueTypeService.getAllIssueTypes()).thenReturn(issueResponses);

        try {
            IssueTrackerContext context = new IssueTrackerContext(null, issueConfig);
            jiraIssueConfigValidator.createValidIssueConfig(context);
            fail();
        } catch (IssueTrackerFieldException e) {
            Assertions.assertTrue(e.getFieldErrors().containsKey(JiraCloudProperties.KEY_JIRA_PROJECT_NAME));
            Assertions.assertTrue(e.getFieldErrors().containsKey(JiraCloudProperties.KEY_ISSUE_CREATOR));
            Assertions.assertFalse(e.getFieldErrors().containsKey(JiraCloudProperties.KEY_ISSUE_TYPE));
        }
    }
}
