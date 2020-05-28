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

package com.thoughtworks.gocd.elasticagent.ecs.aws.comparator;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Tag;
import org.joda.time.DateTime;

import java.util.Comparator;
import java.util.Optional;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;

public class MostIdleInstanceComparator implements Comparator<Instance> {
    private final DateTime now;

    public MostIdleInstanceComparator(DateTime now) {
        this.now = now;
    }

    @Override
    public int compare(Instance instance1, Instance instance2) {
        Long lastSeen1 = getLastSeenTime(instance1);
        Long lastSeen2 = getLastSeenTime(instance2);

        return lastSeen2.compareTo(lastSeen1);
    }

    private Long getLastSeenTime(Instance instance1) {
        final Optional<String> lastSeenIdle = instance1.getTags().stream()
                .filter(tag -> tag.getKey().equals(LAST_SEEN_IDLE))
                .findFirst()
                .map(Tag::getValue);

        return lastSeenIdle.map(s -> now.getMillis() - Long.parseLong(s)).orElse(0L);
    }
}
