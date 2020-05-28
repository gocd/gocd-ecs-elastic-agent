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
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.Optional;
import java.util.function.Predicate;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.STOPPED_AT;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.STOPPED;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static java.text.MessageFormat.format;

public class EligibleForTerminationPredicate implements Predicate<Instance> {
    private final PluginSettings pluginSettings;
    private Clock clock;

    public EligibleForTerminationPredicate(PluginSettings pluginSettings) {
        this(pluginSettings, Clock.DEFAULT);
    }

    EligibleForTerminationPredicate(PluginSettings pluginSettings, Clock clock) {
        this.pluginSettings = pluginSettings;
        this.clock = clock;
    }

    @Override
    public boolean test(Instance instance) {
        if (isNotInSoppedState(instance)) {
            return false;
        }

        final Optional<String> stoppedAt = instance.getTags().stream()
                .filter(tag -> tag.getKey().equals(STOPPED_AT))
                .findFirst()
                .map(Tag::getValue);

        if (!stoppedAt.isPresent()) {
            LOG.info(format("Instance {0} does not have STOPPED_AT tag. Instance without tag is eligible for termination.", instance.getInstanceId()));
            return true;
        }

        Period timeInstanceCanStayStopped = getTimeInstanceCanStayStopped(instance);

        final DateTime instanceStoppedAt = new DateTime(Long.parseLong(stoppedAt.get()));
        return clock.now().isAfter(instanceStoppedAt.plus(timeInstanceCanStayStopped));
    }

    private boolean isNotInSoppedState(Instance instance) {
        return !STOPPED.equalsIgnoreCase(instance.getState().getName());
    }

    private Period getTimeInstanceCanStayStopped(Instance instance) {
        return Platform.from(instance.getPlatform()) == LINUX ? pluginSettings.terminateStoppedLinuxInstanceAfter() : pluginSettings.terminateStoppedWindowsInstanceAfter();
    }
}
