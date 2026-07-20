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

package com.thoughtworks.gocd.elasticagent.ecs.aws.predicate;

import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.Mock;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.time.Duration;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.STOPPED_AT;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class EligibleForTerminationPredicateTest {
    @Mock
    private PluginSettings pluginSettings;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void shouldReturnTrueIfInstanceDoesNotHaveStoppedAtLabel() {
        final Instance instance = linuxInstanceWithTag("i-abcd123", InstanceStateName.STOPPED, Tag.builder().key("Foo").value("Bar").build());

        final boolean testResult = new EligibleForTerminationPredicate(null).test(instance);

        assertThat(testResult).isTrue();
    }

    @ParameterizedTest
    @EnumSource(value = InstanceStateName.class, names = {"PENDING", "RUNNING", "STOPPING", "SHUTTING_DOWN", "TERMINATED"})
    void shouldReturnFalseForInstanceWithState(InstanceStateName state) {
        final Instance instance = instance("i-abcd123", state, Platform.WINDOWS.name());

        final boolean testResult = new EligibleForTerminationPredicate(null).test(instance);

        assertThat(testResult).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Platform.class)
    void shouldReturnTrueWhenInstanceIsStoppedLongerThanTheTimeConfiguredInPluginSettings(Platform platform) {
        final Clock.TestClock testClock = new Clock.TestClock();
        final Instance instance = instanceBuilder("i-abcd123", InstanceStateName.STOPPED, platform.name())
                .tags(Tag.builder().key(STOPPED_AT).value(String.valueOf(testClock.now().toEpochMilli())).build())
                .build();

        when(pluginSettings.terminateStoppedLinuxInstanceAfter()).thenReturn(Duration.ofSeconds(20));
        when(pluginSettings.terminateStoppedWindowsInstanceAfter()).thenReturn(Duration.ofSeconds(20));

        testClock.forward(Duration.ofSeconds(21));

        final boolean testResult = new EligibleForTerminationPredicate(pluginSettings, testClock).test(instance);

        assertThat(testResult).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Platform.class)
    void shouldReturnFalseWhenTerminationTimeIsNotReached(Platform platform) {
        final Clock.TestClock testClock = new Clock.TestClock();
        final Instance instance = instanceBuilder("i-abcd123", InstanceStateName.STOPPED, platform.name())
                .tags(Tag.builder().key(STOPPED_AT).value(String.valueOf(testClock.now().toEpochMilli())).build())
                .build();

        when(pluginSettings.terminateStoppedLinuxInstanceAfter()).thenReturn(Duration.ofSeconds(20));
        when(pluginSettings.terminateStoppedWindowsInstanceAfter()).thenReturn(Duration.ofSeconds(20));

        testClock.forward(Duration.ofSeconds(19));

        final boolean testResult = new EligibleForTerminationPredicate(pluginSettings, testClock).test(instance);

        assertThat(testResult).isFalse();
    }
}
