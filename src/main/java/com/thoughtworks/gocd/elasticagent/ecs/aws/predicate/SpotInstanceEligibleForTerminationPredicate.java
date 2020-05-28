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

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static java.text.MessageFormat.format;

public class SpotInstanceEligibleForTerminationPredicate implements Predicate<Instance> {
    private final PluginSettings pluginSettings;
    private Clock clock;

    public SpotInstanceEligibleForTerminationPredicate(PluginSettings pluginSettings) {
        this(pluginSettings, Clock.DEFAULT);
    }

    SpotInstanceEligibleForTerminationPredicate(PluginSettings pluginSettings, Clock clock) {
        this.pluginSettings = pluginSettings;
        this.clock = clock;
    }

    @Override
    public boolean test(Instance instance) {
        final Optional<String> idleSince = instance.getTags().stream()
                .filter(tag -> tag.getKey().equals(LAST_SEEN_IDLE))
                .findFirst()
                .map(Tag::getValue);

        if (!idleSince.isPresent()) {
            LOG.info(format("Spot Instance {0} does not have LAST_SEEN_IDLE tag. Instance without this tag is not eligible for termination.", instance.getInstanceId()));
            return false;
        }

        Period timeInstanceCanStayIdle = getTimeInstanceCanStayIdle(instance);
        final DateTime instanceIdleSince = new DateTime(Long.parseLong(idleSince.get()));

        return clock.now().isAfter(instanceIdleSince.plus(timeInstanceCanStayIdle));
    }

    private Period getTimeInstanceCanStayIdle(Instance instance) {
        return Platform.from(instance.getPlatform()) == LINUX ? pluginSettings.terminateIdleLinuxSpotInstanceAfter() : pluginSettings.terminateIdleWindowsSpotInstanceAfter();
    }
}
