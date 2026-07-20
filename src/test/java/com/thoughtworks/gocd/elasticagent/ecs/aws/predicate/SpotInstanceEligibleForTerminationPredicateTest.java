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

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;


import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

class SpotInstanceEligibleForTerminationPredicateTest {
    @Mock
    private PluginSettings pluginSettings;

    @BeforeEach
    void setUp() {
        openMocks(this);
    }

    @Test
    void shouldReturnFalseIfInstanceDoesNotHaveLastSeenIdleLabel() {
        final Instance instance = linuxInstanceWithTag("i-abcd123", InstanceStateName.STOPPED, Tag.builder().key("Foo").value("Bar").build());

        final boolean testResult = new SpotInstanceEligibleForTerminationPredicate(null).test(instance);

        assertThat(testResult).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Platform.class)
    void shouldReturnTrueWhenInstanceIsIdleForLongerThanTheTimeConfiguredInPluginSettings(Platform platform) {
        final Clock.TestClock testClock = new Clock.TestClock();
        final Instance instance = spotInstanceBuilder("i-abcd123", InstanceStateName.RUNNING, platform.name())
                .tags(Tag.builder().key(LAST_SEEN_IDLE).value(String.valueOf(testClock.now().toEpochMilli())).build())
                .build();

        when(pluginSettings.terminateIdleLinuxSpotInstanceAfter()).thenReturn(Duration.ofSeconds(20));
        when(pluginSettings.terminateIdleWindowsSpotInstanceAfter()).thenReturn(Duration.ofSeconds(20));

        testClock.forward(Duration.ofSeconds(21));

        final boolean testResult = new SpotInstanceEligibleForTerminationPredicate(pluginSettings, testClock).test(instance);

        assertThat(testResult).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Platform.class)
    void shouldReturnFalseWhenTerminationTimeIsNotReached(Platform platform) {
        final Clock.TestClock testClock = new Clock.TestClock();
        final Instance instance = spotInstanceBuilder("i-abcd123", InstanceStateName.RUNNING, platform.name())
                .tags(Tag.builder().key(LAST_SEEN_IDLE).value(String.valueOf(testClock.now().toEpochMilli())).build())
                .build();

        when(pluginSettings.terminateIdleLinuxSpotInstanceAfter()).thenReturn(Duration.ofSeconds(20));
        when(pluginSettings.terminateIdleWindowsSpotInstanceAfter()).thenReturn(Duration.ofSeconds(20));

        testClock.forward(Duration.ofSeconds(19));

        final boolean testResult = new SpotInstanceEligibleForTerminationPredicate(pluginSettings, testClock).test(instance);

        assertThat(testResult).isFalse();
    }
}
