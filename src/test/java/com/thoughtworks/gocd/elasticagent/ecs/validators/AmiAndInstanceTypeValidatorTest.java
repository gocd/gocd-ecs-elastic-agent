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

import java.util.List;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AmiAndInstanceTypeValidatorTest {
    private AmiAndInstanceTypeValidator validator;
    private ValidateClusterProfileRequest request;

    @BeforeEach
    void setUp() {
        validator = new AmiAndInstanceTypeValidator();
        request = mock(ValidateClusterProfileRequest.class);
    }

    @Nested
    class Linux {
        @Test
        void shouldNotReturnErrorWhenMinInstanceCountIsNotSpecified() {
            final List<Map<String, String>> validationResult = validator.validate(request);

            assertThat(validationResult).hasSize(0);
        }

        @Test
        void shouldReturnErrorWhenMinimumInstanceCountIsSpecifiedAndAmiIsNot() {
            when(request.get(MIN_LINUX_INSTANCE_COUNT)).thenReturn("1");
            when(request.get(LINUX_INSTANCE_TYPE)).thenReturn("t2.small");

            final List<Map<String, String>> validationResult = validator.validate(request);

            assertThat(validationResult).hasSize(1);
            assertThat(validationResult.get(0))
                    .containsEntry("key", LINUX_AMI)
                    .containsEntry("message", "LinuxAmi must not be blank.");
        }

        @Test
        void shouldReturnErrorWhenMinimumInstanceCountIsSpecifiedAndInstanceTypeIsNot() {
            when(request.get(MIN_LINUX_INSTANCE_COUNT)).thenReturn("1");
            when(request.get(LINUX_AMI)).thenReturn("ami-12454");

            final List<Map<String, String>> validationResult = validator.validate(request);

            assertThat(validationResult).hasSize(1);
            assertThat(validationResult.get(0))
                    .containsEntry("key", LINUX_INSTANCE_TYPE)
                    .containsEntry("message", "LinuxInstanceType must not be blank.");
        }
    }

    @Nested
    class Windows {
        @Test
        void shouldNotReturnErrorWhenMinInstanceCountIsNotSpecified() {
            final List<Map<String, String>> validationResult = validator.validate(request);

            assertThat(validationResult).hasSize(0);
        }

        @Test
        void shouldReturnErrorWhenMinimumInstanceCountIsSpecifiedAndAmiIsNot() {
            when(request.get(MIN_WINDOWS_INSTANCE_COUNT)).thenReturn("1");
            when(request.get(WINDOWS_INSTANCE_TYPE)).thenReturn("t2.small");

            final List<Map<String, String>> validationResult = validator.validate(request);

            assertThat(validationResult).hasSize(1);
            assertThat(validationResult.get(0))
                    .containsEntry("key", WINDOWS_AMI)
                    .containsEntry("message", "WindowsAmi must not be blank.");
        }

        @Test
        void shouldReturnErrorWhenMinimumInstanceCountIsSpecifiedAndInstanceTypeIsNot() {
            when(request.get(MIN_WINDOWS_INSTANCE_COUNT)).thenReturn("1");
            when(request.get(WINDOWS_AMI)).thenReturn("ami-12454");

            final List<Map<String, String>> validationResult = validator.validate(request);

            assertThat(validationResult).hasSize(1);
            assertThat(validationResult.get(0))
                    .containsEntry("key", WINDOWS_INSTANCE_TYPE)
                    .containsEntry("message", "WindowsInstanceType must not be blank.");
        }
    }

}
