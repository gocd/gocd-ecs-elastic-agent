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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import com.google.gson.reflect.TypeToken;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import com.thoughtworks.gocd.elasticagent.ecs.requests.JobCompletionRequest;
import org.junit.jupiter.api.Test;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.GSON;
import static org.assertj.core.api.Assertions.assertThat;

class ClusterProfilePropertiesTest {

    @Test
    void shouldAlwaysGenerateSameUUID() {
        PluginSettings pluginSettings = new PluginSettingsBuilder()
                .addSetting("GoServerUrl", "https://cd.server.com/go")
                .addSetting("ClusterName", "deployment-cluster")
                .addSetting("AWSAccessKeyId", "some_access_key")
                .addSetting("AWSSecretAccessKey", "some_secret_key")
                .addSetting("EnvironmentVariables", "TZ=PST")
                .addSetting("ContainerAutoregisterTimeout", "3")
                .addSetting("InstanceType", "c2.small")
                .addSetting("KeyPairName", "your_ssh_key_name")
                .addSetting("IamInstanceProfile", "instance_profile_name")
                .addSetting("AWSRegion", "us-east-x")
                .addSetting("SubnetIds", "s-shouldAlwaysGenerateSameUUID")
                .addSetting("SecurityGroupIds", "sg-shouldAlwaysGenerateSameUUID")
                .build();

        ClusterProfileProperties clusterProfileProperties = ClusterProfileProperties.fromConfiguration(GSON.fromJson(GSON.toJson(pluginSettings), new TypeToken<>() {}));
        assertThat(clusterProfileProperties.uuid()).isEqualTo(clusterProfileProperties.uuid());
    }

    @Test
    void shouldGenerateSameUUIDAcrossRequests() {
        String createAgentRequestJSON = """
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

        String jobCompletionRequestJSON = """
                {
                  "elastic_agent_id": "ea1",
                  "job_identifier": {
                    "pipeline_name": "test-pipeline",
                    "pipeline_counter": 1,
                    "pipeline_label": "Test Pipeline",
                    "stage_name": "test-stage",
                    "stage_counter": "1",
                    "job_name": "test-job",
                    "job_id": 100
                  },
                  "elastic_agent_profile_properties": {
                    "Image": "value1",
                    "MaxMemory": "2G",
                    "ReservedMemory": "150M",
                    "TerminationPolicy": ""
                  },
                  "cluster_profile_properties": {
                    "GoServerUrl": "https://cd.server.com/go",\s
                    "ClusterName": "deployment-cluster"
                  }
                }""";

        CreateAgentRequest createAgentRequest = CreateAgentRequest.fromJSON(createAgentRequestJSON);
        String createAgentRequestUUID = createAgentRequest.clusterProfileProperties().uuid();

        JobCompletionRequest jobCompletionRequest = JobCompletionRequest.fromJSON(jobCompletionRequestJSON);
        String jobCompletionRequestUUID = jobCompletionRequest.clusterProfileProperties().uuid();

        assertThat(createAgentRequestUUID).isEqualTo(jobCompletionRequestUUID);
    }
}
