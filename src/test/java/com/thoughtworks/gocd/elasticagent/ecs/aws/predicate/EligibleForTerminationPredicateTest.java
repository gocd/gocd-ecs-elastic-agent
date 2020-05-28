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

package com.thoughtworks.gocd.elasticagent.ecs.aws.predicate;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.STOPPED_AT;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.instance;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.linuxInstanceWithTag;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

class EligibleForTerminationPredicateTest {
    @Mock
    private PluginSettings pluginSettings;

    @BeforeEach
    void setUp() {
        initMocks(this);
    }

    @Test
    void shouldReturnTrueIfInstanceDoesNotHaveStoppedAtLabel() {
        final Instance instance = linuxInstanceWithTag("i-abcd123", STOPPED, new Tag("Foo", "Bar"));

        final boolean testResult = new EligibleForTerminationPredicate(null).test(instance);

        assertThat(testResult).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {PENDING, RUNNING, STOPPING, SHUTTING_DOWN, TERMINATED})
    void shouldReturnFalseForInstanceWithState(String state) {
        final Instance instance = instance("i-abcd123", state, Platform.WINDOWS.name());

        final boolean testResult = new EligibleForTerminationPredicate(null).test(instance);

        assertThat(testResult).isFalse();
    }

    @ParameterizedTest
    @EnumSource(Platform.class)
    void shouldReturnTrueWhenInstanceIsStoppedLongerThanTheTimeConfiguredInPluginSettings(Platform platform) {
        final Clock.TestClock testClock = new Clock.TestClock();
        final Instance instance = instance("i-abcd123", STOPPED, platform.name())
                .withTags(new Tag(STOPPED_AT, String.valueOf(testClock.now().getMillis())));

        when(pluginSettings.terminateStoppedLinuxInstanceAfter()).thenReturn(Period.seconds(20));
        when(pluginSettings.terminateStoppedWindowsInstanceAfter()).thenReturn(Period.seconds(20));

        testClock.forward(Period.seconds(21));

        final boolean testResult = new EligibleForTerminationPredicate(pluginSettings, testClock).test(instance);

        assertThat(testResult).isTrue();
    }

    @ParameterizedTest
    @EnumSource(Platform.class)
    void shouldReturnFalseWhenTerminationTimeIsNotReached(Platform platform) {
        final Clock.TestClock testClock = new Clock.TestClock();
        final Instance instance = instance("i-abcd123", STOPPED, platform.name())
                .withTags(new Tag(STOPPED_AT, String.valueOf(testClock.now().getMillis())));

        when(pluginSettings.terminateStoppedLinuxInstanceAfter()).thenReturn(Period.seconds(20));
        when(pluginSettings.terminateStoppedWindowsInstanceAfter()).thenReturn(Period.seconds(20));

        testClock.forward(Period.seconds(19));

        final boolean testResult = new EligibleForTerminationPredicate(pluginSettings, testClock).test(instance);

        assertThat(testResult).isFalse();
    }
}
