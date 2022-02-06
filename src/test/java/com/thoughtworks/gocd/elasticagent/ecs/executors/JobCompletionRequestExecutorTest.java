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

package com.thoughtworks.gocd.elasticagent.ecs.executors;

import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.Agents;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTasks;
import com.thoughtworks.gocd.elasticagent.ecs.PluginRequest;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Agent;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.requests.JobCompletionRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class JobCompletionRequestExecutorTest {
    @Mock
    private PluginRequest mockPluginRequest;
    @Mock
    private ECSTasks mockAgentInstances;
    @Captor
    private ArgumentCaptor<List<Agent>> agentsArgumentCaptor;

    @BeforeEach
    public void setUp() {
        openMocks(this);
    }

    @Test
    public void shouldTerminateElasticAgentOnJobCompletion() throws Exception {
        JobIdentifier jobIdentifier = new JobIdentifier("test", 1L, "test", "test_stage", "1", "test_job", 100L);
        String elasticAgentId = "agent-1";
        ClusterProfileProperties clusterProfileProperties = new ClusterProfileProperties();
        JobCompletionRequest request = new JobCompletionRequest(elasticAgentId, jobIdentifier, new ElasticAgentProfileProperties(), clusterProfileProperties);
        JobCompletionRequestExecutor executor = new JobCompletionRequestExecutor(request, mockAgentInstances, mockPluginRequest);
        Agents agents = new Agents();
        agents.add(new Agent(elasticAgentId));

        when(mockPluginRequest.listAgents()).thenReturn(agents);

        GoPluginApiResponse response = executor.execute();

        InOrder inOrder = inOrder(mockPluginRequest, mockAgentInstances);
        inOrder.verify(mockPluginRequest).disableAgents(agentsArgumentCaptor.capture());
        inOrder.verify(mockAgentInstances).terminate(elasticAgentId, clusterProfileProperties);
        inOrder.verify(mockPluginRequest).deleteAgents(agentsArgumentCaptor.capture());

        List<Agent> agentsToDisabled = agentsArgumentCaptor.getValue();
        assertThat(1).isEqualTo(agentsToDisabled.size());
        assertThat(elasticAgentId).isEqualTo(agentsToDisabled.get(0).elasticAgentId());

        List<Agent> agentsToDelete = agentsArgumentCaptor.getValue();
        assertThat(agentsToDisabled).isEqualTo(agentsToDelete);
        assertThat(200).isEqualTo(response.responseCode());
        assertThat(response.responseBody().isEmpty()).isTrue();
    }

    @Test
    void shouldSkipTerminatingANonExistingAgent() throws Exception {
        JobIdentifier jobIdentifier = new JobIdentifier("test", 1L, "test", "test_stage", "1", "test_job", 100L);
        String elasticAgentId = "agent-1";
        JobCompletionRequest request = new JobCompletionRequest(elasticAgentId, jobIdentifier, new ElasticAgentProfileProperties(), new ClusterProfileProperties());
        JobCompletionRequestExecutor executor = new JobCompletionRequestExecutor(request, mockAgentInstances, mockPluginRequest);
        when(mockPluginRequest.listAgents()).thenReturn(new Agents());

        GoPluginApiResponse response = executor.execute();

        verify(mockPluginRequest, never()).disableAgents(anyCollection());
        verify(mockPluginRequest, never()).deleteAgents(anyCollection());
        verifyNoInteractions(mockAgentInstances);
        assertThat(200).isEqualTo(response.responseCode());
    }
}
