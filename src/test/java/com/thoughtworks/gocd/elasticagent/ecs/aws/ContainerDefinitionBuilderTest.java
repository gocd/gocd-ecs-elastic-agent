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

import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ecs.model.ContainerDefinition;
import software.amazon.awssdk.services.ecs.model.KeyValuePair;
import software.amazon.awssdk.services.ecs.model.LogConfiguration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerDefinitionBuilderTest {

    private PluginSettings pluginSettings;
    private CreateAgentRequest createAgentRequest;
    private ElasticAgentProfileProperties elasticAgentProfileProperties;
    private JobIdentifier jobIdentifier;

    @BeforeEach
    void setUp() {
        pluginSettings = mock(PluginSettings.class);
        createAgentRequest = mock(CreateAgentRequest.class);
        elasticAgentProfileProperties = mock(ElasticAgentProfileProperties.class);
        jobIdentifier = mock(JobIdentifier.class);

        when(createAgentRequest.elasticProfile()).thenReturn(elasticAgentProfileProperties);
        when(createAgentRequest.getJobIdentifier()).thenReturn(jobIdentifier);
        when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
    }

    @Test
    void shouldBuildContainerDefinition() {
        final LogConfiguration logConfiguration = LogConfiguration.builder()
                .logDriver("awslog")
                .options(Collections.singletonMap("group", "foo"))
                .build();

        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(elasticAgentProfileProperties.getMaxMemory()).thenReturn(2048);
        when(elasticAgentProfileProperties.getReservedMemory()).thenReturn(1024);
        when(elasticAgentProfileProperties.getCommand()).thenReturn(Arrays.asList("ping x.x.x.x", "-c", "160"));
        when(pluginSettings.logConfiguration()).thenReturn(logConfiguration);

        ContainerDefinitionBuilder builder = new ContainerDefinitionBuilder(createAgentRequest)
                .name("foo")
                .pluginSettings(pluginSettings);

        final ContainerDefinition containerDefinition = builder.build().build();

        assertThat(containerDefinition.name()).isEqualTo("foo");
        assertThat(containerDefinition.image()).isEqualTo("alpine:latest");
        assertThat(containerDefinition.memory()).isEqualTo(2048);
        assertThat(containerDefinition.memoryReservation()).isEqualTo(1024);
        assertThat(containerDefinition.command()).contains("ping x.x.x.x", "-c", "160");
        assertThat(containerDefinition.logConfiguration()).isEqualTo(logConfiguration);
    }

    @Test
    void shouldBuildContainerDefinitionWithEnvironments() {
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(elasticAgentProfileProperties.getEnvironment()).thenReturn(List.of("TZ=PST"));

        when(pluginSettings.getEnvironmentVariables()).thenReturn(List.of("JAVA_HOME=/var/lib/java"));
        when(pluginSettings.getGoServerUrl()).thenReturn("https://foo.server/go");

        when(createAgentRequest.autoRegisterKey()).thenReturn("some-auto-register-key");
        when(createAgentRequest.environment()).thenReturn("some-environment");
        when(createAgentRequest.autoRegisterPropertiesAsEnvironmentVars("foo")).thenCallRealMethod();

        ContainerDefinitionBuilder builder = new ContainerDefinitionBuilder(createAgentRequest)
                .name("foo")
                .pluginSettings(pluginSettings);

        final ContainerDefinition containerDefinition = builder.build().build();

        assertThat(containerDefinition.environment()).contains(
                KeyValuePair.builder().name("TZ").value("PST").build(),
                KeyValuePair.builder().name("JAVA_HOME").value("/var/lib/java").build(),
                KeyValuePair.builder().name("GO_EA_SERVER_URL").value("https://foo.server/go").build(),
                KeyValuePair.builder().name("GO_EA_AUTO_REGISTER_KEY").value("some-auto-register-key").build(),
                KeyValuePair.builder().name("GO_EA_AUTO_REGISTER_ENVIRONMENT").value("some-environment").build(),
                KeyValuePair.builder().name("GO_EA_AUTO_REGISTER_ELASTIC_AGENT_ID").value("foo").build(),
                KeyValuePair.builder().name("GO_EA_AUTO_REGISTER_ELASTIC_PLUGIN_ID").value(PLUGIN_ID).build()
        );
    }

    @Test
    void shouldBuildContainerDefinitionWithLabels() {
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(createAgentRequest.environment()).thenReturn("some-environment");
        when(elasticAgentProfileProperties.toJson()).thenReturn("elastic-profile-in-json");
        when(jobIdentifier.toJson()).thenReturn("job-identifier-in-json");


        ContainerDefinitionBuilder builder = new ContainerDefinitionBuilder(createAgentRequest)
                .name("foo")
                .pluginSettings(pluginSettings)
                .serverId("gocd-server-id");

        final ContainerDefinition containerDefinition = builder.build().build();

        assertThat(containerDefinition.dockerLabels())
                .containsEntry(CREATED_BY_LABEL_KEY, PLUGIN_ID)
                .containsEntry(ENVIRONMENT_LABEL_KEY, "some-environment")
                .containsEntry(CONFIGURATION_LABEL_KEY, "elastic-profile-in-json")
                .containsEntry(LABEL_JOB_IDENTIFIER, "job-identifier-in-json")
                .containsEntry(LABEL_SERVER_ID, "gocd-server-id");
    }

    @Test
    void shouldBuildContainerDefinitionPrivilegedModeOnForLinux() {
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(elasticAgentProfileProperties.isPrivileged()).thenReturn(true);
        when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);


        ContainerDefinitionBuilder builder = new ContainerDefinitionBuilder(createAgentRequest)
                .name("foo")
                .pluginSettings(pluginSettings)
                .serverId("gocd-server-id");

        final ContainerDefinition containerDefinition = builder.build().build();

        assertThat(containerDefinition.privileged()).isTrue();
    }

    @Test
    void shouldBuildContainerDefinitionPrivilegedModeOffForLinux() {
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(elasticAgentProfileProperties.isPrivileged()).thenReturn(false);
        when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);


        ContainerDefinitionBuilder builder = new ContainerDefinitionBuilder(createAgentRequest)
                .name("foo")
                .pluginSettings(pluginSettings)
                .serverId("gocd-server-id");

        final ContainerDefinition containerDefinition = builder.build().build();

        assertThat(containerDefinition.privileged()).isFalse();
    }

    @Test
    void shouldBuildContainerDefinitionWithPrivilegedModeOffEvenIfItIsSetToTrueForWindows() {
        when(elasticAgentProfileProperties.getImage()).thenReturn("windowsservercore");
        when(elasticAgentProfileProperties.isPrivileged()).thenReturn(true);
        when(elasticAgentProfileProperties.platform()).thenReturn(WINDOWS);


        ContainerDefinitionBuilder builder = new ContainerDefinitionBuilder(createAgentRequest)
                .name("foo")
                .pluginSettings(pluginSettings)
                .serverId("gocd-server-id");

        final ContainerDefinition containerDefinition = builder.build().build();

        assertThat(containerDefinition.privileged()).isFalse();
    }

    @Test
    void shouldNotMakeMemoryReservationForWindows() {
        when(elasticAgentProfileProperties.getImage()).thenReturn("alpine");
        when(elasticAgentProfileProperties.getMaxMemory()).thenReturn(2048);
        when(elasticAgentProfileProperties.getReservedMemory()).thenReturn(1024);
        when(elasticAgentProfileProperties.platform()).thenReturn(Platform.WINDOWS);

        ContainerDefinitionBuilder builder = new ContainerDefinitionBuilder(createAgentRequest)
                .name("foo")
                .pluginSettings(pluginSettings)
                .serverId("gocd-server-id");

        final ContainerDefinition containerDefinition = builder.build().build();

        assertThat(containerDefinition.memory()).isEqualTo(2048);
        assertThat(containerDefinition.memoryReservation()).isNull();
    }
}
