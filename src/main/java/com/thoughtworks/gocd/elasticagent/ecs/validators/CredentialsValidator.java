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
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.AWSCredentialsException;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;

import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.*;

public class CredentialsValidator extends AbstractValidator {
    private final AWSCredentialsProviderChain credentialsProviderChain;

    public CredentialsValidator() {
        this(new AWSCredentialsProviderChain());
    }

    CredentialsValidator(AWSCredentialsProviderChain credentialsProviderChain) {
        this.credentialsProviderChain = credentialsProviderChain;
    }

    void validatePluginSettings(ValidateClusterProfileRequest request) {

        try {
            credentialsProviderChain.getAWSCredentialsProvider(
                    request.get(AWS_ACCESS_KEY_ID),
                    request.get(AWS_SECRET_ACCESS_KEY),
                    request.get(AWS_ASSUME_ROLE_ARN),
                    request.get(CLUSTER_NAME));
        } catch (AWSCredentialsException e) {
            addError(AWS_ACCESS_KEY_ID, e.getMessage());
            addError(AWS_SECRET_ACCESS_KEY, e.getMessage());
        } catch (Exception e) {
            // Put other types of errors against all three fields; so effectively against the ARN.
            addError(AWS_ASSUME_ROLE_ARN, e.getMessage());
        }
    }
}
