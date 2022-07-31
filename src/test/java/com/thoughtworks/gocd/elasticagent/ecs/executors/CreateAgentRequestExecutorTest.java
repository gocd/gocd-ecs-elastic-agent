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

import com.thoughtworks.gocd.elasticagent.ecs.ECSTasks;
import com.thoughtworks.gocd.elasticagent.ecs.PluginRequest;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ConsoleLogAppender;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class CreateAgentRequestExecutorTest {

    @Test
    void shouldAskECSTaskToCreateAnAgent() throws Exception {
        ClusterProfileProperties settings = mock(ClusterProfileProperties.class);
        CreateAgentRequest request = mock(CreateAgentRequest.class);
        when(request.clusterProfileProperties()).thenReturn(settings);
        when(request.getJobIdentifier()).thenReturn(new JobIdentifier("test-pipeline", 1L, "Test Pipeline", "test-stage", "1", "test-job", 100L));
        when(request.elasticProfile()).thenReturn(new ElasticAgentProfileProperties());
        ECSTasks agentInstances = mock(ECSTasks.class);
        PluginRequest pluginRequest = mock(PluginRequest.class);
        final EventStream eventStream = mock(EventStream.class);

        new CreateAgentRequestExecutor(request, agentInstances, pluginRequest, eventStream).execute();

        verify(agentInstances).create(eq(request), eq(settings), any(ConsoleLogAppender.class));
    }
}
