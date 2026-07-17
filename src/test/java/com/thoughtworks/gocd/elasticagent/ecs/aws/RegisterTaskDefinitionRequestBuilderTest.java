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

import com.thoughtworks.gocd.elasticagent.ecs.domain.BindMount;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import software.amazon.awssdk.services.ecs.model.*;

import java.util.List;

import static java.util.Arrays.asList;
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
    private ContainerDefinition.Builder containerDefinitionBuilder;
    private RegisterTaskDefinitionRequestBuilder builder;

    @BeforeEach
    void setUp() {
        openMocks(this);
        builder = new RegisterTaskDefinitionRequestBuilder();

    }

    @Test
    void shouldBuildRegisterTaskDefinitionRequest() {
        final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder, "some_task");

        assertThat(request.containerDefinitions()).hasSize(1);
        assertThat(request.containerDefinitions()).contains(containerDefinitionBuilder.build());

        assertThat(request.volumes()).hasSize(0);
        verify(containerDefinitionBuilder, times(0)).mountPoints(anyCollection());
    }

    @Test
    void shouldRegisterTaskDefinitionRequestWithTaskRoleArn() {
        when(elasticAgentProfileProperties.getTaskRoleArn()).thenReturn("task-role-arn");

        final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder, "some_task");

        assertThat(request.taskRoleArn()).isEqualTo("task-role-arn");
    }

    @Test
    void ifBindMountConfigured_shouldRegisterTaskDefinitionRequestWithVolumeAndContainerDefinitionWithMountPoint() {
        BindMount bindMount = new BindMount("data", "sourcePath", "containerPath");
        when(elasticAgentProfileProperties.bindMounts()).thenReturn(singletonList(bindMount));

        final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, ContainerDefinition.builder(), "some_task");

        assertThat(request.volumes().size()).isEqualTo(1);

        Volume volume = request.volumes().getFirst();
        assertThat(volume.name()).isEqualTo(bindMount.getName());
        assertThat(volume.host().sourcePath()).isEqualTo(bindMount.getSourcePath());

        ContainerDefinition containerDefinition = request.containerDefinitions().getFirst();
        assertThat(containerDefinition.mountPoints().size()).isEqualTo(1);

        MountPoint mountPoint = containerDefinition.mountPoints().getFirst();
        assertThat(mountPoint.containerPath()).isEqualTo(bindMount.getContainerPath());
        assertThat(mountPoint.sourceVolume()).isEqualTo(bindMount.getName());
    }

    @Test
    void ifMultipleBindMountsConfigured_shouldRegisterTaskDefinitionRequestWithVolumesAndContainerDefinitionWithMountPoints() {
        BindMount bindMount1 = new BindMount("data1", "sourcePath1", "containerPath1");
        BindMount bindMount2 = new BindMount("data2", "sourcePath2", "containerPath2");
        when(elasticAgentProfileProperties.bindMounts()).thenReturn(asList(bindMount1, bindMount2));

        final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, ContainerDefinition.builder(), "some_task");

        ContainerDefinition containerDefinition = request.containerDefinitions().getFirst();

        assertThat(request.volumes().size()).isEqualTo(2);
        assertThat(containerDefinition.mountPoints().size()).isEqualTo(2);

        Volume volume1 = request.volumes().getFirst();
        assertThat(volume1.name()).isEqualTo(bindMount1.getName());
        assertThat(volume1.host().sourcePath()).isEqualTo(bindMount1.getSourcePath());

        MountPoint mountPoint1 = containerDefinition.mountPoints().getFirst();
        assertThat(mountPoint1.containerPath()).isEqualTo(bindMount1.getContainerPath());
        assertThat(mountPoint1.sourceVolume()).isEqualTo(bindMount1.getName());

        Volume volume2 = request.volumes().get(1);
        assertThat(volume2.name()).isEqualTo(bindMount2.getName());
        assertThat(volume2.host().sourcePath()).isEqualTo(bindMount2.getSourcePath());

        MountPoint mountPoint2 = containerDefinition.mountPoints().get(1);
        assertThat(mountPoint2.containerPath()).isEqualTo(bindMount2.getContainerPath());
        assertThat(mountPoint2.sourceVolume()).isEqualTo(bindMount2.getName());
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

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder, "some_task");

            assertThat(request.containerDefinitions()).hasSize(1);
            assertThat(request.containerDefinitions()).contains(containerDefinitionBuilder.build());

            assertThat(request.volumes()).hasSize(1);
            assertThat(request.volumes()).contains(
                    Volume.builder()
                            .name("efs")
                            .host(HostVolumeProperties.builder().sourcePath(pluginSettings.efsMountLocation()).build())
                            .build()
            );

            verify(containerDefinitionBuilder, times(1)).mountPoints(List.of(
                    MountPoint.builder()
                            .sourceVolume("efs")
                            .containerPath(pluginSettings.efsMountLocation())
                            .build()
            ));
        }

        @Test
        void shouldNotMountEFSWhenSpecifiedInPluginSettings() {
            when(pluginSettings.efsDnsOrIP()).thenReturn(null);

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder, "some_task");

            assertThat(request.containerDefinitions()).hasSize(1);
            assertThat(request.containerDefinitions()).contains(containerDefinitionBuilder.build());

            assertThat(request.volumes()).hasSize(0);

            verify(containerDefinitionBuilder, never()).mountPoints(anyCollection());
        }

        @Test
        void shouldBuildRegisterTaskDefinitionRequestWithDockerSocketMountWhenMountDockerSocketIsSetToTrue() {
            when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(true);

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder, "some_task");

            assertThat(request.containerDefinitions()).hasSize(1);
            assertThat(request.containerDefinitions()).contains(containerDefinitionBuilder.build());

            assertThat(request.volumes()).hasSize(1);
            assertThat(request.volumes()).contains(
                    Volume.builder()
                            .name("DockerSocket")
                            .host(HostVolumeProperties.builder().sourcePath("/var/run/docker.sock").build())
                            .build()
            );

            verify(containerDefinitionBuilder, times(1)).mountPoints(List.of(
                    MountPoint.builder()
                            .sourceVolume("DockerSocket")
                            .containerPath("/var/run/docker.sock")
                            .build()
            ));
        }

        @Test
        void shouldNotMountDockerSocketWhenMountDockerSocketIsSetToFalse() {
            when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(false);

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder, "some_task");

            assertThat(request.containerDefinitions()).hasSize(1);
            assertThat(request.containerDefinitions()).contains(containerDefinitionBuilder.build());

            assertThat(request.volumes()).hasSize(0);
            verify(containerDefinitionBuilder, never()).mountPoints(anyCollection());
        }


        @Test
        void shouldAddEFSAndMountDockerSocketMountWhenBothProvided() {
            when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(true);
            when(pluginSettings.efsDnsOrIP()).thenReturn("efs-volume-dns");

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder, "some_task");

            assertThat(request.containerDefinitions()).hasSize(1);
            assertThat(request.containerDefinitions()).contains(containerDefinitionBuilder.build());

            assertThat(request.volumes()).hasSize(2);
            assertThat(request.volumes()).contains(
                    Volume.builder()
                            .name("DockerSocket")
                            .host(
                                    HostVolumeProperties.builder().sourcePath("/var/run/docker.sock").build()
                            )
                            .build(),
                    Volume.builder()
                            .name("efs")
                            .host(
                                    HostVolumeProperties.builder().sourcePath(pluginSettings.efsMountLocation()).build()
                            )
                            .build()
            );

            // the builder collects all mount points and sets them in one call (v2 builders replace, not append)
            verify(containerDefinitionBuilder, times(1)).mountPoints(List.of(
                    MountPoint.builder()
                            .sourceVolume("efs")
                            .containerPath(pluginSettings.efsMountLocation())
                            .build(),
                    MountPoint.builder()
                            .sourceVolume("DockerSocket")
                            .containerPath("/var/run/docker.sock")
                            .build()
            ));
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

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder, "some_task");

            assertThat(request.containerDefinitions()).hasSize(1);
            assertThat(request.containerDefinitions()).contains(containerDefinitionBuilder.build());

            assertThat(request.volumes()).hasSize(0);

            verify(containerDefinitionBuilder, never()).mountPoints(anyCollection());
        }

        @Test
        void shouldNotMountDockerSocketEvenItIsSetToTrue() {
            when(elasticAgentProfileProperties.isMountDockerSocket()).thenReturn(true);

            final RegisterTaskDefinitionRequest request = builder.build(pluginSettings, elasticAgentProfileProperties, containerDefinitionBuilder, "some_task");

            assertThat(request.containerDefinitions()).hasSize(1);
            assertThat(request.containerDefinitions()).contains(containerDefinitionBuilder.build());

            assertThat(request.volumes()).hasSize(0);

            verify(containerDefinitionBuilder, never()).mountPoints(anyCollection());
        }
    }
}
