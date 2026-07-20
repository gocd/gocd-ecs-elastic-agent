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

import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;
import software.amazon.awssdk.services.ecs.model.VersionInfo;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

@EqualsAndHashCode
public class ECSContainerInstance {

    private final String containerInstanceArn;
    private final String id;
    private final String ec2InstanceId;
    private final String status;
    private final boolean agentConnected;
    private final String agentUpdateStatus;
    private final String dockerVersion;
    private final String agentVersion;
    private final ContainerResources remainingResources;
    private final ContainerResources registeredResources;
    private final String imageId;
    private final String instanceType;
    private final String instanceState;
    private final Long launchTimeMillis;
    private final List<ECSContainer> containers;
    private final String runningSince;
    private final String platform;

    public ECSContainerInstance(ContainerInstance containerInstance, Instance instance, List<ECSContainer> containers) {
        agentConnected = containerInstance.agentConnected();
        agentUpdateStatus = containerInstance.agentUpdateStatusAsString();
        containerInstanceArn = containerInstance.containerInstanceArn();
        ec2InstanceId = containerInstance.ec2InstanceId();
        status = containerInstance.status();
        final VersionInfo versionInfo = containerInstance.versionInfo();
        dockerVersion = versionInfo == null ? null : versionInfo.dockerVersion();
        agentVersion = versionInfo == null ? null : versionInfo.agentVersion();
        remainingResources = new ContainerResources(containerInstance.remainingResources());
        registeredResources = new ContainerResources(containerInstance.registeredResources());
        this.platform = Platform.from(instance.platformAsString()).name();
        this.imageId = instance.imageId();
        this.instanceType = instance.instanceTypeAsString();
        this.instanceState = instance.state() == null ? null : instance.state().nameAsString();
        this.launchTimeMillis = instance.launchTime() == null ? null : instance.launchTime().toEpochMilli();
        this.containers = containers;
        final String[] arnParts = containerInstance.containerInstanceArn().split("/");
        this.id = arnParts[arnParts.length - 1];
        this.runningSince = Util.formatDurationWordsFromNow(Objects.requireNonNullElseGet(instance.launchTime(), Instant::now));
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

    public String getDockerVersion() {
        return dockerVersion;
    }

    public String getAgentVersion() {
        return agentVersion;
    }

    public ContainerResources getRemainingResources() {
        return remainingResources;
    }

    public ContainerResources getRegisteredResources() {
        return registeredResources;
    }

    public String getImageId() {
        return imageId;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getInstanceState() {
        return instanceState;
    }

    public Long getLaunchTimeMillis() {
        return launchTimeMillis;
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
