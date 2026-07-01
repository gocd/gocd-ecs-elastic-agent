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

package com.thoughtworks.gocd.elasticagent.ecs.requests;

import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfileProperties;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CreateAgentRequestTest {

    @Test
    void shouldDeserializeFromJSON() {
        String json = """
                {
                  "auto_register_key": "secret-key",
                  "elastic_agent_profile_properties": {
                    "Image": "value1",
                    "MaxMemory": "2G",
                    "ReservedMemory": "150M",
                    "TerminationPolicy": ""
                  },
                  "cluster_profile_properties": {
                    "GoServerUrl": "https://cd.server.com/go",\s
                    "ClusterName": "deployment-cluster"
                  },
                  "environment": "prod"
                }""";

        CreateAgentRequest request = CreateAgentRequest.fromJSON(json);
        assertThat(request.autoRegisterKey()).isEqualTo("secret-key");
        assertThat(request.environment()).isEqualTo("prod");
        assertThat(request.elasticProfile().getImage()).isEqualTo("value1");
        assertThat(request.elasticProfile().getMaxMemory()).isEqualTo(2048);

        Map<String, String> clusterProfileConfigurations = new HashMap<>();
        clusterProfileConfigurations.put("GoServerUrl", "https://cd.server.com/go");
        clusterProfileConfigurations.put("ClusterName", "deployment-cluster");
        ClusterProfileProperties expectedClusterProfileProperties = ClusterProfileProperties.fromConfiguration(clusterProfileConfigurations);

        assertThat(request.clusterProfileProperties()).isEqualTo(expectedClusterProfileProperties);
    }
}
