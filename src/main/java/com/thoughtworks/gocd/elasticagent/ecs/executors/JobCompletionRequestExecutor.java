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

import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.Agents;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTasks;
import com.thoughtworks.gocd.elasticagent.ecs.PluginRequest;
import com.thoughtworks.gocd.elasticagent.ecs.RequestExecutor;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Agent;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.requests.JobCompletionRequest;

import java.util.Collections;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;


public class JobCompletionRequestExecutor implements RequestExecutor {
    private final JobCompletionRequest jobCompletionRequest;
    private final ECSTasks agentInstances;
    private final PluginRequest pluginRequest;

    public JobCompletionRequestExecutor(JobCompletionRequest jobCompletionRequest, ECSTasks agentInstances, PluginRequest pluginRequest) {
        this.jobCompletionRequest = jobCompletionRequest;
        this.agentInstances = agentInstances;
        this.pluginRequest = pluginRequest;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        PluginSettings clusterProfileProperties = jobCompletionRequest.clusterProfileProperties();
        String elasticAgentId = jobCompletionRequest.getElasticAgentId();
        Agents agents = pluginRequest.listAgents();

        if (!agents.agentIds().contains(elasticAgentId)) {
            LOG.debug("[Job Completion] Skipping request to delete agent with id '{}' as the agent does not exist on the server.", elasticAgentId);
            return DefaultGoPluginApiResponse.success("");
        }

        Agent agent = new Agent(elasticAgentId);

        LOG.debug("[Job Completion] Disabling elastic agent with id {} on job completion {}.", agent.elasticAgentId(), jobCompletionRequest.jobIdentifier());
        pluginRequest.disableAgents(Collections.singletonList(agent));

        LOG.debug("[Job Completion] Terminating elastic agent with id {} on job completion {}.", agent.elasticAgentId(), jobCompletionRequest.jobIdentifier());
        agentInstances.terminate(agent.elasticAgentId(), clusterProfileProperties);

        LOG.debug("[Job Completion] Deleting elastic agent with id {} on job completion {}.", agent.elasticAgentId(), jobCompletionRequest.jobIdentifier());
        pluginRequest.deleteAgents(Collections.singletonList(agent));
        return DefaultGoPluginApiResponse.success("");
    }
}
