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

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.apache.commons.lang3.StringUtils;
import software.amazon.awssdk.services.ecs.model.*;

import java.util.ArrayList;
import java.util.List;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class RegisterTaskDefinitionRequestBuilder {
    private static final Logger LOG = Logger.getLoggerFor(RegisterTaskDefinitionRequestBuilder.class);

    public RegisterTaskDefinitionRequest build(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, ContainerDefinition.Builder containerDefinitionBuilder, String taskName) {

        final RegisterTaskDefinitionRequest.Builder request = RegisterTaskDefinitionRequest.builder()
                .family(taskName)
                .taskRoleArn(StringUtils.defaultIfBlank(elasticAgentProfileProperties.getTaskRoleArn(), null));

        if (elasticAgentProfileProperties.platform() == Platform.WINDOWS) {
            return request.containerDefinitions(containerDefinitionBuilder.build()).build();
        }

        List<Volume> volumes = new ArrayList<>();
        List<MountPoint> mountPoints = new ArrayList<>();

        if (isNotBlank(pluginSettings.efsDnsOrIP())) {
            LOG.info(format("[create-agent] Adding EFS volume {0} to task configuration.", pluginSettings.efsDnsOrIP()));
            volumes.add(Volume.builder()
                    .name("efs")
                    .host(HostVolumeProperties.builder().sourcePath(pluginSettings.efsMountLocation()).build())
                    .build());

            mountPoints.add(MountPoint.builder()
                    .sourceVolume("efs")
                    .containerPath(pluginSettings.efsMountLocation())
                    .build());
        }

        if (elasticAgentProfileProperties.isMountDockerSocket()) {
            LOG.info("[create-agent] Adding /var/run/docker.sock to task configuration.");
            volumes.add(Volume.builder()
                    .name("DockerSocket")
                    .host(HostVolumeProperties.builder().sourcePath("/var/run/docker.sock").build())
                    .build());

            mountPoints.add(MountPoint.builder()
                    .sourceVolume("DockerSocket")
                    .containerPath("/var/run/docker.sock")
                    .build());
        }

        if (!elasticAgentProfileProperties.bindMounts().isEmpty()) {
            elasticAgentProfileProperties.bindMounts().forEach(bindMount -> {
                volumes.add(Volume.builder()
                        .name(bindMount.getName())
                        .host(HostVolumeProperties.builder().sourcePath(bindMount.getSourcePath()).build())
                        .build());
                mountPoints.add(MountPoint.builder()
                        .sourceVolume(bindMount.getName())
                        .containerPath(bindMount.getContainerPath())
                        .build());
            });
        }

        if (!mountPoints.isEmpty()) {
            containerDefinitionBuilder.mountPoints(mountPoints);
        }

        return request
                .volumes(volumes)
                .containerDefinitions(containerDefinitionBuilder.build())
                .build();
    }
}
