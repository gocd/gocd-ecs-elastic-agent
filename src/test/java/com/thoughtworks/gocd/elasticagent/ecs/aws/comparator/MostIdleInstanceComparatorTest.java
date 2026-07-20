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

package com.thoughtworks.gocd.elasticagent.ecs.aws.comparator;

import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;
import static org.assertj.core.api.Assertions.assertThat;

class MostIdleInstanceComparatorTest {
    @Test
    void shouldSortInstanceOnLastSeenIdleTime() {
        final Instance instance1 = Instance.builder().instanceId("1").tags(Tag.builder().key(LAST_SEEN_IDLE).value("1234").build()).build();
        final Instance instance2 = Instance.builder().instanceId("2").tags(Tag.builder().key(LAST_SEEN_IDLE).value("4321").build()).build();
        final Instance instance3 = Instance.builder().instanceId("3").tags(Tag.builder().key(LAST_SEEN_IDLE).value("1000").build()).build();

        final List<Instance> unsortedInstances = Arrays.asList(instance1, instance2, instance3);
        unsortedInstances.sort(new MostIdleInstanceComparator(Clock.DEFAULT.now()));

        assertThat(unsortedInstances).containsExactly(instance3, instance1, instance2);
    }

    @Test
    void shouldConsiderLastSeenTimeToCurrentTimeIfNotPresentInEC2() {
        final Instance instance1 = Instance.builder().instanceId("1").build();
        final Instance instance2 = Instance.builder().instanceId("2").tags(Tag.builder().key(LAST_SEEN_IDLE).value("4321").build()).build();
        final Instance instance3 = Instance.builder().instanceId("3").tags(Tag.builder().key(LAST_SEEN_IDLE).value("1000").build()).build();

        final List<Instance> unsortedInstances = Arrays.asList(instance1, instance2, instance3);
        unsortedInstances.sort(new MostIdleInstanceComparator(Clock.DEFAULT.now()));

        assertThat(unsortedInstances).containsExactly(instance3, instance2, instance1);
    }

    @Test
    void shouldReverseTheOrder() {
        final Instance instance1 = Instance.builder().instanceId("1").build();
        final Instance instance2 = Instance.builder().instanceId("2").tags(Tag.builder().key(LAST_SEEN_IDLE).value("4321").build()).build();
        final Instance instance3 = Instance.builder().instanceId("3").tags(Tag.builder().key(LAST_SEEN_IDLE).value("1000").build()).build();

        final List<Instance> unsortedInstances = Arrays.asList(instance1, instance2, instance3);
        unsortedInstances.sort(new MostIdleInstanceComparator(Clock.DEFAULT.now()));

        assertThat(unsortedInstances).containsExactly(instance3, instance2, instance1);

        unsortedInstances.sort(new MostIdleInstanceComparator(Clock.DEFAULT.now()).reversed());
        assertThat(unsortedInstances).containsExactly(instance1, instance2, instance3);
    }
}
