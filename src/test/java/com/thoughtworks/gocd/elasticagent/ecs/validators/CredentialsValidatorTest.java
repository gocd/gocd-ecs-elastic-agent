/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.amazonaws.auth.EnvironmentVariableCredentialsProvider;
import com.amazonaws.auth.SystemPropertiesCredentialsProvider;
import com.thoughtworks.gocd.elasticagent.ecs.aws.AWSCredentialsProviderChain;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;
import com.thoughtworks.gocd.extensions.SystemProperty;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.amazonaws.SDKGlobalConfiguration.ACCESS_KEY_SYSTEM_PROPERTY;
import static com.amazonaws.SDKGlobalConfiguration.SECRET_KEY_SYSTEM_PROPERTY;
import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.AWS_ACCESS_KEY_ID;
import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.AWS_SECRET_ACCESS_KEY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class CredentialsValidatorTest {
    @Mock
    private ValidateClusterProfileRequest request;
    private CredentialsValidator credentialsValidator;

    @BeforeEach
    void setUp() {
        openMocks(this);
        credentialsValidator = new CredentialsValidator(new AWSCredentialsProviderChain(new EnvironmentVariableCredentialsProvider(), new SystemPropertiesCredentialsProvider()));
    }

    @Test
    void shouldReturnEmptyErrorIfAccessKeyAndSecretKeyIsProvided() {
        when(request.get(AWS_ACCESS_KEY_ID)).thenReturn("access-key");
        when(request.get(AWS_SECRET_ACCESS_KEY)).thenReturn("secret-key");

        final Collection<? extends Map<String, String>> validationResult = credentialsValidator.validate(request);

        assertThat(validationResult).isEmpty();
    }

    @Test
    @SystemProperty(key = ACCESS_KEY_SYSTEM_PROPERTY, value = "access-key-from-system-prop")
    @SystemProperty(key = SECRET_KEY_SYSTEM_PROPERTY, value = "secret-key-from-system-prop")
    void shouldReturnEmptyErrorIfCredentialsAutoDetectTheCredentialsUsingProviders() {
        final Collection<? extends Map<String, String>> validationResult = credentialsValidator.validate(request);

        assertThat(validationResult).isEmpty();
    }

    @Test
    void shouldReturnErrorIfItFailsToAutoDetectCredentials() {
        final List<Map<String, String>> validationResult = credentialsValidator.validate(request);

        assertThat(validationResult).hasSize(2);
        assertThat(validationResult.get(0)).containsEntry("key", AWS_ACCESS_KEY_ID);
        assertThat(validationResult.get(0)).containsEntry("message", "Unable to load AWS credentials from any provider in the chain");

        assertThat(validationResult.get(1)).containsEntry("key", AWS_SECRET_ACCESS_KEY);
        assertThat(validationResult.get(1)).containsEntry("message", "Unable to load AWS credentials from any provider in the chain");
    }

    @Test
    void shouldReturnErrorIfOnlyAccessKeyProvidedNotASecretKey() {
        when(request.get(AWS_ACCESS_KEY_ID)).thenReturn("access-key");

        final List<Map<String, String>> validationResult = credentialsValidator.validate(request);

        assertThat(validationResult).hasSize(2);
        assertThat(validationResult.get(0)).containsEntry("key", AWS_ACCESS_KEY_ID);
        assertThat(validationResult.get(0)).containsEntry("message", "Secret key is mandatory if access key is provided");

        assertThat(validationResult.get(1)).containsEntry("key", AWS_SECRET_ACCESS_KEY);
        assertThat(validationResult.get(1)).containsEntry("message", "Secret key is mandatory if access key is provided");

    }
}
