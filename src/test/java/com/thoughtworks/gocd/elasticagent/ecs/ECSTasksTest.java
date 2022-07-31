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

import com.amazonaws.services.ecs.model.Task;
import com.amazonaws.services.ecs.model.TaskDefinition;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.TaskHelper;
import com.thoughtworks.gocd.elasticagent.ecs.domain.*;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ECSTasksTest {

    private TaskHelper taskHelper;
    private EventStream eventStream;
    private PluginSettings pluginSettings;
    private CreateAgentRequest createAgentRequest;
    private ContainerInstanceHelper containerInstanceHelper;
    private ECSTasks ecsTasks;
    private ConsoleLogAppender consoleLogAppender;

    @BeforeEach
    void setUp() {
        final ElasticAgentProfileProperties elasticAgentProfileProperties = mock(ElasticAgentProfileProperties.class);
        taskHelper = mock(TaskHelper.class);
        eventStream = mock(EventStream.class);
        pluginSettings = mock(PluginSettings.class);
        createAgentRequest = mock(CreateAgentRequest.class);
        containerInstanceHelper = mock(ContainerInstanceHelper.class);
        consoleLogAppender = mock(ConsoleLogAppender.class);
        ecsTasks = new ECSTasks(taskHelper, containerInstanceHelper, eventStream);

        when(elasticAgentProfileProperties.platform()).thenReturn(Platform.LINUX);
        when(createAgentRequest.elasticProfile()).thenReturn(elasticAgentProfileProperties);

        when(pluginSettings.getContainerAutoregisterTimeout()).thenReturn(new Period().withMinutes(2));
    }

    @Test
    void shouldCreateAnAgent() throws Exception {
        final ECSTask task = mock(ECSTask.class);

        when(task.name()).thenReturn("agent-id");
        when(taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender)).thenReturn(Optional.of(task));

        assertThat(ecsTasks.hasInstance(task.name())).isFalse();

        final Optional<ECSTask> ecsTask = ecsTasks.create(createAgentRequest, pluginSettings, consoleLogAppender);

        assertThat(ecsTasks.hasInstance(ecsTask.get().name())).isTrue();
    }

    @Test
    void shouldNotRegisterIfTaskIsNotCreated() throws Exception {
        when(taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender)).thenReturn(Optional.empty());

        assertThat(ecsTasks.hasAnyTasks()).isFalse();

        ecsTasks.create(createAgentRequest, pluginSettings, consoleLogAppender);

        assertThat(ecsTasks.hasAnyTasks()).isFalse();
    }

    @Test
    void shouldNotCreateAnAgentIfOneAlreadyExistWithSameJobIdentifier() throws Exception {
        final JobIdentifier jobIdentifier = mock(JobIdentifier.class);
        final ECSTask task = mock(ECSTask.class);

        when(task.name()).thenReturn("agent-id");
        when(task.getJobIdentifier()).thenReturn(jobIdentifier);
        when(createAgentRequest.getJobIdentifier()).thenReturn(jobIdentifier);
        when(taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender)).thenReturn(Optional.of(task));

        ecsTasks.create(createAgentRequest, pluginSettings, consoleLogAppender);
        ecsTasks.create(createAgentRequest, pluginSettings, consoleLogAppender);
        ecsTasks.create(createAgentRequest, pluginSettings, consoleLogAppender);

        verify(taskHelper, times(1)).create(createAgentRequest, pluginSettings, consoleLogAppender);
    }

    @Test
    void shouldTerminateAnExistingAgent() throws Exception {
        final ECSTask task = mock(ECSTask.class);

        when(task.name()).thenReturn("agent-id");
        when(taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender)).thenReturn(Optional.of(task));
        ecsTasks.create(createAgentRequest, pluginSettings, consoleLogAppender);
        assertThat(ecsTasks.hasInstance(task.name())).isTrue();

        ecsTasks.terminate(task.name(), pluginSettings);

        assertThat(ecsTasks.hasInstance(task.name())).isFalse();
    }

    @Test
    void shouldTerminateAnUnregisteredAgentAfterTimeout() throws Exception {
        final ECSTask task = mock(ECSTask.class);
        final Agents agents = new Agents();

        when(task.name()).thenReturn("agent-id");
        when(task.createdAt()).thenReturn(DateTime.now().minusMinutes(3));
        when(taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender)).thenReturn(Optional.of(task));
        ecsTasks.create(createAgentRequest, pluginSettings, consoleLogAppender);

        ecsTasks.terminateUnregisteredInstances(pluginSettings, agents);

        assertThat(ecsTasks.hasInstance("agent-id")).isFalse();
    }

    @Test
    void shouldNotTerminateAnUnregisteredAgentBeforeTimeout() throws Exception {
        final ECSTask task = mock(ECSTask.class);
        final Agents agents = new Agents();

        when(task.name()).thenReturn("agent-id");
        when(task.createdAt()).thenReturn(DateTime.now().minusMinutes(1));
        when(taskHelper.create(createAgentRequest, pluginSettings, consoleLogAppender)).thenReturn(Optional.of(task));
        ecsTasks.create(createAgentRequest, pluginSettings, consoleLogAppender);

        ecsTasks.terminateUnregisteredInstances(pluginSettings, agents);

        assertThat(ecsTasks.hasInstance("agent-id")).isTrue();
    }

    @Test
    void shouldRefreshAllAgentInstancesAtStartup() throws Exception {
        final PluginRequest pluginRequest = mock(PluginRequest.class);
        final TaskDefinition taskDefinition = mock(TaskDefinition.class);
        final Task task = mock(Task.class);
        final ECSTask ecsTask = mock(ECSTask.class);

        when(ecsTask.name()).thenReturn("agent-id");
        when(taskHelper.listAllTasks(pluginSettings)).thenReturn(Collections.singletonMap(task, taskDefinition));
        when(taskHelper.fromTaskInfo(eq(task), eq(taskDefinition), anyMap(), any())).thenReturn(Optional.of(ecsTask));

        ecsTasks.refreshAll(pluginSettings);

        verify(taskHelper, times(1)).listAllTasks(pluginSettings);
        verify(taskHelper, times(1)).fromTaskInfo(eq(task), eq(taskDefinition), anyMap(), eq(null));
        assertThat(ecsTasks.hasInstance(ecsTask.name())).isTrue();
    }

    @Test
    void shouldNotRefreshAllAgentInstancesAfterTheStartup() throws Exception {
        ecsTasks.refreshAll(pluginSettings);
        verify(taskHelper, times(1)).listAllTasks(pluginSettings);

        ecsTasks.refreshAll(pluginSettings);

        verifyNoMoreInteractions(taskHelper);
    }
}
