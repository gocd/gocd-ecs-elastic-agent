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
import com.thoughtworks.gocd.elasticagent.ecs.ECSTask;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTasks;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Agent;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ShouldAssignWorkRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ShouldAssignWorkRequestExecutorTest {
    private final String environment = "production";
    private ECSTasks agentInstances;
    private ECSTask task;
    private ElasticAgentProfileProperties elasticAgentProfileProperties;
    private ClusterProfileProperties clusterProfileProperties;

    @BeforeEach
    void setUp() {
        task = mock(ECSTask.class);
        agentInstances = mock(ECSTasks.class);
        elasticAgentProfileProperties = mock(ElasticAgentProfileProperties.class);
        clusterProfileProperties = mock(ClusterProfileProperties.class);

        when(task.name()).thenReturn("task-name");
        when(task.getJobIdentifier()).thenReturn(getJobIdentifierWithId(1));
        when(agentInstances.find("task-name")).thenReturn(task);
    }

    @Test
    void shouldAssignWorkToContainerWithMatchingJobIdentifier() {
        ShouldAssignWorkRequest request = new ShouldAssignWorkRequest(new Agent(task.name(), null, null, null), environment, elasticAgentProfileProperties, getJobIdentifierWithId(1), clusterProfileProperties);

        GoPluginApiResponse response = new ShouldAssignWorkRequestExecutor(request, agentInstances).execute();

        assertThat(response.responseCode()).isEqualTo(200);
        assertThat(response.responseBody()).isEqualTo("true");
    }

    @Test
    void shouldNotAssignWorkToContainerWithDifferentJobIdentifier() {
        ShouldAssignWorkRequest request = new ShouldAssignWorkRequest(new Agent(task.name(), null, null, null), "FooEnv", elasticAgentProfileProperties, getJobIdentifierWithId(2), clusterProfileProperties);

        GoPluginApiResponse response = new ShouldAssignWorkRequestExecutor(request, agentInstances).execute();

        assertThat(response.responseCode()).isEqualTo(200);
        assertThat(response.responseBody()).isEqualTo("false");
    }

    private JobIdentifier getJobIdentifierWithId(long jobId) {
        return new JobIdentifier("up42", 1L, "p-label", "up42-stage", "1", "test", jobId);
    }

}
