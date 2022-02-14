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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.*;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTask;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.InstanceSelectionStrategyFactory;
import com.thoughtworks.gocd.elasticagent.ecs.domain.*;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ContainerFailedToRegisterException;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ContainerInstanceFailedToRegisterException;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.LimitExceededException;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;

import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.*;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.getServerId;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static java.text.MessageFormat.format;
import static java.util.Optional.empty;
import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;

public class TaskHelper {
    private final ContainerInstanceHelper containerInstanceHelper;
    private final RegisterTaskDefinitionRequestBuilder registerTaskDefinitionRequestBuilder;
    private final InstanceSelectionStrategyFactory instanceSelectionStrategyFactory;
    private SpotInstanceService spotInstanceService;

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
        final String taskName = "GoCD" + UUID.randomUUID().toString().replaceAll("-", "");

        final ElasticAgentProfileProperties elasticAgentProfileProperties = createAgentRequest.elasticProfile();

        ContainerDefinition containerDefinition = new ContainerDefinitionBuilder()
                .withName(taskName)
                .pluginSettings(pluginSettings)
                .createAgentRequest(createAgentRequest)
                .withServerId(getServerId())
                .build();

        StopPolicy stopPolicy = elasticAgentProfileProperties.platform() == LINUX ? pluginSettings.getLinuxStopPolicy() : pluginSettings.getWindowsStopPolicy();
        Optional<ContainerInstance> containerInstance = instanceSelectionStrategyFactory
                .strategyFor(stopPolicy)
                .instanceForScheduling(pluginSettings, elasticAgentProfileProperties, containerDefinition);

