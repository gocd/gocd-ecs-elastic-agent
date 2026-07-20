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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.gocd.elasticagent.ecs.Constants;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTask;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.InstanceSelectionStrategyFactory;
import com.thoughtworks.gocd.elasticagent.ecs.domain.*;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ContainerFailedToRegisterException;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ContainerInstanceFailedToRegisterException;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.LimitExceededException;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import org.apache.commons.lang3.Strings;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.*;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.getServerId;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static java.text.MessageFormat.format;
import static java.util.Optional.empty;

public class TaskHelper {
    private static final Logger LOG = Logger.getLoggerFor(TaskHelper.class);

    private final ContainerInstanceHelper containerInstanceHelper;
    private final RegisterTaskDefinitionRequestBuilder registerTaskDefinitionRequestBuilder;
    private final InstanceSelectionStrategyFactory instanceSelectionStrategyFactory;
    private final SpotInstanceService spotInstanceService;

    public TaskHelper() {
        this(new ContainerInstanceHelper(), new RegisterTaskDefinitionRequestBuilder(), new InstanceSelectionStrategyFactory(), SpotInstanceService.instance());
    }

    TaskHelper(ContainerInstanceHelper containerInstanceHelper, RegisterTaskDefinitionRequestBuilder registerTaskDefinitionRequestBuilder,
               InstanceSelectionStrategyFactory instanceSelectionStrategyFactory, SpotInstanceService spotInstanceService) {
        this.containerInstanceHelper = containerInstanceHelper;
        this.registerTaskDefinitionRequestBuilder = registerTaskDefinitionRequestBuilder;
        this.instanceSelectionStrategyFactory = instanceSelectionStrategyFactory;
        this.spotInstanceService = spotInstanceService;
    }

    public Optional<ECSTask> create(CreateAgentRequest createAgentRequest, PluginSettings pluginSettings, ConsoleLogAppender consoleLogAppender) throws ContainerInstanceFailedToRegisterException, LimitExceededException, ContainerFailedToRegisterException {
        final String taskName = "GoCD" + UUID.randomUUID().toString().replace("-", "");

        final ElasticAgentProfileProperties elasticAgentProfileProperties = createAgentRequest.elasticProfile();

        ContainerDefinitionBuilder containerDefinitionBuilder = new ContainerDefinitionBuilder(createAgentRequest);

        StopPolicy stopPolicy = elasticAgentProfileProperties.platform() == LINUX ? pluginSettings.getLinuxStopPolicy() : pluginSettings.getWindowsStopPolicy();
        Optional<ContainerInstance> containerInstance = instanceSelectionStrategyFactory
                .strategyFor(stopPolicy)
                .instanceForScheduling(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder.buildPlacementRequirement());

        if (containerInstance.isEmpty()) {
            consoleLogAppender.accept("No running instance(s) found to build the ECS Task to perform current job.");
            LOG.info(format("[create-agent] No running instances found to build container with profile {0}", createAgentRequest.elasticProfile().toJson()));
            if (elasticAgentProfileProperties.runAsSpotInstance()) {
                spotInstanceService.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);
            } else {
                containerInstance = Optional.of(containerInstanceHelper.startOrCreateOneInstance(pluginSettings, elasticAgentProfileProperties, consoleLogAppender));
            }
        } else {
            consoleLogAppender.accept("Found existing running container instance platform matching ECS Task instance configuration. Not starting a new EC2 instance...");
        }
        if (containerInstance.isEmpty()) {
            return empty();
        }

