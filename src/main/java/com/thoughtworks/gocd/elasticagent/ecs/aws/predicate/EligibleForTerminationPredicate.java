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

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.function.Predicate;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.STOPPED_AT;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static java.text.MessageFormat.format;

public class EligibleForTerminationPredicate implements Predicate<Instance> {
    private static final Logger LOG = Logger.getLoggerFor(EligibleForTerminationPredicate.class);
    private final PluginSettings pluginSettings;
    private final Clock clock;

    public EligibleForTerminationPredicate(PluginSettings pluginSettings) {
        this(pluginSettings, Clock.DEFAULT);
    }

    EligibleForTerminationPredicate(PluginSettings pluginSettings, Clock clock) {
        this.pluginSettings = pluginSettings;
        this.clock = clock;
    }

    @Override
    public boolean test(Instance instance) {
        if (isNotInStoppedState(instance)) {
            return false;
        }

        final Optional<String> stoppedAt = instance.tags().stream()
                .filter(tag -> tag.key().equals(STOPPED_AT))
                .findFirst()
                .map(Tag::value);

        if (stoppedAt.isEmpty()) {
            LOG.info(format("Instance {0} does not have STOPPED_AT tag. Instance without tag is eligible for termination.", instance.instanceId()));
            return true;
        }

        Duration timeInstanceCanStayStopped = getTimeInstanceCanStayStopped(instance);

        return clock.now().isAfter(Instant.ofEpochMilli(Long.parseLong(stoppedAt.get())).plus(timeInstanceCanStayStopped));
    }

    private boolean isNotInStoppedState(Instance instance) {
        return !InstanceStateName.STOPPED.equals(instance.state().name());
    }

    private Duration getTimeInstanceCanStayStopped(Instance instance) {
        return Platform.from(instance.platformAsString()) == LINUX ? pluginSettings.terminateStoppedLinuxInstanceAfter() : pluginSettings.terminateStoppedWindowsInstanceAfter();
    }
}
