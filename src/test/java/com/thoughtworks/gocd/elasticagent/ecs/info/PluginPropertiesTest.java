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

package com.thoughtworks.gocd.elasticagent.ecs.info;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginPropertiesTest {
    @Test
    void shouldReadPluginProperties() {
        final PluginProperties properties = PluginProperties.instance();

        assertThat(properties.get("id")).isEqualTo("com.thoughtworks.gocd.elastic-agent.ecs");
        assertThat(properties.get("name")).isEqualTo("GoCD Elastic Agent Plugin for Amazon ECS");
        assertThat(properties.get("description")).isEqualTo("GoCD Elastic Agent Plugin for Amazon Elastic Container Service allow for more efficient use of instances");
        assertThat(properties.get("vendorName")).isEqualTo("ThoughtWorks, Inc.");
        assertThat(properties.get("vendorUrl")).isEqualTo("https://github.com/gocd/gocd-ecs-elastic-agent");
        assertThat(properties.get("goCdVersion")).isEqualTo("19.3.0");
        assertThat(properties.get("version")).isNotBlank();
    }
}
