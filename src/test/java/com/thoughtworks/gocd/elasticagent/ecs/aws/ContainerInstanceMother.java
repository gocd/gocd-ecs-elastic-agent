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

import com.amazonaws.services.ecs.model.Attribute;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.Resource;
import com.amazonaws.services.ecs.model.VersionInfo;

import java.util.Random;
import java.util.UUID;

import static com.amazonaws.services.ecs.model.AgentUpdateStatus.UPDATED;

public class ContainerInstanceMother {
    public static ContainerInstance containerInstance(String ec2InstanceId) {
        return new ContainerInstance()
                .withEc2InstanceId(ec2InstanceId)
                .withAgentConnected(true)
                .withAgentUpdateStatus(UPDATED)
                .withStatus("ACTIVE")
                .withContainerInstanceArn(String.format("arn:aws:ecs:us-west-2:%s:container-instance/%s", new Random().nextInt(), UUID.randomUUID()));
    }

    public static ContainerInstance containerInstance(String ec2InstanceId, int pendingTasksCount, int runningTasksCount) {
        return containerInstance(ec2InstanceId)
                .withPendingTasksCount(pendingTasksCount)
                .withRunningTasksCount(runningTasksCount)
                .withAgentConnected(true);
    }

    public static ContainerInstance containerInstance(String ec2InstanceId, String containerInstanceArn) {
        return containerInstance(ec2InstanceId)
                .withContainerInstanceArn(containerInstanceArn);
    }

    public static ContainerInstance containerInstance(String ec2InstanceId, boolean agentConnected) {
        return containerInstance(ec2InstanceId)
                .withAgentConnected(agentConnected);
    }

    public static ContainerInstance containerInstance(String instanceId, String containerInstanceArn, String status, int registeredCPU, int registeredMemory, int remainingCPU, int remainingMemory) {
        return containerInstance(instanceId, containerInstanceArn)
                .withAgentConnected(true)
                .withAgentUpdateStatus(UPDATED)
                .withStatus(status)
                .withRegisteredResources(
                        resourceWith("cpu", registeredCPU),
                        resourceWith("memory", registeredMemory)
                )
                .withRemainingResources(
                        resourceWith("CPU", remainingCPU),
                        resourceWith("memory", remainingMemory)
                ).withVersionInfo(versionInfo("1.13", "1.24"));
    }

    public static ContainerInstance containerInstance(String ec2InstanceId, Attribute... attributes) {
        return containerInstance(ec2InstanceId)
                .withAttributes(attributes);
    }

    public static Resource resourceWith(String resourceName, int registeredCPU) {
        return new Resource().withName(resourceName).withType("INTEGER").withIntegerValue(registeredCPU);
    }

    public static VersionInfo versionInfo(String dockerVersion, String agentVersion) {
        return new VersionInfo().withAgentVersion(agentVersion).withDockerVersion(dockerVersion);
    }
}
