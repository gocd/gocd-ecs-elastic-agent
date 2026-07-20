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
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

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
    private EcsClient ecsClient;
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
        final TaskDefinition taskDefinition = TaskDefinition.builder().taskDefinitionArn("task-definition-arn").build();
        final Task task = Task.builder().taskArn("task-arn").taskDefinitionArn(taskDefinition.taskDefinitionArn()).build();
        final ContainerInstance containerInstance = ContainerInstance.builder().containerInstanceArn("container-instance-arn").build();
        final ArgumentCaptor<StartTaskRequest> startTaskRequestArgumentCaptor = ArgumentCaptor.forClass(StartTaskRequest.class);
        final ArgumentCaptor<String> taskNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        RegisterTaskDefinitionRequest registerTaskDefinitionRequest = RegisterTaskDefinitionRequest.builder().build();

        when(pluginSettings.efsDnsOrIP()).thenReturn("efs-dns-name");
        when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(true);
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(registerTaskDefinitionRequestBuilder.build(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.Builder.class), taskNameArgumentCaptor.capture())).thenReturn(registerTaskDefinitionRequest);
        when(ecsClient.registerTaskDefinition(any(RegisterTaskDefinitionRequest.class))).thenReturn(
                RegisterTaskDefinitionResponse.builder().taskDefinition(taskDefinition).build()
        );
        when(ecsClient.startTask(startTaskRequestArgumentCaptor.capture())).thenReturn(StartTaskResponse.builder().tasks(task).build());
        when(instanceSelectionStrategyFactory.strategyFor(any()))
                .thenReturn(instanceSelectionStrategy);
        when(instanceSelectionStrategy.instanceForScheduling(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinitionBuilder.PlacementRequirement.class)))
                .thenReturn(Optional.of(containerInstance));
        when(elasticAgentProfileProperties.runAsSpotInstance()).thenReturn(false);

        final Optional<ECSTask> ecsTask = taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender);

        assertThat(ecsTask.isPresent()).isTrue();
        assertThat(ecsTask.get().taskArn()).isEqualTo(task.taskArn());
        assertThat(ecsTask.get().taskDefinition()).isEqualTo(taskDefinition);

        final StartTaskRequest startTaskRequest = startTaskRequestArgumentCaptor.getValue();
        assertThat(startTaskRequest.cluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(startTaskRequest.taskDefinition()).isEqualTo(taskDefinition.taskDefinitionArn());
        assertThat(startTaskRequest.containerInstances()).contains(containerInstance.containerInstanceArn());

        InOrder inOrder = inOrder(consoleLogAppender);

        inOrder.verify(consoleLogAppender, times(1)).accept("Found existing running container instance platform matching ECS Task instance configuration. Not starting a new EC2 instance...");
        inOrder.verify(consoleLogAppender, times(1)).accept("Registering ECS Task definition with cluster...");
        inOrder.verify(consoleLogAppender, times(1)).accept("Done registering ECS Task definition with cluster.");
        inOrder.verify(consoleLogAppender, times(1)).accept("Starting ECS Task to perform current job...");
        inOrder.verify(consoleLogAppender, times(1)).accept(String.format("ECS Task %s scheduled on container instance %s.", taskNameArgumentCaptor.getValue(), containerInstance.ec2InstanceId()));

        verifyNoMoreInteractions(consoleLogAppender);
    }

    @Test
    void shouldCreateTaskAndScaleUpIfNoMatchingContainerInstanceFound() throws Exception {
        final InstanceSelectionStrategy instanceSelectionStrategy = mock(InstanceSelectionStrategy.class);
        final TaskDefinition taskDefinition = TaskDefinition.builder().taskDefinitionArn("task-definition-arn").build();
        final Task task = Task.builder().taskArn("task-arn").taskDefinitionArn(taskDefinition.taskDefinitionArn()).build();
        final ContainerInstance containerInstance = ContainerInstance.builder().containerInstanceArn("container-instance-arn").build();
        final ArgumentCaptor<String> taskNameArgumentCaptor = ArgumentCaptor.forClass(String.class);
        RegisterTaskDefinitionRequest registerTaskDefinitionRequest = RegisterTaskDefinitionRequest.builder().build();

        when(pluginSettings.efsDnsOrIP()).thenReturn("efs-dns-name");
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(ecsClient.registerTaskDefinition(any(RegisterTaskDefinitionRequest.class))).thenReturn(
                RegisterTaskDefinitionResponse.builder().taskDefinition(taskDefinition).build()
        );
        when(registerTaskDefinitionRequestBuilder.build(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.Builder.class), taskNameArgumentCaptor.capture())).thenReturn(registerTaskDefinitionRequest);
        when(containerInstanceHelper.startOrCreateOneInstance(pluginSettings, elasticAgentProfileProperties, consoleLogAppender)).thenReturn(containerInstance);
        when(ecsClient.startTask(any(StartTaskRequest.class))).thenReturn(StartTaskResponse.builder().tasks(task).build());
        when(instanceSelectionStrategyFactory.strategyFor(any()))
                .thenReturn(instanceSelectionStrategy);
        when(instanceSelectionStrategy.instanceForScheduling(eq(pluginSettings), eq(elasticAgentProfileProperties), any())).thenReturn(Optional.empty());

        final Optional<ECSTask> ecsTask = taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender);

        assertThat(ecsTask.isPresent()).isTrue();
        assertThat(ecsTask.get().taskArn()).isEqualTo(task.taskArn());
        assertThat(ecsTask.get().taskDefinition()).isEqualTo(taskDefinition);

        verify(containerInstanceHelper).startOrCreateOneInstance(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);
        verifyNoMoreInteractions(containerInstanceHelper);

        InOrder inOrder = inOrder(consoleLogAppender);

        inOrder.verify(consoleLogAppender, times(1)).accept("No running instance(s) found to build the ECS Task to perform current job.");
        inOrder.verify(consoleLogAppender, times(1)).accept("Registering ECS Task definition with cluster...");
        inOrder.verify(consoleLogAppender, times(1)).accept("Done registering ECS Task definition with cluster.");
        inOrder.verify(consoleLogAppender, times(1)).accept("Starting ECS Task to perform current job...");
        inOrder.verify(consoleLogAppender, times(1)).accept(String.format("ECS Task %s scheduled on container instance %s.", taskNameArgumentCaptor.getValue(), containerInstance.ec2InstanceId()));

        verifyNoMoreInteractions(consoleLogAppender);
    }

    @Test
    void shouldScaleUpASpotInstance_IfNotMatchingContainerInstanceFound_And_IfProfileRequiresASpotInstance() throws LimitExceededException, ContainerFailedToRegisterException {
        final InstanceSelectionStrategy instanceSelectionStrategy = mock(InstanceSelectionStrategy.class);
        final TaskDefinition taskDefinition = TaskDefinition.builder().taskDefinitionArn("task-definition-arn").build();
        final Task task = Task.builder().taskArn("task-arn").taskDefinitionArn(taskDefinition.taskDefinitionArn()).build();
        final ContainerInstance containerInstance = ContainerInstance.builder().containerInstanceArn("container-instance-arn").build()  ;
        RegisterTaskDefinitionRequest registerTaskDefinitionRequest = RegisterTaskDefinitionRequest.builder().build();

        when(pluginSettings.efsDnsOrIP()).thenReturn("efs-dns-name");
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(elasticAgentProfileProperties.runAsSpotInstance()).thenReturn(true);
        when(ecsClient.registerTaskDefinition(any(RegisterTaskDefinitionRequest.class))).thenReturn(
                RegisterTaskDefinitionResponse.builder().taskDefinition(taskDefinition).build()
        );
        when(registerTaskDefinitionRequestBuilder.build(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.Builder.class), any())).thenReturn(registerTaskDefinitionRequest);
        when(containerInstanceHelper.startOrCreateOneInstance(pluginSettings, elasticAgentProfileProperties, consoleLogAppender)).thenReturn(containerInstance);
        when(ecsClient.startTask(any(StartTaskRequest.class))).thenReturn(StartTaskResponse.builder().tasks(task).build());
        when(instanceSelectionStrategyFactory.strategyFor(any()))
                .thenReturn(instanceSelectionStrategy);
        when(instanceSelectionStrategy.instanceForScheduling(eq(pluginSettings), eq(elasticAgentProfileProperties), any())).thenReturn(Optional.empty());

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
                RegisterTaskDefinitionResponse.builder().taskDefinition(TaskDefinition.builder().taskDefinitionArn("task-definition-arn").build()).build()
        );

        when(registerTaskDefinitionRequestBuilder.build(eq(pluginSettings), eq(elasticAgentProfileProperties), any(ContainerDefinition.Builder.class), any())).thenReturn(RegisterTaskDefinitionRequest.builder().build());
        when(instanceSelectionStrategyFactory.strategyFor(any())).thenReturn(instanceSelectionStrategy);
        when(instanceSelectionStrategy.instanceForScheduling(eq(pluginSettings), eq(elasticAgentProfileProperties), any()))
                .thenReturn(Optional.of(ContainerInstance.builder().containerInstanceArn("container-instance-arn").build()));

        when(ecsClient.startTask(any(StartTaskRequest.class))).thenReturn(StartTaskResponse.builder()
                .failures(Failure.builder().reason("Failed to start task.").arn("task-arn").build())
                .build()
        );

        try {
            taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender);
            fail("Should fail");
        } catch (ContainerFailedToRegisterException e) {
            assertThat(e.getMessage()).contains("Failed to start task.");
        }

        verify(ecsClient).deregisterTaskDefinition(any(DeregisterTaskDefinitionRequest.class));
        verify(ecsClient).deleteTaskDefinitions(any(DeleteTaskDefinitionsRequest.class));
    }

    @Test
    void shouldStopAndCleanupTask() {
        final ECSTask ecsTask = mock(ECSTask.class);

        when(ecsTask.taskArn()).thenReturn("task-arn");
        when(ecsTask.taskDefinitionArn()).thenReturn("task-definition-arn");

        taskHelper.stopAndCleanupTask(pluginSettings, ecsTask);

        final ArgumentCaptor<StopTaskRequest> stopTaskRequestArgumentCaptor = ArgumentCaptor.forClass(StopTaskRequest.class);
        verify(ecsClient).stopTask(stopTaskRequestArgumentCaptor.capture());

        final StopTaskRequest stopTaskRequest = stopTaskRequestArgumentCaptor.getValue();
        assertThat(stopTaskRequest).isNotNull();
        assertThat(stopTaskRequest.cluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(stopTaskRequest.reason()).isEqualTo("Stopped by GoCD server.");
        assertThat(stopTaskRequest.task()).isEqualTo("task-arn");

        final ArgumentCaptor<DeregisterTaskDefinitionRequest> deregisterTaskDefinitionRequestArgumentCaptor = ArgumentCaptor.forClass(DeregisterTaskDefinitionRequest.class);
        verify(ecsClient).deregisterTaskDefinition(deregisterTaskDefinitionRequestArgumentCaptor.capture());

        final DeregisterTaskDefinitionRequest deregisterTaskDefinitionRequest = deregisterTaskDefinitionRequestArgumentCaptor.getValue();
        assertThat(deregisterTaskDefinitionRequest).isNotNull();
        assertThat(deregisterTaskDefinitionRequest.taskDefinition()).isEqualTo("task-definition-arn");

        final ArgumentCaptor<DeleteTaskDefinitionsRequest> deleteTaskDefinitionsCaptor = ArgumentCaptor.forClass(DeleteTaskDefinitionsRequest.class);
        verify(ecsClient).deleteTaskDefinitions(deleteTaskDefinitionsCaptor.capture());

        final DeleteTaskDefinitionsRequest deleteTaskDefinitionRequest = deleteTaskDefinitionsCaptor.getValue();
        assertThat(deleteTaskDefinitionRequest).isNotNull();
        assertThat(deleteTaskDefinitionRequest.taskDefinitions()).containsExactly("task-definition-arn");
    }

    @Test
    void shouldCleanupTaskDefinition() {
        taskHelper.cleanupTaskDefinition(pluginSettings, "task-definition-arn");

        final ArgumentCaptor<DeregisterTaskDefinitionRequest> deregisterTaskDefinitionRequestArgumentCaptor = ArgumentCaptor.forClass(DeregisterTaskDefinitionRequest.class);
        verify(ecsClient).deregisterTaskDefinition(deregisterTaskDefinitionRequestArgumentCaptor.capture());

        final DeregisterTaskDefinitionRequest deregisterTaskDefinitionRequest = deregisterTaskDefinitionRequestArgumentCaptor.getValue();
        assertThat(deregisterTaskDefinitionRequest).isNotNull();
        assertThat(deregisterTaskDefinitionRequest.taskDefinition()).isEqualTo("task-definition-arn");

        final ArgumentCaptor<DeleteTaskDefinitionsRequest> deleteTaskDefinitionsCaptor = ArgumentCaptor.forClass(DeleteTaskDefinitionsRequest.class);
        verify(ecsClient).deleteTaskDefinitions(deleteTaskDefinitionsCaptor.capture());

        final DeleteTaskDefinitionsRequest deleteTaskDefinitionRequest = deleteTaskDefinitionsCaptor.getValue();
        assertThat(deleteTaskDefinitionRequest).isNotNull();
        assertThat(deleteTaskDefinitionRequest.taskDefinitions()).containsExactly("task-definition-arn");
    }

    @Test
    void shouldListAllTasks() {
        final Task task = Task.builder().taskArn("task-arn").taskDefinitionArn("task-definition-arn").build();
        final TaskDefinition taskDefinition = TaskDefinition.builder().family("foo").taskDefinitionArn("task-definition-arn").build();

        final ArgumentCaptor<ListTasksRequest> listTasksRequestArgumentCaptor = ArgumentCaptor.forClass(ListTasksRequest.class);
        when(ecsClient.listTasks(listTasksRequestArgumentCaptor.capture())).thenReturn(ListTasksResponse.builder().taskArns(task.taskArn()).build());

        final ArgumentCaptor<DescribeTasksRequest> describeTasksRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeTasksRequest.class);
        when(ecsClient.describeTasks(describeTasksRequestArgumentCaptor.capture())).thenReturn(DescribeTasksResponse.builder().tasks(task).build());

        final ArgumentCaptor<DescribeTaskDefinitionRequest> describeTaskDefinitionRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeTaskDefinitionRequest.class);
        when(ecsClient.describeTaskDefinition(describeTaskDefinitionRequestArgumentCaptor.capture())).thenReturn(
                DescribeTaskDefinitionResponse.builder().taskDefinition(taskDefinition).build()
        );

        final Map<Task, TaskDefinition> taskTaskDefinitionMap = taskHelper.listAllTasks(pluginSettings);

        assertThat(taskTaskDefinitionMap).containsEntry(task, taskDefinition);
        assertThat(listTasksRequestArgumentCaptor.getValue().cluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(describeTasksRequestArgumentCaptor.getValue().cluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(describeTasksRequestArgumentCaptor.getValue().tasks()).contains(task.taskArn());
        assertThat(describeTaskDefinitionRequestArgumentCaptor.getValue().taskDefinition()).contains(taskDefinition.taskDefinitionArn());
    }

    @Test
    void shouldListAllActiveTasks() {
        final Task task = Task.builder().taskArn("arn/task-arn").taskDefinitionArn("arn/task-definition-arn").build();
        final TaskDefinition taskDefinition = TaskDefinition.builder().family("foo").taskDefinitionArn("arn/task-definition-arn").build();

        final ArgumentCaptor<ListTasksRequest> listTasksRequestArgumentCaptor = ArgumentCaptor.forClass(ListTasksRequest.class);
        when(ecsClient.listTasks(listTasksRequestArgumentCaptor.capture())).thenReturn(ListTasksResponse.builder().taskArns(task.taskArn()).build());

        final ArgumentCaptor<DescribeTasksRequest> describeTasksRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeTasksRequest.class);
        when(ecsClient.describeTasks(describeTasksRequestArgumentCaptor.capture())).thenReturn(DescribeTasksResponse.builder().tasks(task).build());

        final ArgumentCaptor<DescribeTaskDefinitionRequest> describeTaskDefinitionRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeTaskDefinitionRequest.class);
        when(ecsClient.describeTaskDefinition(describeTaskDefinitionRequestArgumentCaptor.capture())).thenReturn(
                DescribeTaskDefinitionResponse.builder().taskDefinition(taskDefinition).build()
        );

        final List<ECSContainer> ecsContainers = taskHelper.allRunningContainers(pluginSettings);

        assertThat(ecsContainers).hasSize(1).contains(new ECSContainer(task, taskDefinition));

        assertThat(listTasksRequestArgumentCaptor.getValue().cluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(listTasksRequestArgumentCaptor.getValue().desiredStatus()).isEqualTo(DesiredStatus.RUNNING);
        assertThat(describeTasksRequestArgumentCaptor.getValue().cluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(describeTasksRequestArgumentCaptor.getValue().tasks()).contains(task.taskArn());
        assertThat(describeTaskDefinitionRequestArgumentCaptor.getValue().taskDefinition()).contains(taskDefinition.taskDefinitionArn());
    }

    @Test
    void shouldCreateECSTaskFromTaskAndTaskDefinitionOnlyWhenServerIdMatches() {
        final Task task = mock(Task.class);
        final TaskDefinition taskDefinition = mock(TaskDefinition.class);
        final ContainerDefinition containerDefinition = mock(ContainerDefinition.class);
        final Map<String, String> dockerLabels = new HashMap<>() {{
            put(CONFIGURATION_LABEL_KEY, "{\"Image\":\"alpine\"}");
            put(CREATED_BY_LABEL_KEY, PLUGIN_ID);
            put(ENVIRONMENT_LABEL_KEY, "environment");
            put(LABEL_SERVER_ID, "gocd-server-id");
        }};

        when(task.taskArn()).thenReturn("task-arn");
        when(task.containerInstanceArn()).thenReturn("container-instance-arn");
        when(taskDefinition.containerDefinitions()).thenReturn(Collections.singletonList(containerDefinition));
        when(containerDefinition.dockerLabels()).thenReturn(dockerLabels);

        final Optional<ECSTask> ecsTask = taskHelper.fromTaskInfo(task, taskDefinition, Collections.singletonMap("container-instance-arn", "i-12345ab"), "gocd-server-id");

        assertThat(ecsTask.isPresent()).isTrue();
        assertThat(ecsTask.get().elasticProfile().getImage()).isEqualTo("alpine");
        assertThat(ecsTask.get().environment()).isEqualTo("environment");
        assertThat(ecsTask.get().getEC2InstanceId()).isEqualTo("i-12345ab");
        assertThat(ecsTask.get().taskDefinition()).isEqualTo(taskDefinition);
        assertThat(ecsTask.get().taskArn()).isEqualTo("task-arn");
    }

    @Test
    void shouldIgnoreTaskWhenContainerDefinitionIsNotTaggedWithServerId() {
        final Task task = mock(Task.class);
        final TaskDefinition taskDefinition = mock(TaskDefinition.class);
        final ContainerDefinition containerDefinition = mock(ContainerDefinition.class);
        final Map<String, String> dockerLabels = new HashMap<>() {{
            put(CONFIGURATION_LABEL_KEY, "{\"Image\":\"alpine\"}");
            put(CREATED_BY_LABEL_KEY, PLUGIN_ID);
            put(ENVIRONMENT_LABEL_KEY, "environment");
        }};

        when(task.taskArn()).thenReturn("task-arn");
        when(taskDefinition.containerDefinitions()).thenReturn(Collections.singletonList(containerDefinition));
        when(containerDefinition.dockerLabels()).thenReturn(dockerLabels);

        final Optional<ECSTask> ecsTask = taskHelper.fromTaskInfo(task, taskDefinition, Collections.singletonMap("container-instance-arn", "i-12345ab"), "gocd-server-id");

        assertThat(ecsTask.isPresent()).isFalse();
    }

    @Test
    void shouldIgnoreTaskWhenContainerDefinitionIsNotTaggedAsCreatedByThisPlugin() {
        final Task task = mock(Task.class);
        final TaskDefinition taskDefinition = mock(TaskDefinition.class);
        final ContainerDefinition containerDefinition = mock(ContainerDefinition.class);
        final Map<String, String> dockerLabels = new HashMap<>() {{
            put(CONFIGURATION_LABEL_KEY, "{\"Image\":\"alpine\"}");
            put(ENVIRONMENT_LABEL_KEY, "environment");
            put(LABEL_SERVER_ID, "gocd-server-id");
        }};

        when(task.taskArn()).thenReturn("task-arn");
        when(taskDefinition.containerDefinitions()).thenReturn(Collections.singletonList(containerDefinition));
        when(containerDefinition.dockerLabels()).thenReturn(dockerLabels);

        final Optional<ECSTask> ecsTask = taskHelper.fromTaskInfo(task, taskDefinition, Collections.singletonMap("container-instance-arn", "i-12345ab"), "gocd-server-id");

        assertThat(ecsTask.isPresent()).isFalse();
    }

    @Test
    void shouldIgnoreForeignTaskWithoutAnyDockerLabels() {
        final Task task = mock(Task.class);
        final TaskDefinition taskDefinition = mock(TaskDefinition.class);
        final ContainerDefinition containerDefinition = ContainerDefinition.builder().build();

        when(task.taskArn()).thenReturn("task-arn");
        when(taskDefinition.containerDefinitions()).thenReturn(Collections.singletonList(containerDefinition));

        final Optional<ECSTask> ecsTask = taskHelper.fromTaskInfo(task, taskDefinition, Collections.singletonMap("container-instance-arn", "i-12345ab"), "gocd-server-id");

        assertThat(ecsTask.isPresent()).isFalse();
    }

    @Test
    void shouldIgnoreTaskOnlyWhenContainerDefinitionIsTaggedWithDifferentServerId() {
        final Task task = mock(Task.class);
        final TaskDefinition taskDefinition = mock(TaskDefinition.class);
        final ContainerDefinition containerDefinition = mock(ContainerDefinition.class);
        final Map<String, String> dockerLabels = new HashMap<>() {{
            put(CONFIGURATION_LABEL_KEY, "{\"Image\":\"alpine\"}");
            put(CREATED_BY_LABEL_KEY, PLUGIN_ID);
            put(ENVIRONMENT_LABEL_KEY, "environment");
            put(LABEL_SERVER_ID, "unknown-server-id");
        }};

        when(task.taskArn()).thenReturn("task-arn");
        when(task.containerInstanceArn()).thenReturn("container-instance-arn");
        when(taskDefinition.containerDefinitions()).thenReturn(Collections.singletonList(containerDefinition));
        when(containerDefinition.dockerLabels()).thenReturn(dockerLabels);

        final Optional<ECSTask> ecsTask = taskHelper.fromTaskInfo(task, taskDefinition, Collections.singletonMap("container-instance-arn", "i-12345ab"), "gocd-server-id");

        assertThat(ecsTask.isPresent()).isFalse();
    }

    @Test
    void shouldRefreshTask() {
        final ArgumentCaptor<DescribeTasksRequest> captor = ArgumentCaptor.forClass(DescribeTasksRequest.class);
        final Task taskFromAWS = mock(Task.class);

        when(pluginSettings.getClusterName()).thenReturn("GoCD");
        when(ecsClient.describeTasks(captor.capture())).thenReturn(DescribeTasksResponse.builder().tasks(taskFromAWS).build());

        final Optional<Task> optionalTask = taskHelper.refreshTask(pluginSettings, "foo");

        assertThat(optionalTask.isPresent()).isTrue();
        assertThat(optionalTask.get()).isEqualTo(taskFromAWS);

        final DescribeTasksRequest describeTasksRequest = captor.getValue();
        assertThat(describeTasksRequest.cluster()).isEqualTo("GoCD");
        assertThat(describeTasksRequest.tasks())
                .hasSize(1)
                .contains("foo");
    }
}
