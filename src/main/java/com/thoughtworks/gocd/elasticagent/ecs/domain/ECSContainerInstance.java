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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.VersionInfo;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.List;

@EqualsAndHashCode
public class ECSContainerInstance {

    private final String containerInstanceArn;
    private final String id;
    private final String ec2InstanceId;
    private final String status;
    private final boolean agentConnected;
    private final String agentUpdateStatus;
    private final VersionInfo dockerInfo;
    private final ContainerResources remainingResources;
    private final ContainerResources registeredResources;
    private final Instance instance;
    private final List<ECSContainer> containers;
    private final String runningSince;
    private final String platform;

    public ECSContainerInstance(ContainerInstance containerInstance, Instance instance, List<ECSContainer> containers) {
        agentConnected = containerInstance.getAgentConnected();
        agentUpdateStatus = containerInstance.getAgentUpdateStatus();
        containerInstanceArn = containerInstance.getContainerInstanceArn();
        ec2InstanceId = containerInstance.getEc2InstanceId();
        status = containerInstance.getStatus();
        dockerInfo = containerInstance.getVersionInfo();
        remainingResources = new ContainerResources(containerInstance.getRemainingResources());
        registeredResources = new ContainerResources(containerInstance.getRegisteredResources());
        this.platform = Platform.from(instance.getPlatform()).name();
        this.instance = instance;
        this.containers = containers;
        final String[] arnParts = containerInstance.getContainerInstanceArn().split("/");
        this.id = arnParts[arnParts.length - 1];
        this.runningSince = Util.PERIOD_FORMATTER.print(new Period(new DateTime(instance.getLaunchTime()), DateTime.now()));
    }

    public boolean getAgentConnected() {
        return agentConnected;
    }

    public String getAgentUpdateStatus() {
        return agentUpdateStatus;
    }

    public String getContainerInstanceArn() {
        return containerInstanceArn;
    }

    public String getEc2InstanceId() {
        return ec2InstanceId;
    }

    public String getStatus() {
        return status;
    }

    public VersionInfo getDockerInfo() {
        return dockerInfo;
    }

    public ContainerResources getRemainingResources() {
        return remainingResources;
    }

    public ContainerResources getRegisteredResources() {
        return registeredResources;
    }

    public Instance getInstance() {
        return instance;
    }

    public List<ECSContainer> getContainers() {
        return containers;
    }

    public String getId() {
        return id;
    }

    public String getRunningSince() {
        return runningSince;
    }

    public String getPlatform() {
        return platform;
    }
}
