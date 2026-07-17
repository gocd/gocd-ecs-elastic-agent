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

package com.thoughtworks.gocd.elasticagent.ecs.aws.matcher;

import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerDefinitionBuilder;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ContainerResources;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;

public class ContainerInstanceMatcher {
    public static final String INACTIVE = "INACTIVE";

    public boolean matches(ContainerInstance containerInstance, ContainerDefinitionBuilder.PlacementRequirement containerDefinition) {
        if (isDisconnected(containerInstance) || notActive(containerInstance)) {
            return false;
        }

        ContainerResources resources = new ContainerResources(containerInstance.remainingResources());
        return isCPUAvailable(resources, containerDefinition) && isMemoryAvailable(resources, containerDefinition);
    }

    private boolean notActive(ContainerInstance containerInstance) {
        return INACTIVE.equals(containerInstance.status());
    }

    private Boolean isDisconnected(ContainerInstance containerInstance) {
        return !containerInstance.agentConnected();
    }

    private boolean isMemoryAvailable(ContainerResources resource, ContainerDefinitionBuilder.PlacementRequirement containerDefinition) {
        return containerDefinition.memory() == null || containerDefinition.memory().doubleValue() < resource.getMemory();
    }

    private boolean isCPUAvailable(ContainerResources resource, ContainerDefinitionBuilder.PlacementRequirement containerDefinition) {
        return containerDefinition.cpu() == null || containerDefinition.cpu().doubleValue() < resource.getCpu();
    }
}
