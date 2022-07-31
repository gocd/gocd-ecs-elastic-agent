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

import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.*;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTask;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.InstanceSelectionStrategy;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.InstanceSelectionStrategyFactory;
import com.thoughtworks.gocd.elasticagent.ecs.domain.*;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ContainerFailedToRegisterException;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.LimitExceededException;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;

import java.util.*;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class TaskHelperTest {
    @Mock
    private PluginSettings pluginSettings;
    @Mock
    private AmazonECSClient ecsClient;
    @Mock
    private ContainerInstanceHelper containerInstanceHelper;
    @Mock
    private RegisterTaskDefinitionRequestBuilder registerTaskDefinitionRequestBuilder;
    @Mock
    private CreateAgentRequest createAgentRequest;
    @Mock
    private ElasticAgentProfileProperties elasticAgentProfileProperties;
    @Mock
    private JobIdentifier jobIdentifier;
    @Mock
    private InstanceSelectionStrategyFactory instanceSelectionStrategyFactory;
    @Mock
    private ConsoleLogAppender consoleLogAppender;
    @Mock
    private SpotInstanceService spotInstanceService;

    private TaskHelper taskHelper;

    @BeforeEach
    void setUp() {
        openMocks(this);

        when(pluginSettings.ecsClient()).thenReturn(ecsClient);
        when(pluginSettings.getClusterName()).thenReturn("Cluster-Name");

        when(createAgentRequest.elasticProfile()).thenReturn(elasticAgentProfileProperties);
        when(createAgentRequest.getJobIdentifier()).thenReturn(jobIdentifier);

        taskHelper = new TaskHelper(containerInstanceHelper, registerTaskDefinitionRequestBuilder, instanceSelectionStrategyFactory, spotInstanceService);
    }

    @Test
    void shouldCreateTaskForCreateAgentRequest() throws Exception {
        final InstanceSelectionStrategy instanceSelectionStrategy = mock(InstanceSelectionStrategy.class);
        final TaskDefinition taskDefinition = new TaskDefinition().withTaskDefinitionArn("task-definition-arn");
        final Task task = new Task().withTaskArn("task-arn").withTaskDefinitionArn(taskDefinition.getTaskDefinitionArn());
        final ContainerInstance containerInstance = new ContainerInstance().withContainerInstanceArn("container-instance-arn");
        final ArgumentCaptor<StartTaskRequest> startTaskRequestArgumentCaptor = ArgumentCaptor.forClass(StartTaskRequest.class);
        RegisterTaskDefinitionRequest registerTaskDefinitionRequest = new RegisterTaskDefinitionRequest();

        when(pluginSettings.efsDnsOrIP()).thenReturn("efs-dns-name");
        when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(true);
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(registerTaskDefinitionRequestBuilder.build(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.class))).thenReturn(registerTaskDefinitionRequest);
        when(ecsClient.registerTaskDefinition(any())).thenReturn(
                new RegisterTaskDefinitionResult().withTaskDefinition(taskDefinition)
        );
        when(ecsClient.startTask(startTaskRequestArgumentCaptor.capture())).thenReturn(new StartTaskResult().withTasks(task));
        when(instanceSelectionStrategyFactory.strategyFor(any()))
                .thenReturn(instanceSelectionStrategy);
        when(instanceSelectionStrategy.instanceForScheduling(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.class)))
                .thenReturn(Optional.of(containerInstance));
        when(elasticAgentProfileProperties.runAsSpotInstance()).thenReturn(false);

        final Optional<ECSTask> ecsTask = taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender);

        assertThat(ecsTask.isPresent()).isTrue();
        assertThat(ecsTask.get().taskArn()).isEqualTo(task.getTaskArn());
        assertThat(ecsTask.get().taskDefinition()).isEqualTo(taskDefinition);

        final StartTaskRequest startTaskRequest = startTaskRequestArgumentCaptor.getValue();
        assertThat(startTaskRequest.getCluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(startTaskRequest.getTaskDefinition()).isEqualTo(taskDefinition.getTaskDefinitionArn());
        assertThat(startTaskRequest.getContainerInstances()).contains(containerInstance.getContainerInstanceArn());

        InOrder inOrder = inOrder(consoleLogAppender);

        inOrder.verify(consoleLogAppender, times(1)).accept("Found existing running container instance platform matching ECS Task instance configuration. Not starting a new EC2 instance...");
        inOrder.verify(consoleLogAppender, times(1)).accept("Registering ECS Task definition with cluster...");
        inOrder.verify(consoleLogAppender, times(1)).accept("Done registering ECS Task definition with cluster.");
        inOrder.verify(consoleLogAppender, times(1)).accept("Starting ECS Task to perform current job...");
        inOrder.verify(consoleLogAppender, times(1)).accept(String.format("ECS Task %s scheduled on container instance %s.", registerTaskDefinitionRequest.getFamily(), containerInstance.getEc2InstanceId()));

        verifyNoMoreInteractions(consoleLogAppender);
    }

    @Test
    void shouldCreateTaskAndScaleUpIfNoMatchingContainerInstanceFound() throws Exception {
        final InstanceSelectionStrategy instanceSelectionStrategy = mock(InstanceSelectionStrategy.class);
        final TaskDefinition taskDefinition = new TaskDefinition().withTaskDefinitionArn("task-definition-arn");
        final Task task = new Task().withTaskArn("task-arn").withTaskDefinitionArn(taskDefinition.getTaskDefinitionArn());
        final ContainerInstance containerInstance = new ContainerInstance().withContainerInstanceArn("container-instance-arn");
        RegisterTaskDefinitionRequest registerTaskDefinitionRequest = new RegisterTaskDefinitionRequest();

        when(pluginSettings.efsDnsOrIP()).thenReturn("efs-dns-name");
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(ecsClient.registerTaskDefinition(any(RegisterTaskDefinitionRequest.class))).thenReturn(
                new RegisterTaskDefinitionResult().withTaskDefinition(taskDefinition)
        );
        when(registerTaskDefinitionRequestBuilder.build(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.class))).thenReturn(registerTaskDefinitionRequest);
        when(containerInstanceHelper.startOrCreateOneInstance(pluginSettings, elasticAgentProfileProperties, consoleLogAppender)).thenReturn(Optional.of(containerInstance));
        when(ecsClient.startTask(any(StartTaskRequest.class))).thenReturn(new StartTaskResult().withTasks(task));
        when(instanceSelectionStrategyFactory.strategyFor(any()))
                .thenReturn(instanceSelectionStrategy);
        when(instanceSelectionStrategy.instanceForScheduling(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.class))).thenReturn(Optional.empty());

        final Optional<ECSTask> ecsTask = taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender);

        assertThat(ecsTask.isPresent()).isTrue();
        assertThat(ecsTask.get().taskArn()).isEqualTo(task.getTaskArn());
        assertThat(ecsTask.get().taskDefinition()).isEqualTo(taskDefinition);

        verify(containerInstanceHelper).startOrCreateOneInstance(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);
        verifyNoMoreInteractions(containerInstanceHelper);

        InOrder inOrder = inOrder(consoleLogAppender);

        inOrder.verify(consoleLogAppender, times(1)).accept("No running instance(s) found to build the ECS Task to perform current job.");
        inOrder.verify(consoleLogAppender, times(1)).accept("Registering ECS Task definition with cluster...");
        inOrder.verify(consoleLogAppender, times(1)).accept("Done registering ECS Task definition with cluster.");
        inOrder.verify(consoleLogAppender, times(1)).accept("Starting ECS Task to perform current job...");
        inOrder.verify(consoleLogAppender, times(1)).accept(String.format("ECS Task %s scheduled on container instance %s.", registerTaskDefinitionRequest.getFamily(), containerInstance.getEc2InstanceId()));

        verifyNoMoreInteractions(consoleLogAppender);
    }

    @Test
    void shouldScaleUpASpotInstance_IfNotMatchingContainerInstanceFound_And_IfProfileRequiresASpotInstance() throws LimitExceededException, ContainerFailedToRegisterException {
        final InstanceSelectionStrategy instanceSelectionStrategy = mock(InstanceSelectionStrategy.class);
        final TaskDefinition taskDefinition = new TaskDefinition().withTaskDefinitionArn("task-definition-arn");
        final Task task = new Task().withTaskArn("task-arn").withTaskDefinitionArn(taskDefinition.getTaskDefinitionArn());
        final ContainerInstance containerInstance = new ContainerInstance().withContainerInstanceArn("container-instance-arn");
        RegisterTaskDefinitionRequest registerTaskDefinitionRequest = new RegisterTaskDefinitionRequest();

        when(pluginSettings.efsDnsOrIP()).thenReturn("efs-dns-name");
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(elasticAgentProfileProperties.runAsSpotInstance()).thenReturn(true);
        when(ecsClient.registerTaskDefinition(any(RegisterTaskDefinitionRequest.class))).thenReturn(
                new RegisterTaskDefinitionResult().withTaskDefinition(taskDefinition)
        );
        when(registerTaskDefinitionRequestBuilder.build(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.class))).thenReturn(registerTaskDefinitionRequest);
        when(containerInstanceHelper.startOrCreateOneInstance(pluginSettings, elasticAgentProfileProperties, consoleLogAppender)).thenReturn(Optional.of(containerInstance));
        when(ecsClient.startTask(any(StartTaskRequest.class))).thenReturn(new StartTaskResult().withTasks(task));
        when(instanceSelectionStrategyFactory.strategyFor(any()))
                .thenReturn(instanceSelectionStrategy);
        when(instanceSelectionStrategy.instanceForScheduling(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.class))).thenReturn(Optional.empty());

        final Optional<ECSTask> ecsTask = taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender);

        assertThat(ecsTask.isPresent()).isFalse();

        verify(spotInstanceService).create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);
        verifyNoMoreInteractions(containerInstanceHelper);

        InOrder inOrder = inOrder(consoleLogAppender);

        inOrder.verify(consoleLogAppender, times(1)).accept("No running instance(s) found to build the ECS Task to perform current job.");

        verifyNoMoreInteractions(consoleLogAppender);
    }

    @Test
    void shouldDeregisterTaskAndErrorOutIfFailsToCreateTask() throws Exception {
        final InstanceSelectionStrategy instanceSelectionStrategy = mock(InstanceSelectionStrategy.class);

        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(ecsClient.registerTaskDefinition(any(RegisterTaskDefinitionRequest.class))).thenReturn(
                new RegisterTaskDefinitionResult().withTaskDefinition(new TaskDefinition().withTaskDefinitionArn("task-definition-arn"))
        );

        when(registerTaskDefinitionRequestBuilder.build(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.class))).thenReturn(new RegisterTaskDefinitionRequest());
        when(instanceSelectionStrategyFactory.strategyFor(any())).thenReturn(instanceSelectionStrategy);
        when(instanceSelectionStrategy.instanceForScheduling(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.class)))
                .thenReturn(Optional.of(new ContainerInstance().withContainerInstanceArn("container-instance-arn")));

        when(ecsClient.startTask(any(StartTaskRequest.class))).thenReturn(new StartTaskResult()
                .withFailures(new Failure().withReason("Failed to start task.").withArn("task-arn"))
        );

        try {
            taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender);
            fail("Should fail");
        } catch (ContainerFailedToRegisterException e) {
            assertThat(e.getMessage()).contains("Failed to start task.");
        }

        verify(ecsClient).deregisterTaskDefinition(any(DeregisterTaskDefinitionRequest.class));
    }

    @Test
    void shouldStopAndDeregisterTask() {
        final ECSTask ecsTask = mock(ECSTask.class);

        when(ecsTask.taskArn()).thenReturn("task-arn");
        when(ecsTask.taskDefinitionArn()).thenReturn("task-definition-arn");

        taskHelper.stopAndDeregisterTask(pluginSettings, ecsTask);

        final ArgumentCaptor<StopTaskRequest> stopTaskRequestArgumentCaptor = ArgumentCaptor.forClass(StopTaskRequest.class);
        verify(ecsClient).stopTask(stopTaskRequestArgumentCaptor.capture());

        final StopTaskRequest stopTaskRequest = stopTaskRequestArgumentCaptor.getValue();
        assertThat(stopTaskRequest).isNotNull();
        assertThat(stopTaskRequest.getCluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(stopTaskRequest.getReason()).isEqualTo("Stopped by GoCD server.");
        assertThat(stopTaskRequest.getTask()).isEqualTo("task-arn");


        final ArgumentCaptor<DeregisterTaskDefinitionRequest> deregisterTaskDefinitionRequestArgumentCaptor = ArgumentCaptor.forClass(DeregisterTaskDefinitionRequest.class);
        verify(ecsClient).deregisterTaskDefinition(deregisterTaskDefinitionRequestArgumentCaptor.capture());

        final DeregisterTaskDefinitionRequest deregisterTaskDefinitionRequest = deregisterTaskDefinitionRequestArgumentCaptor.getValue();
        assertThat(deregisterTaskDefinitionRequest).isNotNull();
        assertThat(deregisterTaskDefinitionRequest.getTaskDefinition()).isEqualTo("task-definition-arn");
    }

    @Test
    void shouldDeregisterTaskDefinition() {
        taskHelper.deregisterTaskDefinition(pluginSettings, "task-definition-arn");

        final ArgumentCaptor<DeregisterTaskDefinitionRequest> deregisterTaskDefinitionRequestArgumentCaptor = ArgumentCaptor.forClass(DeregisterTaskDefinitionRequest.class);
        verify(ecsClient).deregisterTaskDefinition(deregisterTaskDefinitionRequestArgumentCaptor.capture());

        final DeregisterTaskDefinitionRequest deregisterTaskDefinitionRequest = deregisterTaskDefinitionRequestArgumentCaptor.getValue();
        assertThat(deregisterTaskDefinitionRequest).isNotNull();
        assertThat(deregisterTaskDefinitionRequest.getTaskDefinition()).isEqualTo("task-definition-arn");
    }

    @Test
    void shouldListAllTasks() {
        final Task task = new Task().withTaskArn("task-arn").withTaskDefinitionArn("task-definition-arn");
        final TaskDefinition taskDefinition = new TaskDefinition().withFamily("foo").withTaskDefinitionArn("task-definition-arn");

        final ArgumentCaptor<ListTasksRequest> listTasksRequestArgumentCaptor = ArgumentCaptor.forClass(ListTasksRequest.class);
        when(ecsClient.listTasks(listTasksRequestArgumentCaptor.capture())).thenReturn(new ListTasksResult().withTaskArns(task.getTaskArn()));

        final ArgumentCaptor<DescribeTasksRequest> describeTasksRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeTasksRequest.class);
        when(ecsClient.describeTasks(describeTasksRequestArgumentCaptor.capture())).thenReturn(new DescribeTasksResult().withTasks(task));

        final ArgumentCaptor<DescribeTaskDefinitionRequest> describeTaskDefinitionRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeTaskDefinitionRequest.class);
        when(ecsClient.describeTaskDefinition(describeTaskDefinitionRequestArgumentCaptor.capture())).thenReturn(
                new DescribeTaskDefinitionResult().withTaskDefinition(taskDefinition)
        );

        final Map<Task, TaskDefinition> taskTaskDefinitionMap = taskHelper.listAllTasks(pluginSettings);

        assertThat(taskTaskDefinitionMap).containsEntry(task, taskDefinition);
        assertThat(listTasksRequestArgumentCaptor.getValue().getCluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(describeTasksRequestArgumentCaptor.getValue().getCluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(describeTasksRequestArgumentCaptor.getValue().getTasks()).contains(task.getTaskArn());
        assertThat(describeTaskDefinitionRequestArgumentCaptor.getValue().getTaskDefinition()).contains(taskDefinition.getTaskDefinitionArn());
    }

    @Test
    void shouldListAllActiveTasks() {
        final Task task = new Task().withTaskArn("arn/task-arn").withTaskDefinitionArn("arn/task-definition-arn");
        final TaskDefinition taskDefinition = new TaskDefinition().withFamily("foo").withTaskDefinitionArn("arn/task-definition-arn");

        final ArgumentCaptor<ListTasksRequest> listTasksRequestArgumentCaptor = ArgumentCaptor.forClass(ListTasksRequest.class);
        when(ecsClient.listTasks(listTasksRequestArgumentCaptor.capture())).thenReturn(new ListTasksResult().withTaskArns(task.getTaskArn()));

        final ArgumentCaptor<DescribeTasksRequest> describeTasksRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeTasksRequest.class);
        when(ecsClient.describeTasks(describeTasksRequestArgumentCaptor.capture())).thenReturn(new DescribeTasksResult().withTasks(task));

        final ArgumentCaptor<DescribeTaskDefinitionRequest> describeTaskDefinitionRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeTaskDefinitionRequest.class);
        when(ecsClient.describeTaskDefinition(describeTaskDefinitionRequestArgumentCaptor.capture())).thenReturn(
                new DescribeTaskDefinitionResult().withTaskDefinition(taskDefinition)
        );

        final List<ECSContainer> ecsContainers = taskHelper.allRunningContainers(pluginSettings);

        assertThat(ecsContainers).hasSize(1).contains(new ECSContainer(task, taskDefinition));

        assertThat(listTasksRequestArgumentCaptor.getValue().getCluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(listTasksRequestArgumentCaptor.getValue().getDesiredStatus()).isEqualTo(DesiredStatus.RUNNING.toString());
        assertThat(describeTasksRequestArgumentCaptor.getValue().getCluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(describeTasksRequestArgumentCaptor.getValue().getTasks()).contains(task.getTaskArn());
        assertThat(describeTaskDefinitionRequestArgumentCaptor.getValue().getTaskDefinition()).contains(taskDefinition.getTaskDefinitionArn());
    }

    @Test
    void shouldCreateECSTaskFromTaskAndTaskDefinitionOnlyWhenServerIdMatches() {
        final Task task = mock(Task.class);
        final TaskDefinition taskDefinition = mock(TaskDefinition.class);
        final ContainerDefinition containerDefinition = mock(ContainerDefinition.class);
        final Map<String, String> dockerLabels = new HashMap<String, String>() {{
            put(CONFIGURATION_LABEL_KEY, "{\"Image\":\"alpine\"}");
            put(CREATED_BY_LABEL_KEY, PLUGIN_ID);
            put(ENVIRONMENT_LABEL_KEY, "environment");
            put(LABEL_SERVER_ID, "gocd-server-id");
        }};

        when(task.getTaskArn()).thenReturn("task-arn");
        when(task.getContainerInstanceArn()).thenReturn("container-instance-arn");
        when(taskDefinition.getContainerDefinitions()).thenReturn(Collections.singletonList(containerDefinition));
        when(containerDefinition.getDockerLabels()).thenReturn(dockerLabels);

        final Optional<ECSTask> ecsTask = taskHelper.fromTaskInfo(task, taskDefinition, Collections.singletonMap("container-instance-arn", "i-12345ab"), "gocd-server-id");

        assertThat(ecsTask.isPresent()).isTrue();
        assertThat(ecsTask.get().elasticProfile().getImage()).isEqualTo("alpine");
        assertThat(ecsTask.get().environment()).isEqualTo("environment");
        assertThat(ecsTask.get().getEC2InstanceId()).isEqualTo("i-12345ab");
        assertThat(ecsTask.get().taskDefinition()).isEqualTo(taskDefinition);
        assertThat(ecsTask.get().taskArn()).isEqualTo("task-arn");
    }

    @Test
    void shouldCreateECSTaskFromTaskAndTaskDefinitionWhenContainerDefinitionIsNotTaggedWithServerId() {
        final Task task = mock(Task.class);
        final TaskDefinition taskDefinition = mock(TaskDefinition.class);
        final ContainerDefinition containerDefinition = mock(ContainerDefinition.class);
        final Map<String, String> dockerLabels = new HashMap<String, String>() {{
            put(CONFIGURATION_LABEL_KEY, "{\"Image\":\"alpine\"}");
            put(CREATED_BY_LABEL_KEY, PLUGIN_ID);
            put(ENVIRONMENT_LABEL_KEY, "environment");
        }};

        when(task.getTaskArn()).thenReturn("task-arn");
        when(task.getContainerInstanceArn()).thenReturn("container-instance-arn");
        when(taskDefinition.getContainerDefinitions()).thenReturn(Collections.singletonList(containerDefinition));
        when(containerDefinition.getDockerLabels()).thenReturn(dockerLabels);

        final Optional<ECSTask> ecsTask = taskHelper.fromTaskInfo(task, taskDefinition, Collections.singletonMap("container-instance-arn", "i-12345ab"), "gocd-server-id");

        assertThat(ecsTask.isPresent()).isTrue();
        assertThat(ecsTask.get().elasticProfile().getImage()).isEqualTo("alpine");
        assertThat(ecsTask.get().environment()).isEqualTo("environment");
        assertThat(ecsTask.get().getEC2InstanceId()).isEqualTo("i-12345ab");
        assertThat(ecsTask.get().taskDefinition()).isEqualTo(taskDefinition);
        assertThat(ecsTask.get().taskArn()).isEqualTo("task-arn");
    }

    @Test
    void shouldIgnoreTaskOnlyWhenContainerDefinitionIsTaggedWithDifferentServerId() {
        final Task task = mock(Task.class);
        final TaskDefinition taskDefinition = mock(TaskDefinition.class);
        final ContainerDefinition containerDefinition = mock(ContainerDefinition.class);
        final Map<String, String> dockerLabels = new HashMap<String, String>() {{
            put(CONFIGURATION_LABEL_KEY, "{\"Image\":\"alpine\"}");
            put(CREATED_BY_LABEL_KEY, PLUGIN_ID);
            put(ENVIRONMENT_LABEL_KEY, "environment");
            put(LABEL_SERVER_ID, "unknown-server-id");
        }};

        when(task.getTaskArn()).thenReturn("task-arn");
        when(task.getContainerInstanceArn()).thenReturn("container-instance-arn");
        when(taskDefinition.getContainerDefinitions()).thenReturn(Collections.singletonList(containerDefinition));
        when(containerDefinition.getDockerLabels()).thenReturn(dockerLabels);

        final Optional<ECSTask> ecsTask = taskHelper.fromTaskInfo(task, taskDefinition, Collections.singletonMap("container-instance-arn", "i-12345ab"), "gocd-server-id");

        assertThat(ecsTask.isPresent()).isFalse();
    }

    @Test
    void shouldRefreshTask() {
        final ArgumentCaptor<DescribeTasksRequest> captor = ArgumentCaptor.forClass(DescribeTasksRequest.class);
        final Task taskFromAWS = mock(Task.class);

        when(pluginSettings.getClusterName()).thenReturn("GoCD");
        when(ecsClient.describeTasks(captor.capture())).thenReturn(new DescribeTasksResult().withTasks(taskFromAWS));

        final Optional<Task> optionalTask = taskHelper.refreshTask(pluginSettings, "foo");

        assertThat(optionalTask.isPresent()).isTrue();
        assertThat(optionalTask.get()).isEqualTo(taskFromAWS);

        final DescribeTasksRequest describeTasksRequest = captor.getValue();
        assertThat(describeTasksRequest.getCluster()).isEqualTo("GoCD");
        assertThat(describeTasksRequest.getTasks())
                .hasSize(1)
                .contains("foo");
    }
}
