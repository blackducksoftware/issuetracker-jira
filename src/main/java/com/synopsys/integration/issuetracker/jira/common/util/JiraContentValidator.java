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

import com.synopsys.integration.issuetracker.common.message.IssueContentLengthValidator;

public class JiraContentValidator extends IssueContentLengthValidator {
    public static final int CONTENT_LENGTH = 30000;
    public static final int TITLE_LENGTH = 255;

    @Override
    public int getTitleLength() {
        return TITLE_LENGTH;
    }

    @Override
    public int getDescriptionLength() {
        return CONTENT_LENGTH;
    }

    @Override
    public int getCommentLength() {
        return CONTENT_LENGTH;
    }
}
