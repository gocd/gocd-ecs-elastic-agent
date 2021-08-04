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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

import com.amazonaws.services.ecs.model.*;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class RegisterTaskDefinitionRequestBuilder {

    public RegisterTaskDefinitionRequest build(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, ContainerDefinition containerDefinition) {

        final RegisterTaskDefinitionRequest request = new RegisterTaskDefinitionRequest()
                .withContainerDefinitions(containerDefinition)
                .withTaskRoleArn(elasticAgentProfileProperties.getTaskRoleArn());

        if (elasticAgentProfileProperties.platform() == Platform.WINDOWS) {
            return request;
        }

        if (isNotBlank(pluginSettings.efsDnsOrIP())) {
            LOG.info(format("[create-agent] Adding EFS volume {0} to task configuration.", pluginSettings.efsDnsOrIP()));
            request.withVolumes(new Volume()
                    .withName("efs")
                    .withHost(new HostVolumeProperties().withSourcePath(pluginSettings.efsMountLocation())));

            request.getContainerDefinitions().get(0).withMountPoints(new MountPoint()
                    .withSourceVolume("efs")
                    .withContainerPath(pluginSettings.efsMountLocation()));
        }

        if (elasticAgentProfileProperties.isMountDockerSocket()) {
            LOG.info("[create-agent] Adding /var/run/docker.sock to task configuration.");
            request.withVolumes(new Volume()
                    .withName("DockerSocket")
                    .withHost(new HostVolumeProperties().withSourcePath("/var/run/docker.sock")));

            request.getContainerDefinitions().get(0)
                    .withMountPoints(new MountPoint()
                            .withSourceVolume("DockerSocket")
                            .withContainerPath("/var/run/docker.sock")
                    );
        }

        if (!elasticAgentProfileProperties.bindMounts().isEmpty()) {
            elasticAgentProfileProperties.bindMounts().forEach(bindMount -> {
                request.withVolumes(new Volume()
                        .withName(bindMount.getName())
                        .withHost(new HostVolumeProperties().withSourcePath(bindMount.getSourcePath()))
                );
                request.getContainerDefinitions().get(0).withMountPoints(new MountPoint()
                        .withSourceVolume(bindMount.getName())
                        .withContainerPath(bindMount.getContainerPath()));
            });
        }

        if (isNotBlank(elasticAgentProfileProperties.getExecutionRoleArn())) {
                LOG.info(format("[create-agent] Adding execution Role to task configuration: {0}", elasticAgentProfileProperties.getExecutionRoleArn()));
                request.withExecutionRoleArn(elasticAgentProfileProperties.getExecutionRoleArn());
        }

        return request;
    }
}
