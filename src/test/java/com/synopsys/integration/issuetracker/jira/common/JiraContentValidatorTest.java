package com.synopsys.integration.issuetracker.jira.common;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.synopsys.integration.issuetracker.common.exception.IssueTrackerContentLengthException;
import com.synopsys.integration.issuetracker.common.message.IssueContentModel;
import com.synopsys.integration.issuetracker.jira.common.util.JiraContentValidator;

public class JiraContentValidatorTest {
    @Test
    public void testValidLengths() throws Exception {
        JiraContentValidator validator = new JiraContentValidator();

        assertEquals(JiraContentValidator.TITLE_LENGTH, validator.getTitleLength());
        assertEquals(JiraContentValidator.CONTENT_LENGTH, validator.getDescriptionLength());
        assertEquals(JiraContentValidator.CONTENT_LENGTH, validator.getCommentLength());
        IssueContentModel issueContentModel = createContentModel(false);
        assertTrue(validator.validateContentLength(issueContentModel));

    }

    @Test
    public void testInvalidLengths() throws Exception {
        JiraContentValidator validator = new JiraContentValidator();

        IssueContentModel issueContentModel = createContentModel(true);
        try {
            validator.validateContentLength(issueContentModel);
            fail();
        } catch (IssueTrackerContentLengthException ex) {

        }
    }

    private IssueContentModel createContentModel(boolean invalid) {
        String title = "Valid Title Length";
        String description = "Valid Description Length";

        if (invalid) {
            StringBuilder titleBuilder = new StringBuilder(title);
            StringBuilder descriptionBuilder = new StringBuilder(description);
            for (int index = title.length(); index < JiraContentValidator.TITLE_LENGTH + 10; index++) {
                titleBuilder.append("a");
            }
            for (int index = description.length(); index < JiraContentValidator.CONTENT_LENGTH + 10; index++) {
                descriptionBuilder.append("b");
            }

            title = titleBuilder.toString();
            description = descriptionBuilder.toString();
        }

        List<String> descriptionComments = new ArrayList<>();
        descriptionComments.add(description);
        List<String> additionalComments = new ArrayList<>();
        additionalComments.add(description);
        return IssueContentModel.of(title, description, descriptionComments, additionalComments);
    }

}
