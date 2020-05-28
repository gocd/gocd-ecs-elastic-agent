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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import org.junit.jupiter.api.Test;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.GSON;
import static org.assertj.core.api.Java6Assertions.assertThat;

public class BindMountTest {
    @Test
    void shouldDeSerializableFromJSON() {
        String json = "{\n" +
                "  \"Name\": \"data\",\n" +
                "  \"SourcePath\": \"/ecs/data\",\n" +
                "  \"ContainerPath\": \"/var/data\"\n" +
                "}";

        BindMount bindMount = GSON.fromJson(json, BindMount.class);

        assertThat(bindMount.getName()).isEqualTo("data");
        assertThat(bindMount.getSourcePath()).isEqualTo("/ecs/data");
        assertThat(bindMount.getContainerPath()).isEqualTo("/var/data");
    }

    @Test
    void shouldBeInvalidConfigurationIfNameIsNotProvided() {
        String json = "{\n" +
                "  \"SourcePath\": \"/ecs/data\",\n" +
                "  \"ContainerPath\": \"/var/data\"\n" +
                "}";

        BindMount bindMount = GSON.fromJson(json, BindMount.class);

        assertThat(bindMount.isValid()).isFalse();
        assertThat(bindMount.errors()).isEqualTo("Name cannot be empty.");
    }

    @Test
    void shouldBeInvalidConfigurationIfSourcePathIsNotProvided() {
        String json = "{\n" +
                "  \"Name\": \"data\",\n" +
                "  \"ContainerPath\": \"/var/data\"\n" +
                "}";

        BindMount bindMount = GSON.fromJson(json, BindMount.class);

        assertThat(bindMount.isValid()).isFalse();
        assertThat(bindMount.errors()).isEqualTo("SourcePath cannot be empty.");
    }

    @Test
    void shouldBeInvalidConfigurationIfContainerPathIsNotProvided() {
        String json = "{\n" +
                "  \"Name\": \"data\",\n" +
                "  \"SourcePath\": \"/ecs/data\"\n" +
                "}";
        BindMount bindMount = GSON.fromJson(json, BindMount.class);

        assertThat(bindMount.isValid()).isFalse();
        assertThat(bindMount.errors()).isEqualTo("ContainerPath cannot be empty.");
    }

    @Test
    void shouldListAllErrors() {
        BindMount bindMount = GSON.fromJson("{}", BindMount.class);

        assertThat(bindMount.isValid()).isFalse();
        assertThat(bindMount.errors()).isEqualTo("ContainerPath cannot be empty. , Name cannot be empty. , SourcePath cannot be empty.");
    }
}
