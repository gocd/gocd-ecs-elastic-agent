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
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;


public class JobCompletionRequestTest {
    @Test
    public void shouldDeserializeFromJSON() {
        String json = "{\n" +
                "  \"elastic_agent_id\": \"ea1\",\n" +
                "  \"job_identifier\": {\n" +
                "    \"pipeline_name\": \"test-pipeline\",\n" +
                "    \"pipeline_counter\": 1,\n" +
                "    \"pipeline_label\": \"Test Pipeline\",\n" +
                "    \"stage_name\": \"test-stage\",\n" +
                "    \"stage_counter\": \"1\",\n" +
                "    \"job_name\": \"test-job\",\n" +
                "    \"job_id\": 100\n" +
                "  },\n" +
                "  \"elastic_agent_profile_properties\": {\n" +
                "    \"Image\": \"value1\",\n" +
                "    \"MaxMemory\": \"2G\",\n" +
                "    \"ReservedMemory\": \"150M\",\n" +
                "    \"TerminationPolicy\": \"\"\n" +
                "  },\n" +
                "  \"cluster_profile_properties\": {\n" +
                "    \"GoServerUrl\": \"https://cd.server.com/go\", \n" +
                "    \"ClusterName\": \"deployment-cluster\"\n" +
                "  }\n" +
                "}";

        JobCompletionRequest request = JobCompletionRequest.fromJSON(json);
        JobIdentifier expectedJobIdentifier = new JobIdentifier("test-pipeline", 1L, "Test Pipeline", "test-stage", "1", "test-job", 100L);
        JobIdentifier actualJobIdentifier = request.jobIdentifier();
        assertThat(actualJobIdentifier).isEqualTo(expectedJobIdentifier);
        assertThat(request.getElasticAgentId()).isEqualTo("ea1");

        assertThat(request.elasticAgentProfileProperties().getImage()).isEqualTo("value1");
        assertThat(request.elasticAgentProfileProperties().getMaxMemory()).isEqualTo(2048);

        Map<String, String> clusterProfileConfigurations = new HashMap<>();
        clusterProfileConfigurations.put("GoServerUrl", "https://cd.server.com/go");
        clusterProfileConfigurations.put("ClusterName", "deployment-cluster");
        ClusterProfileProperties expectedClusterProfileProperties = ClusterProfileProperties.fromConfiguration(clusterProfileConfigurations);

        assertThat(request.clusterProfileProperties()).isEqualTo(expectedClusterProfileProperties);
    }
}
