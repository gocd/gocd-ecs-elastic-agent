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

import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.AgentInstances;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTask;
import com.thoughtworks.gocd.elasticagent.ecs.PluginRequest;
import com.thoughtworks.gocd.elasticagent.ecs.RequestExecutor;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ConsoleLogAppender;
import com.thoughtworks.gocd.elasticagent.ecs.events.Event;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventFingerprint;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.LimitExceededException;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import org.joda.time.LocalTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static java.text.MessageFormat.format;

public class CreateAgentRequestExecutor implements RequestExecutor {
    private static final DateTimeFormatter MESSAGE_PREFIX_FORMATTER = DateTimeFormat.forPattern("'##|'HH:mm:ss.SSS '[go]'");
    private final AgentInstances<ECSTask> agentInstances;
    private final PluginRequest pluginRequest;
    private final EventStream eventStream;
    private final CreateAgentRequest request;

    public CreateAgentRequestExecutor(CreateAgentRequest request, AgentInstances<ECSTask> agentInstances, PluginRequest pluginRequest, EventStream eventStream) {
        this.request = request;
        this.agentInstances = agentInstances;
        this.pluginRequest = pluginRequest;
        this.eventStream = eventStream;
    }

    @Override
    public GoPluginApiResponse execute() {
        ConsoleLogAppender consoleLogAppender = text -> {
            final String message = String.format("%s %s\n", LocalTime.now().toString(MESSAGE_PREFIX_FORMATTER), text);
            pluginRequest.appendToConsoleLog(request.getJobIdentifier(), message);
        };

        try {
            consoleLogAppender.accept(String.format("Received a request to create an agent for the job: [%s]", request.getJobIdentifier().getRepresentation()));
            LOG.info(format("[create-agent] Creating agent with profile {0}", request.elasticProfile().toJson()));

            agentInstances.create(request, request.clusterProfileProperties(), consoleLogAppender);

            LOG.info(format("[create-agent] Done creating agent for profile : {0}", request.elasticProfile().toJson()));
            eventStream.remove(EventFingerprint.forElasticProfile(request.elasticProfile()));
            eventStream.remove(EventFingerprint.forCreateEC2Instance());
        } catch (LimitExceededException e) {
            eventStream.update(Event.warningEvent(EventFingerprint.forCreateEC2Instance(), e.getMessage(), null));
            LOG.warn(e.getMessage(), e);
        } catch (Exception e) {
            eventStream.update(Event.errorEvent(EventFingerprint.forElasticProfile(request.elasticProfile()), format("Error creating agent for profile: {0}", request.elasticProfile().toJson()), e.getMessage()));
            LOG.error(format("[create-agent] Failed to create an agent for profile : {0}", request.elasticProfile().toJson()), e);
        }
        return new DefaultGoPluginApiResponse(200);
    }

}
