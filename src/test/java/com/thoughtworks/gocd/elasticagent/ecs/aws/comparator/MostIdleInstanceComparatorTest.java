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

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;
import static org.assertj.core.api.Assertions.assertThat;

class MostIdleInstanceComparatorTest {
    @Test
    void shouldSortInstanceOnLastSeenIdleTime() {
        final Instance instance1 = new Instance().withInstanceId("1").withTags(new Tag().withKey(LAST_SEEN_IDLE).withValue("1234"));
        final Instance instance2 = new Instance().withInstanceId("2").withTags(new Tag().withKey(LAST_SEEN_IDLE).withValue("4321"));
        final Instance instance3 = new Instance().withInstanceId("3").withTags(new Tag().withKey(LAST_SEEN_IDLE).withValue("1000"));

        final List<Instance> unsortedInstances = Arrays.asList(instance1, instance2, instance3);
        unsortedInstances.sort(new MostIdleInstanceComparator(Clock.DEFAULT.now()));

        assertThat(unsortedInstances).containsExactly(instance3, instance1, instance2);
    }

    @Test
    void shouldConsiderLastSeenTimeToCurrentTimeIfNotPresentInEC2() {
        final Instance instance1 = new Instance().withInstanceId("1");
        final Instance instance2 = new Instance().withInstanceId("2").withTags(new Tag().withKey(LAST_SEEN_IDLE).withValue("4321"));
        final Instance instance3 = new Instance().withInstanceId("3").withTags(new Tag().withKey(LAST_SEEN_IDLE).withValue("1000"));

        final List<Instance> unsortedInstances = Arrays.asList(instance1, instance2, instance3);
        unsortedInstances.sort(new MostIdleInstanceComparator(Clock.DEFAULT.now()));

        assertThat(unsortedInstances).containsExactly(instance3, instance2, instance1);
    }

    @Test
    void shouldReversTheOrder() {
        final Instance instance1 = new Instance().withInstanceId("1");
        final Instance instance2 = new Instance().withInstanceId("2").withTags(new Tag().withKey(LAST_SEEN_IDLE).withValue("4321"));
        final Instance instance3 = new Instance().withInstanceId("3").withTags(new Tag().withKey(LAST_SEEN_IDLE).withValue("1000"));

        final List<Instance> unsortedInstances = Arrays.asList(instance1, instance2, instance3);
        unsortedInstances.sort(new MostIdleInstanceComparator(Clock.DEFAULT.now()));

        assertThat(unsortedInstances).containsExactly(instance3, instance2, instance1);

        unsortedInstances.sort(new MostIdleInstanceComparator(Clock.DEFAULT.now()).reversed());
        assertThat(unsortedInstances).containsExactly(instance1, instance2, instance3);
    }
}
