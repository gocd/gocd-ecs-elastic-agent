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

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.*;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.SpotInstanceService;
import com.thoughtworks.gocd.elasticagent.ecs.aws.TaskHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.InstanceSelectionStrategy;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.InstanceSelectionStrategyFactory;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.StopOperation;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.TerminateOperation;
import com.thoughtworks.gocd.elasticagent.ecs.domain.*;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ServerPingRequest;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentMatcher;

import java.util.*;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Agent.ConfigState.Disabled;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.RUNNING;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.STOPPED;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class ServerPingRequestExecutorTest {

    private ClusterProfileProperties clusterProfileProperties;
    private ElasticAgentProfileProperties elasticAgentProfileProperties;
    private ContainerInstanceHelper containerInstanceHelper;
    private TaskHelper taskHelper;
    private PluginRequest pluginRequest;
    private ECSTasks agentInstances;
    private ServerPingRequestExecutor executor;
    private InstanceSelectionStrategyFactory instanceSelectionStrategyFactory;
    private InstanceSelectionStrategy instanceSelectionStrategy;
    private StopOperation stopOperation;
    private TerminateOperation terminationOperation;
    private ServerPingRequest serverPingRequest;
    private EventStream eventStream;
    private Map<String, ECSTasks> allAgentInstances;
    private ConsoleLogAppender consoleLogAppender;
    private SpotInstanceService spotInstanceService;

    @BeforeEach
    void setUp() {
        consoleLogAppender = mock(ConsoleLogAppender.class);
        clusterProfileProperties = mock(ClusterProfileProperties.class);
        when(clusterProfileProperties.uuid()).thenReturn("id1");
        elasticAgentProfileProperties = mock(ElasticAgentProfileProperties.class);
        containerInstanceHelper = mock(ContainerInstanceHelper.class);
        taskHelper = mock(TaskHelper.class);
        pluginRequest = mock(PluginRequest.class);
        serverPingRequest = mock(ServerPingRequest.class);
        eventStream = mock(EventStream.class);
        agentInstances = new ECSTasks(taskHelper, containerInstanceHelper, eventStream);
        instanceSelectionStrategyFactory = mock(InstanceSelectionStrategyFactory.class);
        instanceSelectionStrategy = mock(InstanceSelectionStrategy.class);
        stopOperation = mock(StopOperation.class);
        terminationOperation = mock(TerminateOperation.class);
        spotInstanceService = mock(SpotInstanceService.class);

        when(clusterProfileProperties.getMaxLinuxInstancesAllowed()).thenReturn(5);
        when(clusterProfileProperties.getClusterName()).thenReturn("GoCD");
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine:latest");
        when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
        when(instanceSelectionStrategyFactory.strategyFor(any())).thenReturn(instanceSelectionStrategy);

        when(serverPingRequest.allClusterProfileProperties()).thenReturn(Arrays.asList(clusterProfileProperties));
        allAgentInstances = new HashMap<>();
        allAgentInstances.put("id1", agentInstances);
        executor = new ServerPingRequestExecutor(serverPingRequest, allAgentInstances, pluginRequest, containerInstanceHelper, instanceSelectionStrategyFactory, stopOperation, terminationOperation, spotInstanceService);
    }

    @Test
    void testShouldDisableIdleAgents() throws Exception {
        final Agents agents = new Agents(Arrays.asList(new Agent("agent-id", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled)));

        when(pluginRequest.listAgents()).thenReturn(agents);
        verifyNoMoreInteractions(pluginRequest);

        executor.execute();

        verify(pluginRequest).disableAgents(argThat(collectionMatches(agents.agents())));
    }

    @Test
    void testShouldTerminateDisabledAgents() throws Exception {
        final Agents agents = new Agents(Arrays.asList(new Agent("agent-id", Agent.AgentState.Idle, Agent.BuildState.Idle, Disabled)));

        when(pluginRequest.listAgents()).thenReturn(agents, agents, new Agents());
        verifyNoMoreInteractions(pluginRequest);

        executor.execute();

        verify(pluginRequest).deleteAgents(argThat(collectionMatches(agents.agents())));
    }

    @Test
    void testShouldTerminateAgentsThatNeverAutoRegistered() throws Exception {
        final ECSTask task = mock(ECSTask.class);

        when(task.name()).thenReturn("task-name");
        when(pluginRequest.listAgents()).thenReturn(new Agents(new ArrayList<>()));
        verifyNoMoreInteractions(pluginRequest);

        when(taskHelper.create(any(CreateAgentRequest.class), eq(clusterProfileProperties), any(ConsoleLogAppender.class))).thenReturn(Optional.of(task));
        agentInstances.clock = new Clock.TestClock().forward(Period.minutes(11));

        Optional<ECSTask> container = agentInstances.create(new CreateAgentRequest(null, elasticAgentProfileProperties, null, null), clusterProfileProperties, consoleLogAppender);

        executor.execute();

        assertThat(agentInstances.hasInstance(container.get().name())).isFalse();
    }

    @Test
    void shouldDeleteAgentFromConfigWhenCorrespondingContainerIsNotPresent() throws Exception {
        when(pluginRequest.listAgents()).thenReturn(new Agents(Arrays.asList(new Agent("foo", Agent.AgentState.Idle, Agent.BuildState.Idle, Agent.ConfigState.Enabled))));
        verifyNoMoreInteractions(pluginRequest);

        ECSTasks spyAgentInstances = spy(new ECSTasks(taskHelper, containerInstanceHelper, null));
        when(spyAgentInstances.getEventStream()).thenReturn(eventStream);
        allAgentInstances.clear();
        allAgentInstances.put("id1", spyAgentInstances);

        new ServerPingRequestExecutor(serverPingRequest, allAgentInstances, pluginRequest, containerInstanceHelper, instanceSelectionStrategyFactory, stopOperation, terminationOperation, spotInstanceService).execute();

        verify(spyAgentInstances).terminateUnregisteredInstances(eq(clusterProfileProperties), any(Agents.class));
    }

    @Test
    void shouldNotScaleUpIfRunningInstancesAreAboveMinimumRequiredInstanceCount() throws Exception {
        final Agents agents = new Agents(new ArrayList<>());
        final List<Instance> runningInstances = Arrays.asList(
                runningLinuxInstance("i-linux1"),
                runningLinuxInstance("i-linux2"),
                runningWindowsInstance("i-windows1"),
                runningWindowsInstance("i-windows2")
        );

        when(containerInstanceHelper.getAllOnDemandInstances(clusterProfileProperties)).thenReturn(runningInstances);
        when(pluginRequest.listAgents()).thenReturn(agents);
        when(clusterProfileProperties.getMinLinuxInstanceCount()).thenReturn(2);
        when(clusterProfileProperties.getMinWindowsInstanceCount()).thenReturn(2);

        executor.execute();

        verify(containerInstanceHelper, times(3)).getAllOnDemandInstances(clusterProfileProperties);
        verify(containerInstanceHelper, times(2)).onDemandContainerInstances(clusterProfileProperties);
        verifyNoMoreInteractions(containerInstanceHelper);
    }

    @ParameterizedTest
    @EnumSource(Platform.class)
    void shouldTerminateStoppedIdleInstance(Platform platform) throws Exception {
        final List<Instance> allInstances = Arrays.asList(
                instance("i-abcdxyz", RUNNING, platform.name()),
                instance("i-abcd123", STOPPED, platform.name())
        );

        final ContainerInstance stoppedContainerInstance = containerInstance("i-abcd123");
        final List<ContainerInstance> containerInstances = Arrays.asList(
                containerInstance("i-abcdxyz"),
                stoppedContainerInstance
        );

        when(containerInstanceHelper.getAllOnDemandInstances(clusterProfileProperties)).thenReturn(allInstances);
        when(containerInstanceHelper.onDemandContainerInstances(clusterProfileProperties)).thenReturn(containerInstances);
        when(pluginRequest.listAgents()).thenReturn(new Agents(new ArrayList<>()));

        executor.execute();

        verify(terminationOperation).execute(clusterProfileProperties, singletonList(stoppedContainerInstance));
    }

    @Test
    void shouldTagSpotInstances() throws Exception {
        when(pluginRequest.listAgents()).thenReturn(new Agents());

        executor.execute();

        verify(spotInstanceService).tagSpotInstances(clusterProfileProperties);
    }

    @Test
    void shouldTagIdleSpotInstances() throws Exception {
        when(pluginRequest.listAgents()).thenReturn(new Agents());

        executor.execute();

        verify(spotInstanceService).tagIdleSpotInstances(clusterProfileProperties);
    }

    @Test
    void shouldTerminateIdleSpotInstances() throws Exception {
        when(pluginRequest.listAgents()).thenReturn(new Agents());

        executor.execute();

        verify(spotInstanceService).terminateIdleSpotInstances(clusterProfileProperties);
    }

    @Nested
    class Linux {
        @BeforeEach
        void setUp() {
            when(pluginRequest.listAgents()).thenReturn(new Agents(new ArrayList<>()));
        }

        @Test
        void shouldScaleUpIfRunningInstancesAreBelowMinimumRequiredInstanceCount() throws Exception {
            when(containerInstanceHelper.getAllOnDemandInstances(clusterProfileProperties)).thenReturn(singletonList(runningLinuxInstance("i-abcd123")));
            when(clusterProfileProperties.getMinLinuxInstanceCount()).thenReturn(5);

            executor.execute();

            verify(containerInstanceHelper, times(3)).getAllOnDemandInstances(clusterProfileProperties);
            verify(containerInstanceHelper).onDemandContainerInstances(clusterProfileProperties);
            verify(containerInstanceHelper).startOrCreateInstance(eq(clusterProfileProperties), any(ElasticAgentProfileProperties.class), eq(4), any(ConsoleLogAppender.class));
            verifyNoMoreInteractions(containerInstanceHelper);
        }

        @Test
        void shouldStopIdleInstanceWhenIdleTimeOutIsReached() throws Exception {
            final List<Instance> runningInstances = singletonList(runningLinuxInstance("i-abcded1"));
            final List<ContainerInstance> containerInstances = singletonList(containerInstance("i-abcded1"));

            when(containerInstanceHelper.getAllInstances(clusterProfileProperties)).thenReturn(runningInstances);
            when(containerInstanceHelper.getContainerInstances(clusterProfileProperties)).thenReturn(containerInstances);
            when(instanceSelectionStrategy.instancesToStop(clusterProfileProperties, LINUX)).thenReturn(Optional.of(containerInstances));

            executor.execute();

            verify(stopOperation).execute(clusterProfileProperties, containerInstances);
        }
    }

    @Nested
    class Windows {
        @BeforeEach
        void setUp() {
            when(pluginRequest.listAgents()).thenReturn(new Agents(new ArrayList<>()));
        }

        @Test
        void shouldScaleUpIfRunningInstancesAreBelowMinimumRequiredInstanceCount() throws Exception {
            final Agents agents = new Agents(new ArrayList<>());

            when(pluginRequest.listAgents()).thenReturn(agents);
            when(containerInstanceHelper.getAllOnDemandInstances(clusterProfileProperties)).thenReturn(singletonList(runningWindowsInstance("i-abcd123")));
            when(clusterProfileProperties.getMinWindowsInstanceCount()).thenReturn(5);

            executor.execute();

            verify(containerInstanceHelper, times(3)).getAllOnDemandInstances(clusterProfileProperties);
            verify(containerInstanceHelper).onDemandContainerInstances(clusterProfileProperties);
            verify(containerInstanceHelper).startOrCreateInstance(eq(clusterProfileProperties), any(ElasticAgentProfileProperties.class), eq(4), any(ConsoleLogAppender.class));
            verifyNoMoreInteractions(containerInstanceHelper);
        }

        @Test
        void shouldStopIdleInstanceWhenIdleTimeOutIsReached() throws Exception {
            final List<Instance> runningInstances = singletonList(runningWindowsInstance("i-abcded1"));
            final List<ContainerInstance> containerInstances = singletonList(containerInstance("i-abcded1"));

            when(containerInstanceHelper.getAllInstances(clusterProfileProperties)).thenReturn(runningInstances);
            when(containerInstanceHelper.getContainerInstances(clusterProfileProperties)).thenReturn(containerInstances);
            when(instanceSelectionStrategy.instancesToStop(clusterProfileProperties, WINDOWS)).thenReturn(Optional.of(containerInstances));

            executor.execute();

            verify(stopOperation).execute(clusterProfileProperties, containerInstances);
        }
    }

    private ArgumentMatcher<Collection<Agent>> collectionMatches(final Collection<Agent> values) {
        return argument -> new ArrayList<>(argument).equals(new ArrayList<>(values));
    }
}
