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
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.ContainerInstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.InstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;
import java.util.Optional;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.linuxInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class OldestInstanceSelectionStrategyTest {

    private ContainerInstanceHelper containerInstanceHelper;
    private PluginSettings pluginSettings;
    private OldestInstanceSelectionStrategy oldestInstanceSelectionStrategy;
    private InstanceMatcher instanceMatcher;
    private ContainerInstanceMatcher containerInstanceMatcher;

    @BeforeEach
    void setUp() {
        containerInstanceHelper = mock(ContainerInstanceHelper.class);
        pluginSettings = mock(PluginSettings.class);

        instanceMatcher = mock(InstanceMatcher.class);
        containerInstanceMatcher = mock(ContainerInstanceMatcher.class);
        oldestInstanceSelectionStrategy = new OldestInstanceSelectionStrategy(containerInstanceHelper, instanceMatcher, containerInstanceMatcher);
    }

    @Nested
    class InstanceForScheduling {
        private ElasticAgentProfileProperties elasticAgentProfileProperties;
        private ContainerDefinition containerDefinition;

        @BeforeEach
        void setUp() {
            elasticAgentProfileProperties = mock(ElasticAgentProfileProperties.class);
            containerDefinition = mock(ContainerDefinition.class);

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
        }

        @ParameterizedTest
        @ValueSource(strings = {RUNNING, PENDING})
        void shouldReturnMostRecentlyLaunchedInstanceWithState(String validState) {
            final List<ContainerInstance> containerInstances = asList(
                    containerInstance("i-stopped", 0, 0),
                    containerInstance("i-abcde", 0, 0),
                    containerInstance("i-12345", 0, 1)
            );

            final Clock.TestClock testClock = new Clock.TestClock();
            final List<Instance> instances = asList(
                    linuxInstance("i-stopped", STOPPED, testClock.now().toDate()),
                    linuxInstance("i-abcde", validState, testClock.now().toDate()),
                    linuxInstance("i-12345", testClock.forward(Period.minutes(2)).now().toDate())
            );

            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);
            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(containerInstanceMatcher.matches(any(), any())).thenReturn(true);

            final Optional<ContainerInstance> containerInstance = oldestInstanceSelectionStrategy.instanceForScheduling(pluginSettings, elasticAgentProfileProperties, containerDefinition);

            assertThat(containerInstance.isPresent()).isTrue();
            assertThat(containerInstance.get().getEc2InstanceId()).isEqualTo("i-12345");
        }
    }

    @Nested
    class InstanceToStop {
        @Test
        void shouldReturnOldestInstance() {
            final List<ContainerInstance> containerInstances = asList(
                    containerInstance("i-abcde", 0, 0),
                    containerInstance("i-12345", 0, 1)
            );

            final Clock.TestClock testClock = new Clock.TestClock();
            final List<Instance> instances = asList(
                    linuxInstance("i-abcde", testClock.now().toDate()),
                    linuxInstance("i-12345", testClock.forward(Period.minutes(2)).now().toDate())
            );

            when(containerInstanceHelper.onDemandContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);

            final Optional<List<ContainerInstance>> containerInstance = oldestInstanceSelectionStrategy.instancesToStop(pluginSettings, LINUX);

            assertThat(containerInstance.isPresent()).isTrue();
            assertThat(containerInstance.get().get(0).getEc2InstanceId()).isEqualTo("i-abcde");
        }

        @Test
        void shouldStopOnlyAOnDemandInstance() {
            when(containerInstanceHelper.onDemandContainerInstances(pluginSettings)).thenReturn(emptyList());

            oldestInstanceSelectionStrategy.instancesToStop(pluginSettings, LINUX);

            verify(containerInstanceHelper).onDemandContainerInstances(pluginSettings);
        }
    }
}
