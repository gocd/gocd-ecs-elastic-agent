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

package com.thoughtworks.gocd.elasticagent.ecs.executors;

import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;
import com.thoughtworks.gocd.elasticagent.ecs.validators.CredentialsValidator;
import com.thoughtworks.gocd.elasticagent.ecs.validators.DockerRegistrySettingsValidator;
import com.thoughtworks.gocd.elasticagent.ecs.validators.VolumeSettingsValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.skyscreamer.jsonassert.JSONAssert;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;

import static com.amazonaws.SDKGlobalConfiguration.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@ExtendWith(SystemStubsExtension.class)
class ValidateClusterProfileRequestExecutorTest {

    @SystemStub
    EnvironmentVariables environmentVariables = new EnvironmentVariables()
            .set(ACCESS_KEY_ENV_VAR, "")
            .set(ALTERNATE_ACCESS_KEY_ENV_VAR, "")
            .set(SECRET_KEY_ENV_VAR, "")
            .set(ALTERNATE_SECRET_KEY_ENV_VAR, "");

    @Test
    void shouldValidateABadConfiguration() throws Exception {
        environmentVariables
                .set(ACCESS_KEY_ENV_VAR, "access-key-from-env")
                .set(SECRET_KEY_ENV_VAR, "secret-key-from-env")
                .execute(() -> {
                    ValidateClusterProfileRequest request = new ValidateClusterProfileRequest();
                    GoPluginApiResponse response = new ValidateClusterProfileRequestExecutor(request).execute();

                    assertThat(response.responseCode()).isEqualTo(200);
                    System.out.println(response.responseBody());
                    JSONAssert.assertEquals("[\n" +
                            "  {\n" +
                            "    \"message\": \"Go Server URL must not be blank.\",\n" +
                            "    \"key\": \"GoServerUrl\"\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"message\": \"AWS Cluster Name must not be blank.\",\n" +
                            "    \"key\": \"ClusterName\"\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"message\": \"Container auto-register timeout (in minutes) must be a positive integer.\",\n" +
                            "    \"key\": \"ContainerAutoregisterTimeout\"\n" +
                            "  },\n" +
                            "  {\n" +
                            "    \"message\": \"Iam instance profile must not be blank.\",\n" +
                            "    \"key\": \"IamInstanceProfile\"\n" +
                            "  }\n" +
                            "]", response.responseBody(), true);
                });
    }

    @Test
    void shouldErrorOutIfLogDriverNameIsNotValid() throws Exception {
        ValidateClusterProfileRequest request = getValidPluginSettings();
        request.put("LogDriver", "unknown-log-driver-name");

        GoPluginApiResponse response = new ValidateClusterProfileRequestExecutor(request).execute();

        JSONAssert.assertEquals("[\n" +
                "  {\n" +
                "    \"message\": \"Log driver unknown-log-driver-name is not supported. Supported log drivers are [json-file, syslog, journald, gelf, fluentd, awslogs, splunk, awsfirelens] \",\n" +
                "    \"key\": \"LogDriver\"\n" +
                "  }\n" +
                "]", response.responseBody(), true);
    }

    @Test
    void shouldValidateAGoodConfiguration() throws Exception {
        ValidateClusterProfileRequest request = getValidPluginSettings();

        GoPluginApiResponse response = new ValidateClusterProfileRequestExecutor(request).execute();

        assertThat(response.responseCode()).isEqualTo(200);
        System.out.println(response.responseBody());
        JSONAssert.assertEquals("[]", response.responseBody(), true);
    }

    private ValidateClusterProfileRequest getValidPluginSettings() {
        ValidateClusterProfileRequest request = new ValidateClusterProfileRequest();
        request.put("AWSSecretAccessKey", "your secret key");
        request.put("AWSAccessKeyId", "your access key");
        request.put("GoServerUrl", "https://ci.example.com");
        request.put("ContainerAutoregisterTimeout", "10");
        request.put("ClusterName", "foo");
        request.put("KeyPairName", "1");
        request.put("IamInstanceProfile", "1");
        request.put("AWSRegion", "ap-south-1");
        request.put("SubnetIds", "xyz-subnet");
        request.put("MaxContainerDataVolumeSize", "20");
        return request;
    }

    @Test
    void shouldValidateAgainstAllValidator() {
        final CredentialsValidator credentialsValidator = mock(CredentialsValidator.class);
        final VolumeSettingsValidator volumeSettingsValidator = mock(VolumeSettingsValidator.class);
        final DockerRegistrySettingsValidator dockerRegistrySettingsValidator = mock(DockerRegistrySettingsValidator.class);
        final ValidateClusterProfileRequest validPluginSettings = getValidPluginSettings();

        new ValidateClusterProfileRequestExecutor(validPluginSettings,
                credentialsValidator, volumeSettingsValidator,
                dockerRegistrySettingsValidator
        ).execute();

        verify(credentialsValidator).validate(validPluginSettings);
        verify(dockerRegistrySettingsValidator).validate(validPluginSettings);
        verify(volumeSettingsValidator).validate(validPluginSettings);
    }
}
