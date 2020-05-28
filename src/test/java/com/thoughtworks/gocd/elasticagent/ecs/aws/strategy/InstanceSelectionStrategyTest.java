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

package com.thoughtworks.gocd.elasticagent.ecs.aws.strategy;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.ContainerInstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.InstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.*;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class InstanceSelectionStrategyTest {
    private InstanceSelectionStrategy idleInstanceSelectionStrategy;
    private PluginSettings pluginSettings;
    private ContainerInstanceHelper containerInstanceHelper;
    private InstanceMatcher instanceMatcher;
    private ContainerInstanceMatcher containerInstanceMatcher;

    @BeforeEach
    void setUp() {
        pluginSettings = mock(PluginSettings.class);
        containerInstanceHelper = mock(ContainerInstanceHelper.class);
        instanceMatcher = mock(InstanceMatcher.class);
        containerInstanceMatcher = mock(ContainerInstanceMatcher.class);

        idleInstanceSelectionStrategy = new StubInstanceSelectionStrategy(containerInstanceHelper, instanceMatcher, containerInstanceMatcher);
    }

    @Nested
    class ForScheduling {

        private ElasticAgentProfileProperties elasticAgentProfileProperties;

        @BeforeEach
        void setUp() {
            elasticAgentProfileProperties = mock(ElasticAgentProfileProperties.class);

            when(elasticAgentProfileProperties.platform()).thenReturn(Platform.LINUX);
        }

        @Test
        void shouldNotReturnContainerInstanceWhenNoContainerInstancesRunning() {
            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(Collections.emptyList());

            final Optional<ContainerInstance> containerInstance = idleInstanceSelectionStrategy.instanceForScheduling(pluginSettings, elasticAgentProfileProperties, null);

            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldNotReturnContainerInstanceIfInstanceDoesNotMatchWithElasticProfile() {
            final List<ContainerInstance> containerInstances = asList(
                    containerInstance("i-linux", 0, 0),
                    containerInstance("i-windows", 0, 0)
            );

            final List<Instance> instances = asList(
                    runningLinuxInstance("i-linux"),
                    runningWindowsInstance("i-windows")
            );

            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);
            when(instanceMatcher.matches(any(), any())).thenReturn(false);

            final Optional<ContainerInstance> containerInstance = idleInstanceSelectionStrategy.instanceForScheduling(pluginSettings, elasticAgentProfileProperties, null);

            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldNotReturnContainerInstanceIfContainerInstanceDoesNotMatchWithContainerDefinition() {
            final List<ContainerInstance> containerInstances = asList(
                    containerInstance("i-linux", 0, 0),
                    containerInstance("i-windows", 0, 0)
            );

            final List<Instance> instances = asList(
                    runningLinuxInstance("i-linux"),
                    runningWindowsInstance("i-windows")
            );

            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);
            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(containerInstanceMatcher.matches(any(), any())).thenReturn(false);

            final Optional<ContainerInstance> containerInstance = idleInstanceSelectionStrategy.instanceForScheduling(pluginSettings, elasticAgentProfileProperties, null);

            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldReturnContainerInstanceIfItMatchesTheProfileAndContainerDefinition() {
            final List<ContainerInstance> containerInstances = asList(
                    containerInstance("i-linux", 0, 0),
                    containerInstance("i-windows", 0, 0)
            );

            final List<Instance> instances = Arrays.asList(
                    runningLinuxInstance("i-linux"),
                    runningWindowsInstance("i-windows")
            );

            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);
            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(containerInstanceMatcher.matches(any(), any())).thenReturn(true);

            final Optional<ContainerInstance> containerInstance = idleInstanceSelectionStrategy.instanceForScheduling(pluginSettings, elasticAgentProfileProperties, null);

            assertThat(containerInstance.isPresent()).isTrue();
            assertThat(containerInstance.get().getEc2InstanceId()).isEqualTo("i-linux");

        }

        @Test
        void shouldRemoveLastSeenIdleTagForAMatchingSpotInstance() {
            final List<ContainerInstance> containerInstances = asList(
                    containerInstance("i-linux", 0, 0),
                    containerInstance("i-windows", 0, 0)
            );

            final List<Instance> instances = asList(
                    runningLinuxInstance("i-linux").withSpotInstanceRequestId("spot_id"),
                    runningWindowsInstance("i-windows").withSpotInstanceRequestId("spot_id_windows")
            );

            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);
            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(containerInstanceMatcher.matches(any(), any())).thenReturn(true);

            final Optional<ContainerInstance> containerInstance = idleInstanceSelectionStrategy.instanceForScheduling(pluginSettings, elasticAgentProfileProperties, null);

            assertThat(containerInstance.isPresent()).isTrue();
            assertThat(containerInstance.get().getEc2InstanceId()).isEqualTo("i-linux");
            verify(containerInstanceHelper).removeLastSeenIdleTag(pluginSettings, asList("i-linux"));
        }
    }
    @Nested
    class ForTermination {
        @Test
        void shouldNotReturnContainerInstanceWhenNoContainerInstancesRunning() {
            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(Collections.emptyList());

            final Optional<List<ContainerInstance>> containerInstance = idleInstanceSelectionStrategy.instancesToStop(pluginSettings, null);

            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldNotReturnContainerInstanceWhenClusterIsAlreadyScaledIn() {
            final List<ContainerInstance> containerInstances = asList(
                    containerInstance("i-linux", 0, 0),
                    containerInstance("i-windows", 0, 0)
            );

            final List<Instance> instances = asList(
                    runningLinuxInstance("i-linux"),
                    runningWindowsInstance("i-windows")
            );

            when(pluginSettings.getMinLinuxInstanceCount()).thenReturn(2);
            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);

            final Optional<List<ContainerInstance>> containerInstance = idleInstanceSelectionStrategy.instancesToStop(pluginSettings, Platform.LINUX);

            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldNotReturnContainerInstanceWhenNoContainerInstancesAreIdle() {
            final List<ContainerInstance> containerInstances = asList(
                    containerInstance("i-linux1", 1, 0),
                    containerInstance("i-linux2", 0, 1),
                    containerInstance("i-windows", 0, 0)
            );

            final List<Instance> instances = asList(
                    runningLinuxInstance("i-linux1"),
                    runningLinuxInstance("i-linux2"),
                    runningWindowsInstance("i-windows")
            );

            when(pluginSettings.getMinLinuxInstanceCount()).thenReturn(0);
            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);

            final Optional<List<ContainerInstance>> containerInstance = idleInstanceSelectionStrategy.instancesToStop(pluginSettings, Platform.LINUX);

            assertThat(containerInstance.isPresent()).isFalse();
        }

        @ParameterizedTest
        @ValueSource(strings = {PENDING, RUNNING})
        void shouldReturnContainerInstanceWithAcceptableStates(String instanceState) {
            final List<ContainerInstance> containerInstances = asList(
                    containerInstance("i-linux1", 0, 0),
                    containerInstance("i-linux2", 0, 0),
                    containerInstance("i-linux3", 0, 0)
            );

            final List<Instance> instances = asList(
                    linuxInstance("i-linux1", TERMINATED),
                    linuxInstance("i-linux2", STOPPED),
                    linuxInstance("i-linux3", instanceState)
            );

            when(pluginSettings.getMinLinuxInstanceCount()).thenReturn(0);
            when(containerInstanceHelper.onDemandContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);

            final Optional<List<ContainerInstance>> containerInstance = idleInstanceSelectionStrategy.instancesToStop(pluginSettings, Platform.LINUX);

            assertThat(containerInstance.isPresent()).isTrue();
            assertThat(containerInstance.get().get(0).getEc2InstanceId()).isEqualTo("i-linux3");
        }
    }

    class StubInstanceSelectionStrategy extends InstanceSelectionStrategy {

        StubInstanceSelectionStrategy(ContainerInstanceHelper containerInstanceHelper, InstanceMatcher instanceMatcher, ContainerInstanceMatcher containerInstanceMatcher) {
            super(containerInstanceHelper, instanceMatcher, containerInstanceMatcher);
        }

        @Override
        protected List<ContainerInstance> findInstancesToStop(PluginSettings pluginSettings, Platform platform, Map<String, ContainerInstance> instanceIdToContainerInstance, List<Instance> idleInstances) {
            return singletonList(instanceIdToContainerInstance.get(idleInstances.get(0).getInstanceId()));
        }

        @Override
        protected void sortInstancesForScheduling(List<Instance> ec2Instances) {
        }
    }
}
