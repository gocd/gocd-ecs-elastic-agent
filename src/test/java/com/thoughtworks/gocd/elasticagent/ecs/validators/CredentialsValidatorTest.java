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
import com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import software.amazon.awssdk.auth.credentials.EnvironmentVariableCredentialsProvider;
import software.amazon.awssdk.auth.credentials.SystemPropertyCredentialsProvider;
import software.amazon.awssdk.core.SdkSystemSetting;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.AWS_ASSUME_ROLE_ARN;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_ACCESS_KEY_ID;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_SECRET_ACCESS_KEY;

@ExtendWith(SystemStubsExtension.class)
class CredentialsValidatorTest {

    @SystemStub
    EnvironmentVariables environmentVariables = new EnvironmentVariables()
            .set(AWS_ACCESS_KEY_ID.environmentVariable(), "")
            .set(AWS_SECRET_ACCESS_KEY.environmentVariable(), "");

    @SystemStub
    SystemProperties systemProperties;

    @Mock
    private ValidateClusterProfileRequest request;
    private CredentialsValidator credentialsValidator;

    @BeforeEach
    void setUp() {
        openMocks(this);
        credentialsValidator = new CredentialsValidator(new AWSCredentialsProviderChain(() -> "my-server-id", EnvironmentVariableCredentialsProvider.create(), SystemPropertyCredentialsProvider.create()));
    }

    @Test
    void shouldReturnEmptyErrorIfAccessKeyAndSecretKeyIsProvided() {
        when(request.get(GetPluginConfigurationExecutor.AWS_ACCESS_KEY_ID)).thenReturn("access-key");
        when(request.get(GetPluginConfigurationExecutor.AWS_SECRET_ACCESS_KEY)).thenReturn("secret-key");

        final Collection<? extends Map<String, String>> validationResult = credentialsValidator.validate(request);

        assertThat(validationResult).isEmpty();
    }

    @Test
    void shouldReturnEmptyErrorIfAssumeRoleArnIsProvidedAlongWithAccessKeyAndSecretKey() throws Exception {
        when(request.get(GetPluginConfigurationExecutor.AWS_ACCESS_KEY_ID)).thenReturn("access-key");
        when(request.get(GetPluginConfigurationExecutor.AWS_SECRET_ACCESS_KEY)).thenReturn("secret-key");
        when(request.get(GetPluginConfigurationExecutor.AWS_ASSUME_ROLE_ARN)).thenReturn("arn:aws:iam::111111111111:role/gocd-ecs-plugin-role");
        when(request.get(GetPluginConfigurationExecutor.CLUSTER_NAME)).thenReturn("GoCD");

        environmentVariables
                .set("AWS_REGION", "us-east-1")
                .execute(() -> {
                    final Collection<? extends Map<String, String>> validationResult = credentialsValidator.validate(request);

                    assertThat(validationResult).isEmpty();
                });
    }

    @Test
    void shouldReturnEmptyErrorIfCredentialsAutoDetectTheCredentialsUsingProviders() throws Exception {
        systemProperties
                .set(SdkSystemSetting.AWS_ACCESS_KEY_ID.property(), "access-key-from-system-prop")
                .set(SdkSystemSetting.AWS_SECRET_ACCESS_KEY.property(), "secret-key-from-system-prop")
                .execute(() -> {
                    final Collection<? extends Map<String, String>> validationResult = credentialsValidator.validate(request);

                    assertThat(validationResult).isEmpty();
                });
    }

    @Test
    void shouldReturnErrorIfItFailsToAutoDetectCredentials() {
        final List<Map<String, String>> validationResult = credentialsValidator.validate(request);

        assertThat(validationResult).containsExactly(Map.of(
                "key", AWS_ASSUME_ROLE_ARN,
                "message", "Unable to load AWS credentials from any provider in the chain")
        );
    }

    @Test
    void shouldReturnErrorIfOnlyAccessKeyProvidedNotASecretKey() {
        when(request.get(GetPluginConfigurationExecutor.AWS_ACCESS_KEY_ID)).thenReturn("access-key");

        final List<Map<String, String>> validationResult = credentialsValidator.validate(request);

        assertThat(validationResult).hasSize(2);
        assertThat(validationResult.get(0)).containsEntry("key", GetPluginConfigurationExecutor.AWS_ACCESS_KEY_ID);
        assertThat(validationResult.get(0)).containsEntry("message", "Secret key is mandatory if access key is provided");

        assertThat(validationResult.get(1)).containsEntry("key", GetPluginConfigurationExecutor.AWS_SECRET_ACCESS_KEY);
        assertThat(validationResult.get(1)).containsEntry("message", "Secret key is mandatory if access key is provided");

    }
}
