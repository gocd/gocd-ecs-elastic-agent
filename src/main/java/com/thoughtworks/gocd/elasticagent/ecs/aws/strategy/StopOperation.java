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

import com.amazonaws.services.ec2.model.CreateTagsRequest;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.STOPPED_AT;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static java.text.MessageFormat.format;

public class StopOperation implements Operation<ContainerInstance> {

    @Override
    public void execute(PluginSettings pluginSettings, Collection<ContainerInstance> containerInstanceToStop) {
        if (containerInstanceToStop.isEmpty()) {
            LOG.info("No instances to stop.");
            return;
        }

        final Set<String> instanceIds = containerInstanceToStop.stream()
                .map(ContainerInstance::getEc2InstanceId).collect(Collectors.toSet());


        LOG.info(format("Adding STOPPED_AT tag to container instances {0}.", instanceIds));
        pluginSettings.ec2Client().createTags(
                new CreateTagsRequest()
                        .withTags(new Tag(STOPPED_AT, String.valueOf(Clock.DEFAULT.now().getMillis())))
                        .withResources(instanceIds)
        );

        LOG.info(format("Stopping idle container instances {0}.", instanceIds));
        final StopInstancesRequest stopInstancesRequest = new StopInstancesRequest()
                .withInstanceIds(instanceIds);
        pluginSettings.ec2Client().stopInstances(stopInstancesRequest);
        LOG.info(format("Container instances {0} stopped.", instanceIds));
    }
}