        if (!containerInstance.isPresent()) {
            consoleLogAppender.accept("No running instance(s) found to build the ECS Task to perform current job.");
            LOG.info(format("[create-agent] No running instances found to build container with profile {0}", createAgentRequest.elasticProfile().toJson()));
            if (elasticAgentProfileProperties.runAsSpotInstance()) {
                spotInstanceService.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);
                return Optional.empty();
            } else {
                containerInstance = containerInstanceHelper.startOrCreateOneInstance(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);
            }
        } else {
            consoleLogAppender.accept("Found existing running container instance platform matching ECS Task instance configuration. Not starting a new EC2 instance...");
        }

        final RegisterTaskDefinitionRequest registerTaskDefinitionRequest = registerTaskDefinitionRequestBuilder
                .build(pluginSettings, elasticAgentProfileProperties, containerDefinition).withFamily(taskName);

        consoleLogAppender.accept("Registering ECS Task definition with cluster...");
        LOG.debug(format("[create-agent] Registering task definition: {0} ", registerTaskDefinitionRequest.toString()));
        RegisterTaskDefinitionResult taskDefinitionResult = pluginSettings.ecsClient().registerTaskDefinition(registerTaskDefinitionRequest);
        consoleLogAppender.accept("Done registering ECS Task definition with cluster.");
        LOG.debug("[create-agent] Done registering task definition");

        TaskDefinition taskDefinitionFromNewTask = taskDefinitionResult.getTaskDefinition();
        StartTaskRequest startTaskRequest = new StartTaskRequest()
                .withTaskDefinition(taskDefinitionFromNewTask.getTaskDefinitionArn())
                .withContainerInstances(containerInstance.get().getContainerInstanceArn())
                .withCluster(pluginSettings.getClusterName());

        consoleLogAppender.accept("Starting ECS Task to perform current job...");
        LOG.debug(format("[create-agent] Starting task : {0} ", startTaskRequest.toString()));
        StartTaskResult startTaskResult = pluginSettings.ecsClient().startTask(startTaskRequest);
        LOG.debug("[create-agent] Done executing start task request.");

        if (isStarted(startTaskResult)) {
            String message = elasticAgentProfileProperties.runAsSpotInstance() ?
                    "[WARNING] The ECS task is scheduled on a Spot Instance. A spot instance termination would re-schedule the job."
                    : String.format("ECS Task %s scheduled on container instance %s.", taskName, containerInstance.get().getEc2InstanceId());

            consoleLogAppender.accept(message);

            LOG.info(format("[create-agent] Task {0} scheduled on container instance {1}", taskName, containerInstance.get().getEc2InstanceId()));
            return Optional.of(new ECSTask(startTaskResult.getTasks().get(0), taskDefinitionFromNewTask, elasticAgentProfileProperties, createAgentRequest.getJobIdentifier(), createAgentRequest.environment(), containerInstance.get().getEc2InstanceId()));
        } else {
            deregisterTaskDefinition(pluginSettings, taskDefinitionFromNewTask.getTaskDefinitionArn());
            String errors = startTaskResult.getFailures().stream().map(failure -> "    " + failure.getArn() + " failed with reason :" + failure.getReason()).collect(Collectors.joining("\n"));
            throw new ContainerFailedToRegisterException("Fail to start task " + taskName + ":\n" + errors);
        }
    }

    public void stopAndDeregisterTask(PluginSettings pluginSettings, ECSTask task) {
        pluginSettings.ecsClient().stopTask(
                new StopTaskRequest()
                        .withCluster(pluginSettings.getClusterName())
                        .withTask(task.taskArn())
                        .withReason("Stopped by GoCD server.")
        );
        deregisterTaskDefinition(pluginSettings, task.taskDefinitionArn());
    }

    public void deregisterTaskDefinition(PluginSettings settings, String taskDefinitionArn) {
        settings.ecsClient().deregisterTaskDefinition(new DeregisterTaskDefinitionRequest().withTaskDefinition(taskDefinitionArn));
    }

    public Map<Task, TaskDefinition> listAllTasks(PluginSettings settings) {
        String clusterName = settings.getClusterName();

        AmazonECS ecsClient = settings.ecsClient();

        List<String> taskArns = ecsClient.listTasks(new ListTasksRequest()
                .withCluster(clusterName)).getTaskArns();

        if (taskArns.isEmpty()) {
            return Collections.emptyMap();
        }

        List<Task> tasks = ecsClient.describeTasks(new DescribeTasksRequest()
                .withTasks(taskArns)
                .withCluster(clusterName))
                .getTasks();

        return tasks.stream().collect(Collectors.toMap(
                task -> task,
                task -> ecsClient.describeTaskDefinition(new DescribeTaskDefinitionRequest()
                        .withTaskDefinition(task.getTaskDefinitionArn()))
                        .getTaskDefinition()
        ));
    }

    public List<ECSContainer> allRunningContainers(PluginSettings settings) {
        String clusterName = settings.getClusterName();

        AmazonECS ecsClient = settings.ecsClient();

        List<String> taskArns = ecsClient.listTasks(new ListTasksRequest()
                .withCluster(clusterName)
                .withDesiredStatus(DesiredStatus.RUNNING)).getTaskArns();

        if (taskArns.isEmpty()) {
            return Collections.emptyList();
        }

        List<Task> tasks = ecsClient.describeTasks(new DescribeTasksRequest()
                .withTasks(taskArns)
                .withCluster(clusterName))
                .getTasks();

        return tasks.stream().map(t -> {
            final TaskDefinition taskDefinition = ecsClient.describeTaskDefinition(new DescribeTaskDefinitionRequest().withTaskDefinition(t.getTaskDefinitionArn())).getTaskDefinition();
            return new ECSContainer(t, taskDefinition);
        }).collect(Collectors.toList());
    }

    public Optional<Task> refreshTask(PluginSettings settings, String taskArn) {
        String clusterName = settings.getClusterName();
        AmazonECS ecsClient = settings.ecsClient();

        List<Task> tasks = ecsClient.describeTasks(new DescribeTasksRequest()
                .withTasks(taskArn)
                .withCluster(clusterName))
                .getTasks();

        if (tasks.isEmpty()) {
            return empty();
        }

        return Optional.of(tasks.get(0));
    }

    public Optional<ECSTask> fromTaskInfo(Task task, TaskDefinition taskDefinition, Map<String, String> arnToInstanceId, String serverId) {
        List<ContainerDefinition> containerDefinitions = taskDefinition.getContainerDefinitions();
        Map<String, String> labels = containerDefinitions.get(0).getDockerLabels();

        if (!equalsIgnoreCase(labels.getOrDefault(LABEL_SERVER_ID, serverId), serverId)) {
            LOG.debug(MessageFormat.format("Ignoring task {0} as server id({1}) doest not match with {2}", task.getTaskArn(), labels.get(LABEL_SERVER_ID), serverId));
            return empty();
        }

        final String instanceId = arnToInstanceId.get(task.getContainerInstanceArn());

        ElasticAgentProfileProperties elasticAgentProfileProperties = ElasticAgentProfileProperties.fromJson(labels.get(CONFIGURATION_LABEL_KEY));
        JobIdentifier jobIdentifier = JobIdentifier.fromJson(labels.get(LABEL_JOB_IDENTIFIER));
        String env = labels.get(ENVIRONMENT_LABEL_KEY);

        return Optional.of(new ECSTask(task, taskDefinition, elasticAgentProfileProperties, jobIdentifier, env, instanceId));
    }

    private boolean isStarted(StartTaskResult startTaskResult) {
        return startTaskResult.getFailures().isEmpty() && !startTaskResult.getTasks().isEmpty();
    }
}
