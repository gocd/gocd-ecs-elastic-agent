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
import com.thoughtworks.gocd.elasticagent.ecs.domain.BindMount;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import java.util.Collections;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class RegisterTaskDefinitionRequestBuilderTest {

    @Mock
    private PluginSettings pluginSettings;
    @Mock
    private ElasticAgentProfileProperties elasticAgentProfileProperties;
    @Mock
    private ContainerDefinition containerDefinition;
    private RegisterTaskDefinitionRequestBuilder builder;

    @BeforeEach
    void setUp() {
        openMocks(this);
        builder = new RegisterTaskDefinitionRequestBuilder();

    }

    @Test
    void shouldBuildRegisterTaskDefinitionRequest() {
        final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinition);

        assertThat(request.getContainerDefinitions()).hasSize(1);
        assertThat(request.getContainerDefinitions()).contains(containerDefinition);

        assertThat(request.getVolumes()).hasSize(0);
        verify(containerDefinition, times(0)).getMountPoints();
    }

    @Test
    void shouldRegisterTaskDefinitionRequestWithTaskRoleArn() {
        when(elasticAgentProfileProperties.getTaskRoleArn()).thenReturn("task-role-arn");

        final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinition);

        assertThat(request.getTaskRoleArn()).isEqualTo("task-role-arn");
    }

    @Test
    void ifBindMountConfigured_shouldRegisterTaskDefinitionRequestWithVolumeAndContainerDefinitionWithMountPoint() {
        BindMount bindMount = new BindMount("data", "sourcePath", "containerPath");
        when(elasticAgentProfileProperties.bindMounts()).thenReturn(singletonList(bindMount));

        final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, new ContainerDefinition());

        assertThat(request.getVolumes().size()).isEqualTo(1);

        Volume volume = request.getVolumes().get(0);
        assertThat(volume.getName()).isEqualTo(bindMount.getName());
        assertThat(volume.getHost().getSourcePath()).isEqualTo(bindMount.getSourcePath());

        ContainerDefinition containerDefinition = request.getContainerDefinitions().get(0);
        assertThat(containerDefinition.getMountPoints().size()).isEqualTo(1);

        MountPoint mountPoint = containerDefinition.getMountPoints().get(0);
        assertThat(mountPoint.getContainerPath()).isEqualTo(bindMount.getContainerPath());
        assertThat(mountPoint.getSourceVolume()).isEqualTo(bindMount.getName());
    }

    @Test
    void ifMultipleBindMountsConfigured_shouldRegisterTaskDefinitionRequestWithVolumesAndContainerDefinitionWithMountPoints() {
        BindMount bindMount1 = new BindMount("data1", "sourcePath1", "containerPath1");
        BindMount bindMount2 = new BindMount("data2", "sourcePath2", "containerPath2");
        when(elasticAgentProfileProperties.bindMounts()).thenReturn(asList(bindMount1, bindMount2));

        final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, new ContainerDefinition());

        ContainerDefinition containerDefinition = request.getContainerDefinitions().get(0);

        assertThat(request.getVolumes().size()).isEqualTo(2);
        assertThat(containerDefinition.getMountPoints().size()).isEqualTo(2);

        Volume volume1 = request.getVolumes().get(0);
        assertThat(volume1.getName()).isEqualTo(bindMount1.getName());
        assertThat(volume1.getHost().getSourcePath()).isEqualTo(bindMount1.getSourcePath());

        MountPoint mountPoint1 = containerDefinition.getMountPoints().get(0);
        assertThat(mountPoint1.getContainerPath()).isEqualTo(bindMount1.getContainerPath());
        assertThat(mountPoint1.getSourceVolume()).isEqualTo(bindMount1.getName());

        Volume volume2 = request.getVolumes().get(1);
        assertThat(volume2.getName()).isEqualTo(bindMount2.getName());
        assertThat(volume2.getHost().getSourcePath()).isEqualTo(bindMount2.getSourcePath());

        MountPoint mountPoint2 = containerDefinition.getMountPoints().get(1);
        assertThat(mountPoint2.getContainerPath()).isEqualTo(bindMount2.getContainerPath());
        assertThat(mountPoint2.getSourceVolume()).isEqualTo(bindMount2.getName());
    }

    @Nested
    class Linux {

        @BeforeEach
        void setUp() {
            when(elasticAgentProfileProperties.platform()).thenReturn(Platform.LINUX);
        }

        @Test
        void shouldMountEFSWhenSpecifiedInPluginSettings() {
            when(pluginSettings.efsDnsOrIP()).thenReturn("efs-volume-dns");

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinition);

            assertThat(request.getContainerDefinitions()).hasSize(1);
            assertThat(request.getContainerDefinitions()).contains(containerDefinition);

            assertThat(request.getVolumes()).hasSize(1);
            assertThat(request.getVolumes()).contains(
                    new Volume()
                            .withName("efs")
                            .withHost(
                                    new HostVolumeProperties().withSourcePath(pluginSettings.efsMountLocation())
                            )
            );

            verify(containerDefinition, times(1)).withMountPoints(
                    new MountPoint()
                            .withSourceVolume("efs")
                            .withContainerPath(pluginSettings.efsMountLocation())
            );
        }

        @Test
        void shouldNotMountEFSWhenSpecifiedInPluginSettings() {
            when(pluginSettings.efsDnsOrIP()).thenReturn(null);

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinition);

            assertThat(request.getContainerDefinitions()).hasSize(1);
            assertThat(request.getContainerDefinitions()).contains(containerDefinition);

            assertThat(request.getVolumes()).hasSize(0);

            verify(containerDefinition, never()).withMountPoints(anyCollection());
        }

        @Test
        void shouldBuildRegisterTaskDefinitionRequestWithDockerSocketMountWhenMountDockerSocketIsSetToTrue() {
            when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(true);

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinition);

            assertThat(request.getContainerDefinitions()).hasSize(1);
            assertThat(request.getContainerDefinitions()).contains(containerDefinition);

            assertThat(request.getVolumes()).hasSize(1);
            assertThat(request.getVolumes()).contains(
                    new Volume()
                            .withName("DockerSocket")
                            .withHost(
                                    new HostVolumeProperties().withSourcePath("/var/run/docker.sock")
                            )
            );

            verify(containerDefinition, times(1)).withMountPoints(
                    new MountPoint()
                            .withSourceVolume("DockerSocket")
                            .withContainerPath("/var/run/docker.sock")
            );
        }

        @Test
        void shouldNotMountDockerSocketWhenMountDockerSocketIsSetToFalse() {
            when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(false);

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinition);

            assertThat(request.getContainerDefinitions()).hasSize(1);
            assertThat(request.getContainerDefinitions()).contains(containerDefinition);

            assertThat(request.getVolumes()).hasSize(0);
            verify(containerDefinition, never()).withMountPoints(anyCollection());
        }


        @Test
        void shouldAddEFSAndMountDockerSocketMountWhenBothProvided() {
            when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(true);
            when(pluginSettings.efsDnsOrIP()).thenReturn("efs-volume-dns");

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinition);

            assertThat(request.getContainerDefinitions()).hasSize(1);
            assertThat(request.getContainerDefinitions()).contains(containerDefinition);

            assertThat(request.getVolumes()).hasSize(2);
            assertThat(request.getVolumes()).contains(
                    new Volume()
                            .withName("DockerSocket")
                            .withHost(
                                    new HostVolumeProperties().withSourcePath("/var/run/docker.sock")
                            ),
                    new Volume()
                            .withName("efs")
                            .withHost(
                                    new HostVolumeProperties().withSourcePath(pluginSettings.efsMountLocation())
                            )
            );

            verify(containerDefinition, times(1)).withMountPoints(
                    new MountPoint()
                            .withSourceVolume("DockerSocket")
                            .withContainerPath("/var/run/docker.sock")
            );

            verify(containerDefinition, times(1)).withMountPoints(
                    new MountPoint()
                            .withSourceVolume("efs")
                            .withContainerPath(pluginSettings.efsMountLocation())
            );
        }
    }

    @Nested
    class Windows {
        @BeforeEach
        void setUp() {
            when(elasticAgentProfileProperties.platform()).thenReturn(Platform.WINDOWS);
        }


        @Test
        void shouldNotMountEFSEvenIfItIsSpecified() {
            when(pluginSettings.efsDnsOrIP()).thenReturn("efs");

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinition);

            assertThat(request.getContainerDefinitions()).hasSize(1);
            assertThat(request.getContainerDefinitions()).contains(containerDefinition);

            assertThat(request.getVolumes()).hasSize(0);

            verify(containerDefinition, never()).withMountPoints(anyCollection());
        }

        @Test
        void shouldNotMountDockerSocketEvenItIsSetToTrue() {
            when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(true);

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinition);

            assertThat(request.getContainerDefinitions()).hasSize(1);
            assertThat(request.getContainerDefinitions()).contains(containerDefinition);

            assertThat(request.getVolumes()).hasSize(0);

            verify(containerDefinition, never()).withMountPoints(anyCollection());
        }
    }
}
