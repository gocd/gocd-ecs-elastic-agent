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
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.comparator.MostIdleInstanceComparator;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;

public class StopIdleInstanceSelectionStrategy extends InstanceSelectionStrategy {
    private final Clock clock;

    public StopIdleInstanceSelectionStrategy() {
        super(new ContainerInstanceHelper());
        this.clock = Clock.DEFAULT;
    }

    StopIdleInstanceSelectionStrategy(ContainerInstanceHelper containerInstanceHelper, Clock clock) {
        super(containerInstanceHelper);
        this.clock = clock;
    }

    @Override
    protected void sortInstancesForScheduling(List<Instance> ec2Instances) {
        ec2Instances.sort(new MostIdleInstanceComparator(clock.now()).reversed());
    }

    @Override
    protected List<ContainerInstance> findInstancesToStop(PluginSettings pluginSettings, Platform platform, Map<String, ContainerInstance> instanceIdToContainerInstance, List<Instance> idleInstances) {
        final Period timeInstanceCanStayIdle = platform == Platform.LINUX ? pluginSettings.stopLinuxInstanceAfter() : pluginSettings.stopWindowsInstanceAfter();

        idleInstances.sort(new MostIdleInstanceComparator(clock.now()));

        return idleInstances.stream()
                .filter(isIdlePeriodIsMoreThan(timeInstanceCanStayIdle))
                .map(instance -> instanceIdToContainerInstance.get(instance.getInstanceId()))
                .collect(Collectors.toList());
    }

    private Predicate<Instance> isIdlePeriodIsMoreThan(Period timeInstanceCanStayIdle) {
        return instance -> {
            final Optional<String> lastSeenIdle = instance.getTags().stream()
                    .filter(tag -> tag.getKey().equals(LAST_SEEN_IDLE))
                    .findFirst()
                    .map(Tag::getValue);

            if (lastSeenIdle.isPresent()) {
                final DateTime lastSeenIdleTime = new DateTime(Long.parseLong(lastSeenIdle.get()));
                if (clock.now().isAfter(lastSeenIdleTime.plus(timeInstanceCanStayIdle))) {
                    return true;
                }
            }
            return false;
        };
    }

}
