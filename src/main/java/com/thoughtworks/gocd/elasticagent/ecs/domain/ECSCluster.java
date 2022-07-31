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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.ContainerInstance;
import lombok.EqualsAndHashCode;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper.*;
import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.toMap;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.http.util.TextUtils.isEmpty;

@EqualsAndHashCode
public class ECSCluster {
    private final String name;
    private final int maxLinuxInstancesAllowed;
    private final int maxWindowsInstancesAllowed;
    private final int maxLinuxSpotInstanceAllowed;
    private final int maxWindowsSpotInstanceAllowed;
    private final Integer runningTasksCount;
    private final List<ECSContainerInstance> containerInstances;
    private final Integer pendingTasksCount;
    private final Integer registeredWindowsOnDemandInstanceCount;
    private final Integer registeredLinuxOnDemandInstanceCount;
    private final Integer registeredWindowsSpotInstanceCount;
    private final Integer registeredLinuxSpotInstanceCount;
    private final Map<String, String> ec2InstanceType;

    public ECSCluster(Cluster cluster, List<ContainerInstance> containerInstances, List<Instance> ec2Instances,
                      List<ECSContainer> ecsContainers, int maxLinuxInstancesAllowed, int maxWindowsInstancesAllowed,
                      int maxLinuxSpotInstanceAllowed, int maxWindowsSpotInstanceAllowed) {
        this.name = cluster.getClusterName();
        this.maxLinuxInstancesAllowed = maxLinuxInstancesAllowed;
        this.maxWindowsInstancesAllowed = maxWindowsInstancesAllowed;
        this.maxLinuxSpotInstanceAllowed = maxLinuxSpotInstanceAllowed;
        this.maxWindowsSpotInstanceAllowed = maxWindowsSpotInstanceAllowed;

        this.ec2InstanceType = ec2Instances.stream().collect(Collectors.toMap(i -> i.getInstanceId(), i -> instanceType(i)));
        final Map<Platform, List<Instance>> platformRoInstances = groupByPlatform(ec2Instances);
        this.registeredWindowsOnDemandInstanceCount = filterBy(platformRoInstances.getOrDefault(Platform.WINDOWS, emptyList()), isOnDemandInstance()).size();
        this.registeredLinuxOnDemandInstanceCount = filterBy(platformRoInstances.getOrDefault(Platform.LINUX, emptyList()), isOnDemandInstance()).size();
        this.registeredWindowsSpotInstanceCount = filterBy(platformRoInstances.getOrDefault(Platform.WINDOWS, emptyList()), isSpotInstance()).size();
        this.registeredLinuxSpotInstanceCount = filterBy(platformRoInstances.getOrDefault(Platform.LINUX, emptyList()), isSpotInstance()).size();
        this.runningTasksCount = cluster.getRunningTasksCount();
        this.pendingTasksCount = cluster.getPendingTasksCount();

        final Map<String, Instance> ec2InstanceMap = toMap(ec2Instances, Instance::getInstanceId, self -> self);
        final Map<String, List<ECSContainer>> ecsContainerMap = getMap(ecsContainers);

        this.containerInstances = containerInstances.stream().map(c -> new ECSContainerInstance(c, ec2InstanceMap.get(c.getEc2InstanceId()), ecsContainerMap.get(c.getContainerInstanceArn()))).collect(toList());
    }

    private String instanceType(Instance instance) {
        return isEmpty(instance.getSpotInstanceRequestId()) ? "On-Demand" : "Spot";
    }

    private Map<String, List<ECSContainer>> getMap(List<ECSContainer> containers) {
        return containers.stream().collect(
                Collectors.groupingBy(ECSContainer::getContainerInstanceArn, HashMap::new, Collectors.toCollection(ArrayList::new))
        );
    }

    public String getName() {
        return name;
    }

    public Integer getRunningTasksCount() {
        return runningTasksCount;
    }

    public List<ECSContainerInstance> getContainerInstances() {
        return containerInstances;
    }

    public Integer getPendingTasksCount() {
        return pendingTasksCount;
    }

    public Integer getRegisteredWindowsOnDemandInstanceCount() {
        return registeredWindowsOnDemandInstanceCount;
    }

    public Integer getRegisteredLinuxOnDemandInstanceCount() {
        return registeredLinuxOnDemandInstanceCount;
    }

    public Integer getRegisteredWindowsSpotInstanceCount() {
        return registeredWindowsSpotInstanceCount;
    }

    public Integer getRegisteredLinuxSpotInstanceCount() {
        return registeredLinuxSpotInstanceCount;
    }

    public int getMaxLinuxInstancesAllowed() {
        return maxLinuxInstancesAllowed;
    }

    public int getMaxWindowsInstancesAllowed() {
        return maxWindowsInstancesAllowed;
    }

    public int getMaxLinuxSpotInstanceAllowed() {
        return maxLinuxSpotInstanceAllowed;
    }

    public int getMaxWindowsSpotInstanceAllowed() {
        return maxWindowsSpotInstanceAllowed;
    }

    public Map<String, String> getEc2InstanceType() {
        return ec2InstanceType;
    }
}
