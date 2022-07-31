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

package com.thoughtworks.gocd.elasticagent.ecs;

import com.thoughtworks.gocd.elasticagent.ecs.domain.ConsoleLogAppender;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;

import java.util.Optional;


public interface AgentInstances<T> {
    Optional<ECSTask> create(CreateAgentRequest request, PluginSettings settings, ConsoleLogAppender consoleLogAppender) throws Exception;

    void terminate(String agentId, PluginSettings settings) throws Exception;

    void terminateUnregisteredInstances(PluginSettings settings, Agents agents) throws Exception;

    Agents instancesCreatedAfterTimeout(PluginSettings settings, Agents agents);

    void refreshAll(PluginSettings pluginRequest) throws Exception;

    T find(String agentId);

    T findByJobIdentifier(JobIdentifier jobIdentifier);
}

