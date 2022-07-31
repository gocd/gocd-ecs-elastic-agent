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

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ContainerResources;

public class ContainerInstanceMatcher {
    public static final String INACTIVE = "INACTIVE";

    public boolean matches(ContainerInstance containerInstance, ContainerDefinition containerDefinition) {
        if (isDisconnected(containerInstance) || notActive(containerInstance)) {
            return false;
        }

        ContainerResources resources = new ContainerResources(containerInstance.getRemainingResources());
        return isCPUAvailable(resources, containerDefinition) && isMemoryAvailable(resources, containerDefinition);
    }

    private boolean notActive(ContainerInstance containerInstance) {
        return INACTIVE.equals(containerInstance.getStatus());
    }

    private Boolean isDisconnected(ContainerInstance containerInstance) {
        return !containerInstance.isAgentConnected();
    }

    private boolean isMemoryAvailable(ContainerResources resource, ContainerDefinition containerDefinition) {
        return containerDefinition.getMemory() == null || containerDefinition.getMemory().doubleValue() < resource.getMemory();
    }

    private boolean isCPUAvailable(ContainerResources resource, ContainerDefinition containerDefinition) {
        return containerDefinition.getCpu() == null || containerDefinition.getCpu().doubleValue() < resource.getCpu();
    }
}