        final RegisterTaskDefinitionRequest registerTaskDefinitionRequest = registerTaskDefinitionRequestBuilder
                .build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder.name(taskName)
                        .pluginSettings(pluginSettings)
                        .serverId(getServerId())
                        .build(), taskName);

        consoleLogAppender.accept("Registering ECS Task definition with cluster...");
        LOG.debug(format("[create-agent] Registering task definition: {0} ", registerTaskDefinitionRequest.toString()));
        RegisterTaskDefinitionResponse taskDefinitionResult = pluginSettings.ecsClient().registerTaskDefinition(registerTaskDefinitionRequest);
        consoleLogAppender.accept("Done registering ECS Task definition with cluster.");
        LOG.debug("[create-agent] Done registering task definition");

        TaskDefinition taskDefinitionFromNewTask = taskDefinitionResult.taskDefinition();
        StartTaskRequest startTaskRequest = StartTaskRequest.builder()
                .taskDefinition(taskDefinitionFromNewTask.taskDefinitionArn())
                .containerInstances(containerInstance.get().containerInstanceArn())
                .cluster(pluginSettings.getClusterName())
                .build();

        consoleLogAppender.accept("Starting ECS Task to perform current job...");
        LOG.debug(format("[create-agent] Starting task : {0} ", startTaskRequest.toString()));
        StartTaskResponse startTaskResult = pluginSettings.ecsClient().startTask(startTaskRequest);
        LOG.debug("[create-agent] Done executing start task request.");

        if (isStarted(startTaskResult)) {
            String message = elasticAgentProfileProperties.runAsSpotInstance() ?
                    "[WARNING] The ECS task is scheduled on a Spot Instance. A spot instance termination would re-schedule the job."
                    : String.format("ECS Task %s scheduled on container instance %s.", taskName, containerInstance.get().ec2InstanceId());

            consoleLogAppender.accept(message);

            LOG.info(format("[create-agent] Task {0} scheduled on container instance {1}", taskName, containerInstance.get().ec2InstanceId()));
            return Optional.of(new ECSTask(startTaskResult.tasks().getFirst(), taskDefinitionFromNewTask, elasticAgentProfileProperties, createAgentRequest.getJobIdentifier(), createAgentRequest.environment(), containerInstance.get().ec2InstanceId()));
        } else {
            cleanupTaskDefinition(pluginSettings, taskDefinitionFromNewTask.taskDefinitionArn());
            String errors = startTaskResult.failures().stream().map(failure -> "    " + failure.arn() + " failed with reason :" + failure.reason()).collect(Collectors.joining("\n"));
            throw new ContainerFailedToRegisterException("Fail to start task " + taskName + ":\n" + errors);
        }
    }

    public void stopAndCleanupTask(PluginSettings pluginSettings, ECSTask task) {
        pluginSettings.ecsClient().stopTask(
                StopTaskRequest.builder()
                        .cluster(pluginSettings.getClusterName())
                        .task(task.taskArn())
                        .reason("Stopped by GoCD server.")
                        .build()
        );
        cleanupTaskDefinition(pluginSettings, task.taskDefinitionArn());
    }

    public void cleanupTaskDefinition(PluginSettings settings, String taskDefinitionArn) {
        settings.ecsClient().deregisterTaskDefinition(DeregisterTaskDefinitionRequest.builder().taskDefinition(taskDefinitionArn).build());
        settings.ecsClient().deleteTaskDefinitions(DeleteTaskDefinitionsRequest.builder().taskDefinitions(taskDefinitionArn).build());
    }

    public Map<Task, TaskDefinition> listAllTasks(PluginSettings settings) {
        String clusterName = settings.getClusterName();

        EcsClient ecsClient = settings.ecsClient();

        List<String> taskArns = ecsClient.listTasks(ListTasksRequest.builder().cluster(clusterName).build()).taskArns();

        if (taskArns.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Task> tasks = ecsClient.describeTasks(DescribeTasksRequest.builder().tasks(taskArns).cluster(clusterName).build()).tasks();

        return tasks.stream().collect(Collectors.toMap(
                task -> task,
                task -> ecsClient.describeTaskDefinition(DescribeTaskDefinitionRequest.builder().taskDefinition(task.taskDefinitionArn()).build()).taskDefinition()
        ));
    }

    public List<ECSContainer> allRunningContainers(PluginSettings settings) {
        String clusterName = settings.getClusterName();

        EcsClient ecsClient = settings.ecsClient();

        List<String> taskArns = ecsClient.listTasks(ListTasksRequest.builder()
                .cluster(clusterName)
                .desiredStatus(DesiredStatus.RUNNING)
                .build()).taskArns();

        if (taskArns.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> tasks = ecsClient.describeTasks(DescribeTasksRequest.builder().tasks(taskArns).cluster(clusterName).build()).tasks();

        return tasks.stream().map(t -> {
            final TaskDefinition taskDefinition = ecsClient.describeTaskDefinition(DescribeTaskDefinitionRequest.builder().taskDefinition(t.taskDefinitionArn()).build()).taskDefinition();
            return new ECSContainer(t, taskDefinition);
        }).collect(Collectors.toList());
    }

    public Optional<Task> refreshTask(PluginSettings settings, String taskArn) {
        String clusterName = settings.getClusterName();
        EcsClient ecsClient = settings.ecsClient();

        List<Task> tasks = ecsClient.describeTasks(DescribeTasksRequest.builder().tasks(taskArn).cluster(clusterName).build()).tasks();

        if (tasks.isEmpty()) {
            return empty();
        }

        return Optional.of(tasks.getFirst());
    }

    public Optional<ECSTask> fromTaskInfo(Task task, TaskDefinition taskDefinition, Map<String, String> arnToInstanceId, String serverId) {
        List<ContainerDefinition> containerDefinitions = taskDefinition.containerDefinitions();
        Map<String, String> labels = containerDefinitions.getFirst().dockerLabels();

        // Both labels are stamped on every task definition this plugin registers, so anything without them
        // (e.g. non-plugin tasks running in a shared cluster) must not be adopted, let alone cleaned up.
        if (!PLUGIN_ID.equals(labels.get(CREATED_BY_LABEL_KEY))) {
            LOG.debug(MessageFormat.format("Ignoring task {0} as it was not created by this plugin.", task.taskArn()));
            return empty();
        }

        if (!Strings.CI.equals(labels.get(Constants.LABEL_SERVER_ID), serverId)) {
            LOG.debug(MessageFormat.format("Ignoring task {0} as server id({1}) does not match with {2}", task.taskArn(), labels.get(LABEL_SERVER_ID), serverId));
            return empty();
        }

        final String instanceId = arnToInstanceId.get(task.containerInstanceArn());

        ElasticAgentProfileProperties elasticAgentProfileProperties = ElasticAgentProfileProperties.fromJson(labels.get(CONFIGURATION_LABEL_KEY));
        JobIdentifier jobIdentifier = JobIdentifier.fromJson(labels.get(LABEL_JOB_IDENTIFIER));
        String env = labels.get(ENVIRONMENT_LABEL_KEY);

        return Optional.of(new ECSTask(task, taskDefinition, elasticAgentProfileProperties, jobIdentifier, env, instanceId));
    }

    private boolean isStarted(StartTaskResponse startTaskResult) {
        return startTaskResult.failures().isEmpty() && !startTaskResult.tasks().isEmpty();
    }
}
