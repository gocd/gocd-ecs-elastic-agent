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

package com.thoughtworks.gocd.elasticagent.ecs.aws.strategy;

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerDefinitionBuilder;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.EC2Config;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.ContainerInstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.InstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.toMap;
import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;
import static software.amazon.awssdk.services.ec2.model.InstanceStateName.PENDING;
import static software.amazon.awssdk.services.ec2.model.InstanceStateName.RUNNING;

public abstract class InstanceSelectionStrategy {
    public static final Set<InstanceStateName> ACCEPTABLE_STATES = Set.of(PENDING, RUNNING);
    private static final Logger LOG = Logger.getLoggerFor(InstanceSelectionStrategy.class);

    protected final ContainerInstanceHelper containerInstanceHelper;
    final InstanceMatcher instanceMatcher;
    final ContainerInstanceMatcher containerInstanceMatcher;

    public InstanceSelectionStrategy(ContainerInstanceHelper containerInstanceHelper) {
        this(containerInstanceHelper, new InstanceMatcher(), new ContainerInstanceMatcher());
    }

    InstanceSelectionStrategy(ContainerInstanceHelper containerInstanceHelper, InstanceMatcher instanceMatcher, ContainerInstanceMatcher containerInstanceMatcher) {
        this.containerInstanceHelper = containerInstanceHelper;
        this.instanceMatcher = instanceMatcher;
        this.containerInstanceMatcher = containerInstanceMatcher;
    }

    protected abstract List<ContainerInstance> findInstancesToStop(PluginSettings pluginSettings, Platform platform, Map<String, ContainerInstance> instanceIdToContainerInstance, List<Instance> idleInstances);

    protected abstract void sortInstancesForScheduling(List<Instance> ec2Instances);

    public Optional<ContainerInstance> instanceForScheduling(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, ContainerDefinitionBuilder.PlacementRequirement placementRequirement) {
        List<ContainerInstance> containerInstanceList = containerInstanceHelper.getContainerInstances(pluginSettings);

        final EC2Config ec2Config = new EC2Config.Builder()
                .profile(elasticAgentProfileProperties)
                .settings(pluginSettings)
                .build();

        if (containerInstanceList.isEmpty()) {
            return Optional.empty();
        }

        final List<Instance> ec2Instances = containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstanceList)
                .stream().filter(instance -> ACCEPTABLE_STATES.contains(instance.state().name()))
                .collect(toList());

        final Map<String, ContainerInstance> instanceMap = toMap(containerInstanceList, ContainerInstance::ec2InstanceId, containerInstance -> containerInstance);

        sortInstancesForScheduling(ec2Instances);

        for (Instance instance : ec2Instances) {
            final ContainerInstance containerInstance = instanceMap.get(instance.instanceId());
            if (instanceMatcher.matches(ec2Config, instance) && containerInstanceMatcher.matches(containerInstance, placementRequirement)) {
                if (isSpotInstance(instance)) {
                    containerInstanceHelper.removeLastSeenIdleTag(pluginSettings, Collections.singletonList(instance.instanceId()));
                }
                return Optional.of(containerInstance);
            } else {
                LOG.info(format("Skipped container creation on container instance {0}: required resources are not available.", instance.instanceId()));
            }
        }

        return Optional.empty();
    }

    private boolean isSpotInstance(Instance instance) {
        return isNotEmpty(instance.spotInstanceRequestId());
    }

    public Optional<List<ContainerInstance>> instancesToStop(PluginSettings pluginSettings, Platform platform) {
        List<ContainerInstance> allContainerInstances = containerInstanceHelper.onDemandContainerInstances(pluginSettings);

        if (allContainerInstances.isEmpty()) {
            LOG.debug("Cluster is already scaled in.");
            return Optional.empty();
        }

        final List<Instance> instancesWithPlatform = containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, allContainerInstances)
                .stream()
                .filter(instance -> ACCEPTABLE_STATES.contains(instance.state().name()))
                .filter(instance -> Platform.from(instance.platformAsString()) == platform)
                .collect(toList());

        if (instancesWithPlatform.size() <= minInstanceCount(platform, pluginSettings)) {
            LOG.debug(format("Skipping scaling in of {0} instance as cluster is already scaled in.", platform.name().toLowerCase()));
            return Optional.empty();
        }

        final Map<String, ContainerInstance> instanceIdToContainerInstance = Util.toMap(allContainerInstances, ContainerInstance::ec2InstanceId, self -> self);

        final List<Instance> idleInstances = idleContainerInstances(instancesWithPlatform, instanceIdToContainerInstance);
        if (idleInstances.isEmpty()) {
            LOG.debug("No idle instance to terminate.");
            return Optional.empty();
        }

        final List<ContainerInstance> instancesToStop = findInstancesToStop(pluginSettings, platform, instanceIdToContainerInstance, idleInstances);
        return instancesToStop == null || instancesToStop.isEmpty() ? Optional.empty() : Optional.of(instancesToStop);
    }


    private List<Instance> idleContainerInstances(List<Instance> instances, Map<String, ContainerInstance> containerInstanceMap) {
        return instances.stream().filter(instance -> isIdle(containerInstanceMap.get(instance.instanceId())))
                .collect(Collectors.toList());
    }

    private boolean isIdle(ContainerInstance containerInstance) {
        return containerInstance.pendingTasksCount() == 0 && containerInstance.runningTasksCount() == 0;
    }

    private int minInstanceCount(Platform platform, PluginSettings pluginSettings) {
        return platform == WINDOWS ? pluginSettings.getMinWindowsInstanceCount() : pluginSettings.getMinLinuxInstanceCount();
    }
}
