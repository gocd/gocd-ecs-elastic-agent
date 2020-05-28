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

import com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthType;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class DockerRegistrySettingsValidatorTest {

    @Mock
    private ValidateClusterProfileRequest ClusterProfileValidateRequest;
    private DockerRegistrySettingsValidator validator;

    @BeforeEach
    void setUp() {
        initMocks(this);

        this.validator = new DockerRegistrySettingsValidator();
    }

    @Test
    void shouldReturnEmptyErrorListIfRegistrySetToDefault() {
        when(ClusterProfileValidateRequest.get(PRIVATE_DOCKER_REGISTRY_AUTH_TYPE)).thenReturn(DockerRegistryAuthType.DEFAULT.getValue());

        final List<Map<String, String>> result = validator.validate(ClusterProfileValidateRequest);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldReturnErrorMessageIfUrlIsNotConfigured() {
        when(ClusterProfileValidateRequest.get(PRIVATE_DOCKER_REGISTRY_AUTH_TYPE)).thenReturn(DockerRegistryAuthType.AUTH_TOKEN.getValue());
        when(ClusterProfileValidateRequest.get(PRIVATE_DOCKER_REGISTRY_EMAIL)).thenReturn("some-email");
        when(ClusterProfileValidateRequest.get(PRIVATE_DOCKER_REGISTRY_AUTH_TOKEN)).thenReturn("some-auth-token");

        final List<Map<String, String>> result = validator.validate(ClusterProfileValidateRequest);

        assertThat(result).hasSize(1);

        assertThat(result.get(0)).containsEntry("key", "PrivateDockerRegistryUrl");
        assertThat(result.get(0)).containsEntry("message", "PrivateDockerRegistryUrl must not be blank.");
    }

    @Test
    void shouldReturnErrorMessageIfEmailIsNotConfigured() {
        when(ClusterProfileValidateRequest.get(PRIVATE_DOCKER_REGISTRY_AUTH_TYPE)).thenReturn(DockerRegistryAuthType.AUTH_TOKEN.getValue());
        when(ClusterProfileValidateRequest.get(PRIVATE_DOCKER_REGISTRY_URL)).thenReturn("some-url");
        when(ClusterProfileValidateRequest.get(PRIVATE_DOCKER_REGISTRY_AUTH_TOKEN)).thenReturn("some-auth-token");

        final List<Map<String, String>> result = validator.validate(ClusterProfileValidateRequest);

        assertThat(result).hasSize(1);

        assertThat(result.get(0)).containsEntry("key", "PrivateDockerRegistryEmail");
        assertThat(result.get(0)).containsEntry("message", "PrivateDockerRegistryEmail must not be blank.");
    }

    @Test
    void shouldReturnErrorMessageIfAuthTokenDataIsNotConfigured() {
        when(ClusterProfileValidateRequest.get(PRIVATE_DOCKER_REGISTRY_AUTH_TYPE)).thenReturn(DockerRegistryAuthType.AUTH_TOKEN.getValue());

        final List<Map<String, String>> result = validator.validate(ClusterProfileValidateRequest);

        assertThat(result).hasSize(3);

        assertThat(result.get(0)).containsEntry("key", "PrivateDockerRegistryEmail");
        assertThat(result.get(0)).containsEntry("message", "PrivateDockerRegistryEmail must not be blank.");

        assertThat(result.get(1)).containsEntry("key", "PrivateDockerRegistryUrl");
        assertThat(result.get(1)).containsEntry("message", "PrivateDockerRegistryUrl must not be blank.");

        assertThat(result.get(2)).containsEntry("key", "PrivateDockerRegistryAuthToken");
        assertThat(result.get(2)).containsEntry("message", "PrivateDockerRegistryAuthToken must not be blank.");
    }

    @Test
    void shouldReturnErrorMessageIfUsernameIsNotConfigured() {
        when(ClusterProfileValidateRequest.get(PRIVATE_DOCKER_REGISTRY_AUTH_TYPE)).thenReturn(DockerRegistryAuthType.USERNAME_PASSWORD.getValue());

        final List<Map<String, String>> result = validator.validate(ClusterProfileValidateRequest);

        assertThat(result).hasSize(4);

        assertThat(result.get(0)).containsEntry("key", "PrivateDockerRegistryEmail");
        assertThat(result.get(0)).containsEntry("message", "PrivateDockerRegistryEmail must not be blank.");

        assertThat(result.get(1)).containsEntry("key", "PrivateDockerRegistryUrl");
        assertThat(result.get(1)).containsEntry("message", "PrivateDockerRegistryUrl must not be blank.");

        assertThat(result.get(2)).containsEntry("key", "PrivateDockerRegistryUsername");
        assertThat(result.get(2)).containsEntry("message", "PrivateDockerRegistryUsername must not be blank.");

        assertThat(result.get(3)).containsEntry("key", "PrivateDockerRegistryPassword");
        assertThat(result.get(3)).containsEntry("message", "PrivateDockerRegistryPassword must not be blank.");
    }
}
