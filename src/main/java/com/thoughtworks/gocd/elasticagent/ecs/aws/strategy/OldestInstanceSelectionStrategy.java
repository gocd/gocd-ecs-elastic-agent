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
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.ContainerInstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.InstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class OldestInstanceSelectionStrategy extends InstanceSelectionStrategy {

    public OldestInstanceSelectionStrategy() {
        super(new ContainerInstanceHelper());
    }

    OldestInstanceSelectionStrategy(ContainerInstanceHelper containerInstanceHelper, InstanceMatcher instanceMatcher, ContainerInstanceMatcher containerInstanceMatcher) {
        super(containerInstanceHelper, instanceMatcher, containerInstanceMatcher);
    }

    @Override
    protected List<ContainerInstance> findInstancesToStop(PluginSettings pluginSettings, Platform platform, Map<String, ContainerInstance> instanceIdToContainerInstance, List<Instance> idleInstances) {
        idleInstances.sort(Comparator.comparing(Instance::getLaunchTime));

        final Instance instance = idleInstances.get(0);

        return singletonList(instanceIdToContainerInstance.get(instance.getInstanceId()));
    }

    @Override
    protected void sortInstancesForScheduling(List<Instance> ec2Instances) {
        ec2Instances.sort(Comparator.comparing(Instance::getLaunchTime).reversed());
    }
}
