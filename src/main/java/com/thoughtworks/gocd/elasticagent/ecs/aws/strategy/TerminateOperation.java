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
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import software.amazon.awssdk.services.ec2.model.TerminateInstancesRequest;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;
import software.amazon.awssdk.services.ecs.model.DeregisterContainerInstanceRequest;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static java.text.MessageFormat.format;

public class TerminateOperation implements Operation<ContainerInstance> {
    private static final Logger LOG = Logger.getLoggerFor(TerminateOperation.class);

    @Override
    public void execute(PluginSettings pluginSettings, Collection<ContainerInstance> containerInstanceToTerminate) {
        if (containerInstanceToTerminate.isEmpty()) {
            LOG.info("No instances to terminate.");
            return;
        }

        final Set<String> instancesToTerminate = containerInstanceToTerminate.stream()
                .map(ContainerInstance::ec2InstanceId)
                .collect(Collectors.toSet());

        LOG.info(format("Terminating idle container instances {0}.", instancesToTerminate));

        containerInstanceToTerminate.forEach(containerInstance -> deregisterContainerInstance(pluginSettings, containerInstance));

        pluginSettings.ec2Client().terminateInstances(TerminateInstancesRequest.builder().instanceIds(instancesToTerminate).build());
        LOG.info(format("Container instances {0} terminated.", instancesToTerminate));
    }

    private void deregisterContainerInstance(PluginSettings pluginSettings, ContainerInstance containerInstanceToDeregister) {
        pluginSettings.ecsClient().deregisterContainerInstance(DeregisterContainerInstanceRequest.builder()
                .containerInstance(containerInstanceToDeregister.containerInstanceArn())
                .cluster(pluginSettings.getClusterName())
                .force(true)
                .build()
        );
    }

}
