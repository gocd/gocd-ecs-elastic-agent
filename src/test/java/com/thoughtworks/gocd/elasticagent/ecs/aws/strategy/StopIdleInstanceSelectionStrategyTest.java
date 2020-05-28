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
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.runningInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static java.lang.String.valueOf;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class StopIdleInstanceSelectionStrategyTest {
    private StopIdleInstanceSelectionStrategy stopIdleInstanceSelectionStrategy;
    private PluginSettings pluginSettings;
    private Clock.TestClock testClock;
    private ContainerInstanceHelper containerInstanceHelper;

    @BeforeEach
    void setUp() {
        testClock = new Clock.TestClock();
        pluginSettings = mock(PluginSettings.class);
        containerInstanceHelper = mock(ContainerInstanceHelper.class);

        stopIdleInstanceSelectionStrategy = new StopIdleInstanceSelectionStrategy(containerInstanceHelper, testClock);
    }

    @Nested
    class InstanceForScheduling {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldReturnInstanceWhichDoesNotMarkedAsSeenIdle(Platform platform) {
            final List<ContainerInstance> containerInstances = Arrays.asList(
                    containerInstance("i-abcde", 0, 0),
                    containerInstance("i-12345", 0, 1)
            );

            final List<Instance> instances = Arrays.asList(
                    runningInstance("i-abcde", platform, new Tag(LAST_SEEN_IDLE, valueOf(testClock.now().getMillis()))),
                    runningInstance("i-12345", platform)
            );

            testClock.forward(new Period().withMinutes(1));

            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);

            final Optional<ContainerInstance> containerInstance = stopIdleInstanceSelectionStrategy.instanceForScheduling(pluginSettings, ElasticAgentProfileProperties.empty(platform), new ContainerDefinition());

            assertThat(containerInstance.isPresent()).isTrue();
            assertThat(containerInstance.get().getEc2InstanceId()).isEqualTo("i-12345");
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldReturnInstanceWhichHasBeenSeenIdleForShortestAmountOfTime(Platform platform) {
            final List<ContainerInstance> containerInstances = Arrays.asList(
                    containerInstance("i-abcde", 0, 0),
                    containerInstance("i-12345", 0, 1),
                    containerInstance("i-foobar", 0, 1)
            );

            final List<Instance> instances = Arrays.asList(
                    runningInstance("i-abcde", platform, new Tag(LAST_SEEN_IDLE, "2")),
                    runningInstance("i-12345", platform, new Tag(LAST_SEEN_IDLE, "1")),
                    runningInstance("i-foobar", platform, new Tag(LAST_SEEN_IDLE, "3"))
            );

            testClock.forward(new Period().withMinutes(4));

            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);

            final Optional<ContainerInstance> containerInstance = stopIdleInstanceSelectionStrategy.instanceForScheduling(pluginSettings, ElasticAgentProfileProperties.empty(platform), new ContainerDefinition());

            assertThat(containerInstance.isPresent()).isTrue();
            assertThat(containerInstance.get().getEc2InstanceId()).isEqualTo("i-foobar");
        }
    }

    @Nested
    class InstanceToStop {
        @Test
        void shouldStopOnlyAOnDemandInstance() {
            when(containerInstanceHelper.onDemandContainerInstances(pluginSettings)).thenReturn(emptyList());

            stopIdleInstanceSelectionStrategy.instancesToStop(pluginSettings, LINUX);

            verify(containerInstanceHelper).onDemandContainerInstances(pluginSettings);
        }


        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldNotReturnInstanceIfIdleTimeoutIsNotReached(Platform platform) {
            final List<ContainerInstance> containerInstances = Arrays.asList(
                    containerInstance("i-abcde", 0, 0),
                    containerInstance("i-12345", 0, 0)
            );

            final List<Instance> instances = Arrays.asList(
                    runningInstance("i-abcde", platform, new Tag(LAST_SEEN_IDLE, valueOf(testClock.now().getMillis()))),
                    runningInstance("i-12345", platform, new Tag(LAST_SEEN_IDLE, valueOf(testClock.now().getMillis())))
            );

            when(pluginSettings.stopLinuxInstanceAfter()).thenReturn(new Period().withMinutes(6));
            when(pluginSettings.stopWindowsInstanceAfter()).thenReturn(new Period().withMinutes(6));
            when(containerInstanceHelper.onDemandContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);

            testClock.forward(new Period().withMinutes(5));

            final Optional<List<ContainerInstance>> containerInstance = stopIdleInstanceSelectionStrategy.instancesToStop(pluginSettings, platform);

            assertThat(containerInstance.isPresent()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldReturnInstancesIfIdleTimeoutIsReached(Platform platform) {
            final ContainerInstance instanceToTerminate1 = containerInstance("i-abcde1", 0, 0);
            final ContainerInstance instanceToTerminate2 = containerInstance("i-abcde2", 0, 0);
            final List<ContainerInstance> containerInstances = Arrays.asList(
                    instanceToTerminate1, instanceToTerminate2,
                    containerInstance("i-12345", 0, 0)
            );

            final List<Instance> instances = Arrays.asList(
                    runningInstance("i-abcde1", platform, new Tag(LAST_SEEN_IDLE, valueOf(testClock.now().getMillis()))),
                    runningInstance("i-abcde2", platform, new Tag(LAST_SEEN_IDLE, valueOf(testClock.now().getMillis()))),
                    runningInstance("i-12345", platform, new Tag(LAST_SEEN_IDLE, valueOf(testClock.now().plusMinutes(3).getMillis())))
            );

            when(pluginSettings.stopLinuxInstanceAfter()).thenReturn(new Period().withMinutes(6));
            when(pluginSettings.stopWindowsInstanceAfter()).thenReturn(new Period().withMinutes(6));
            when(containerInstanceHelper.onDemandContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);

            testClock.forward(new Period().withMinutes(7));

            final Optional<List<ContainerInstance>> containerInstance = stopIdleInstanceSelectionStrategy.instancesToStop(pluginSettings, platform);

            assertThat(containerInstance.isPresent()).isTrue();
            assertThat(containerInstance.get())
                    .hasSize(2)
                    .contains(instanceToTerminate1, instanceToTerminate2);
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldNotReturnInstanceIfItIsNotIdle(Platform platform) {
            final List<ContainerInstance> containerInstances = Arrays.asList(
                    containerInstance("i-abcde", 0, 1),
                    containerInstance("i-12345", 0, 1)
            );

            final List<Instance> instances = Arrays.asList(
                    runningInstance("i-abcde", platform, new Tag(LAST_SEEN_IDLE, valueOf(testClock.now().getMillis()))),
                    runningInstance("i-12345", platform, new Tag(LAST_SEEN_IDLE, valueOf(testClock.now().getMillis())))
            );

            when(containerInstanceHelper.onDemandContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(instances);

            testClock.forward(new Period().withMinutes(7));

            final Optional<List<ContainerInstance>> containerInstance = stopIdleInstanceSelectionStrategy.instancesToStop(pluginSettings, LINUX);

            assertThat(containerInstance.isPresent()).isFalse();
        }
    }
}
