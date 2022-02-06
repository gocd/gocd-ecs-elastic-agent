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

import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.List;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class VolumeSettingsValidatorTest {
    @Mock
    private ValidateClusterProfileRequest request;

    private VolumeSettingsValidator validator;

    @BeforeEach
    void setUp() {
        openMocks(this);

        validator = new VolumeSettingsValidator();
    }

    @Nested
    class Linux {
        @Test
        void shouldReturnEmptyErrorListIfVolumeTypeIsSetToNone() {
            when(request.get(LINUX_DOCKER_VOLUME_TYPE)).thenReturn("none");

            final List<Map<String, String>> errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        void shouldErrorOutIfVolumeSizeIsNotProvided() {
            when(request.get(LINUX_DOCKER_VOLUME_TYPE)).thenReturn("gp2");

            final List<Map<String, String>> errors = validator.validate(request);

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0))
                    .containsEntry("key", "LinuxDockerVolumeSize")
                    .containsEntry("message", "LinuxDockerVolumeSize must not be blank.");
        }

        @Test
        void shouldErrorOutIfProvisionedIOPSNotSetWhenVolumeTypeIsIO1() {
            when(request.get(LINUX_DOCKER_VOLUME_TYPE)).thenReturn("io1");
            when(request.get(LINUX_DOCKER_VOLUME_SIZE)).thenReturn("100");

            final List<Map<String, String>> errors = validator.validate(request);

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0))
                .containsEntry("key", "LinuxDockerVolumeProvisionedIOPS")
                .containsEntry("message", "LinuxDockerVolumeProvisionedIOPS must not be blank.");
        }
    }

    @Nested
    class Windows {
        @Test
        void shouldReturnEmptyErrorListIfVolumeTypeIsSetToNone() {
            when(request.get(WINDOWS_OS_VOLUME_TYPE)).thenReturn("none");

            final List<Map<String, String>> errors = validator.validate(request);

            assertThat(errors).isEmpty();
        }

        @Test
        void shouldErrorOutIfVolumeSizeIsNotProvided() {
            when(request.get(WINDOWS_OS_VOLUME_TYPE)).thenReturn("gp2");

            final List<Map<String, String>> errors = validator.validate(request);

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0))
                    .containsEntry("key", "WindowsOSVolumeSize")
                    .containsEntry("message", "WindowsOSVolumeSize must not be blank.");
        }

        @Test
        void shouldErrorOutIfVolumeSizeIsIO1AndProvisionedIOPSNotSet() {
            when(request.get(WINDOWS_OS_VOLUME_TYPE)).thenReturn("io1");
            when(request.get(WINDOWS_OS_VOLUME_SIZE)).thenReturn("100");

            final List<Map<String, String>> errors = validator.validate(request);

            assertThat(errors).hasSize(1);
            assertThat(errors.get(0))
                .containsEntry("key", "WindowsOSVolumeProvisionedIOPS")
                .containsEntry("message", "WindowsOSVolumeProvisionedIOPS must not be blank.");
        }
    }
}
