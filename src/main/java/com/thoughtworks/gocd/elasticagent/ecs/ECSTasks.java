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

import com.amazonaws.services.ecs.model.*;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.TaskHelper;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Agent;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ConsoleLogAppender;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.events.Event;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventFingerprint;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ServerRequestFailedException;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import org.joda.time.DateTime;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.getServerId;
import static java.text.MessageFormat.format;

public class ECSTasks implements AgentInstances<ECSTask> {
    private final Map<String, ECSTask> tasks = new ConcurrentHashMap<>();
    private final TaskHelper taskHelper;
    private final EventStream eventStream;
    private final ContainerInstanceHelper containerInstanceHelper;
    public Clock clock = Clock.DEFAULT;
    private boolean refreshed;

    public ECSTasks(TaskHelper taskHelper, ContainerInstanceHelper containerInstanceHelper, EventStream eventStream) {
        this.taskHelper = taskHelper;
        this.eventStream = eventStream;
        this.containerInstanceHelper = containerInstanceHelper;
    }

    @Override
    public Optional<ECSTask> create(CreateAgentRequest request, PluginSettings settings, ConsoleLogAppender consoleLogAppender) throws Exception {
        synchronized (request.elasticProfile().platform()) {
            final ECSTask existingTask = findByJobIdentifier(request.getJobIdentifier());
            if (existingTask != null) {
                consoleLogAppender.accept(String.format("An ECS task to perform current job is already scheduled on instance %s. Skipping current create agent request.", existingTask.getEC2InstanceId()));
                LOG.info(MessageFormat.format("Task is already scheduled on instance {0}.", existingTask.getEC2InstanceId()));
                return Optional.of(existingTask);
            }

            final Optional<ECSTask> task = taskHelper.create(request, settings, consoleLogAppender);

            task.ifPresent(this::register);

            return task;
        }
    }

    @Override
    public void terminate(String agentId, PluginSettings pluginSettings) {
        ECSTask task = tasks.get(agentId);
        try {
            if (task != null) {
                LOG.info(format("Task {0} is not null. Ensuring it has been stopped and cleaned up.", task.name()));
                taskHelper.stopAndCleanupTask(pluginSettings, task);
                if (task.getEC2InstanceId() != null && !task.getEC2InstanceId().equalsIgnoreCase("FARGATE")) {
                    LOG.info(format("Task {0} ran on an EC2 instance.", task.name()));
                    containerInstanceHelper.checkAndMarkEC2InstanceIdle(pluginSettings, task.getEC2InstanceId());
                } else {
                    LOG.info(format("Task {0} is a FARGATE task. No EC2 to clean up.", task.name()));
                }
                LOG.info(format("Task {0} is terminated.", task.name()));
            } else {
                LOG.warn(format("Requested to deregister task that does not exist {0}", agentId));
            }
            eventStream.remove(EventFingerprint.forTerminateAgent(agentId));
        } catch (ClientException | ServerException e) {
            EventFingerprint eventFingerprint = EventFingerprint.forTerminateAgent(agentId);
            eventStream.update(Event.errorEvent(eventFingerprint, format("Error terminating container with id: {0}", agentId), e.getMessage()));
            LOG.warn(format("Cannot terminate a task that does not exist {0}", task.taskDefinitionArn()));
        }
        tasks.remove(agentId);
    }

    @Override
    public void terminateUnregisteredInstances(PluginSettings settings, Agents agents) throws Exception {
        ECSTasks toTerminate = unregisteredAfterTimeout(settings, agents);

        if (toTerminate.tasks.isEmpty()) {
            return;
        }

        for (ECSTask task : toTerminate.tasks.values()) {
            terminate(task.name(), settings);
        }
    }

    private ECSTasks unregisteredAfterTimeout(PluginSettings settings, Agents knownAgents) {
        ECSTasks unregisteredContainers = new ECSTasks(taskHelper, containerInstanceHelper, eventStream);

        if (tasks.isEmpty()) {
            return unregisteredContainers;
        }

        for (ECSTask task : tasks.values()) {
            if (knownAgents.containsAgentWithId(task.name())) {
                continue;
            }

            DateTime dateTimeCreated = new DateTime(task.createdAt());

            if (clock.now().isAfter(dateTimeCreated.plus(settings.getContainerAutoregisterTimeout()))) {
                unregisteredContainers.register(task);
            }
        }

        return unregisteredContainers;
    }

    @Override
    public Agents instancesCreatedAfterTimeout(PluginSettings settings, Agents agents) {
        ArrayList<Agent> oldAgents = new ArrayList<>();
        for (Agent agent : agents.agents()) {
            ECSTask task = tasks.get(agent.elasticAgentId());
            if (task == null) {
                continue;
            }

            if (clock.now().isAfter(task.createdAt().plus(settings.getContainerAutoregisterTimeout()))) {
                oldAgents.add(agent);
            }
        }
        return new Agents(oldAgents);
    }

    @Override
    public void refreshAll(PluginSettings clusterProfileProperties) throws ServerRequestFailedException {
        try {
            if (!refreshed) {
                final List<ContainerInstance> containerInstances = containerInstanceHelper.getContainerInstances(clusterProfileProperties);
                final Map<String, String> arnToInstanceId = Util.toMap(containerInstances, ContainerInstance::getContainerInstanceArn, ContainerInstance::getEc2InstanceId);

                Map<Task, TaskDefinition> allTasks = taskHelper.listAllTasks(clusterProfileProperties);
                allTasks.forEach((task, taskDefinition) -> register(taskHelper.fromTaskInfo(task, taskDefinition, arnToInstanceId, getServerId())));
                refreshed = true;
            }
            eventStream.remove(EventFingerprint.forRefreshContainers());
        } catch (Exception e) {
            eventStream.update(Event.errorEvent(EventFingerprint.forRefreshContainers(), "Error while listing containers", e.getMessage()));
            throw e;
        }
    }

    public EventStream getEventStream() {
        return eventStream;
    }

    @Override
    public ECSTask find(String agentId) {
        return tasks.get(agentId);
    }

    @Override
    public ECSTask findByJobIdentifier(JobIdentifier jobIdentifier) {
        return tasks.values().stream().filter(task -> task.getJobIdentifier().equals(jobIdentifier)).findFirst().orElse(null);
    }

    // used by tests
    public boolean hasInstance(String agentId) {
        return tasks.containsKey(agentId);
    }

    // used by tests
    public boolean hasAnyTasks() {
        return !tasks.isEmpty();
    }

    private void register(ECSTask task) {
        tasks.put(task.name(), task);
    }

    private void register(Optional<ECSTask> task) {
        task.ifPresent(this::register);
    }
}
