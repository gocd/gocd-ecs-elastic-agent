/*
 * Copyright 2022 Thoughtworks, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.thoughtworks.gocd.elasticagent.ecs.validators;

import com.thoughtworks.gocd.elasticagent.ecs.aws.AWSCredentialsProviderChain;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;

import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.AWS_ACCESS_KEY_ID;
import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.AWS_SECRET_ACCESS_KEY;

public class CredentialsValidator extends AbstractValidator {
    private final AWSCredentialsProviderChain credentialsProviderChain;

    public CredentialsValidator() {
        this(new AWSCredentialsProviderChain());
    }

    CredentialsValidator(AWSCredentialsProviderChain credentialsProviderChain) {
        this.credentialsProviderChain = credentialsProviderChain;
    }

    void validatePluginSettings(ValidateClusterProfileRequest request) {
        final String accessKey = request.get(AWS_ACCESS_KEY_ID);
        final String secretKey = request.get(AWS_SECRET_ACCESS_KEY);
        try {
            credentialsProviderChain.getAWSCredentialsProvider(accessKey, secretKey);
        } catch (Exception e) {
            addError(AWS_ACCESS_KEY_ID, e.getMessage());
            addError(AWS_SECRET_ACCESS_KEY, e.getMessage());
        }
    }
}
