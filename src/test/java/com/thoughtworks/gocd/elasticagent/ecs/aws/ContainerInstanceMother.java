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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

import software.amazon.awssdk.services.ecs.model.Attribute;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;
import software.amazon.awssdk.services.ecs.model.Resource;
import software.amazon.awssdk.services.ecs.model.VersionInfo;

import java.util.Random;
import java.util.UUID;

import static software.amazon.awssdk.services.ecs.model.AgentUpdateStatus.UPDATED;

public class ContainerInstanceMother {
    public static ContainerInstance containerInstance(String ec2InstanceId) {
        return containerInstanceBuilder(ec2InstanceId)
                .build();
    }

    public static ContainerInstance containerInstance(String ec2InstanceId, int pendingTasksCount, int runningTasksCount) {
        return containerInstanceBuilder(ec2InstanceId)
                .pendingTasksCount(pendingTasksCount)
                .runningTasksCount(runningTasksCount)
                .agentConnected(true)
                .build();
    }

    public static ContainerInstance containerInstance(String ec2InstanceId, String containerInstanceArn) {
        return containerInstanceBuilder(ec2InstanceId, containerInstanceArn)
                .build();
    }

    public static ContainerInstance containerInstance(String ec2InstanceId, boolean agentConnected) {
        return containerInstanceBuilder(ec2InstanceId)
                .agentConnected(agentConnected)
                .build();
    }

    public static ContainerInstance containerInstance(String instanceId, String containerInstanceArn, String status, int registeredCPU, int registeredMemory, int remainingCPU, int remainingMemory) {
        return containerInstanceBuilder(instanceId, containerInstanceArn)
                .agentConnected(true)
                .agentUpdateStatus(UPDATED)
                .status(status)
                .registeredResources(
                        resourceWith("cpu", registeredCPU),
                        resourceWith("memory", registeredMemory)
                )
                .remainingResources(
                        resourceWith("CPU", remainingCPU),
                        resourceWith("memory", remainingMemory)
                ).versionInfo(versionInfo("1.13", "1.24"))
                .build();
    }

    public static ContainerInstance.Builder containerInstanceBuilder(String ec2InstanceId, Attribute... attributes) {
        return containerInstanceBuilder(ec2InstanceId)
                .attributes(attributes);
    }

    public static Resource resourceWith(String resourceName, int registeredCPU) {
        return Resource.builder().name(resourceName).type("INTEGER").integerValue(registeredCPU).build();
    }

    public static VersionInfo versionInfo(String dockerVersion, String agentVersion) {
        return VersionInfo.builder().agentVersion(agentVersion).dockerVersion(dockerVersion).build();
    }

    public static ContainerInstance.Builder containerInstanceBuilder(String ec2InstanceId) {
        return ContainerInstance.builder()
                .ec2InstanceId(ec2InstanceId)
                .agentConnected(true)
                .agentUpdateStatus(UPDATED)
                .status("ACTIVE")
                .containerInstanceArn(String.format("arn:aws:ecs:us-west-2:%s:container-instance/%s", new Random().nextInt(), UUID.randomUUID()));
    }

    public static ContainerInstance.Builder containerInstanceBuilder(String ec2InstanceId, String containerInstanceArn) {
        return containerInstanceBuilder(ec2InstanceId)
                .containerInstanceArn(containerInstanceArn);
    }
}
