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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import org.joda.time.Period;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PluginSettingsTest {

    @Test
    void shouldDeserializeFromJSON() {
        PluginSettings pluginSettings = new PluginSettingsBuilder()
                .addSetting("GoServerUrl", "https://your.server.url")
                .addSetting("ClusterName", "sample_cluster")
                .addSetting("AWSAccessKeyId", "some_access_key")
                .addSetting("AWSSecretAccessKey", "some_secret_key")
                .addSetting("EnvironmentVariables", "TZ=PST")
                .addSetting("ContainerAutoregisterTimeout", "3")
                .addSetting("InstanceType", "c2.small")
                .addSetting("KeyPairName", "your_ssh_key_name")
                .addSetting("IamInstanceProfile", "instance_profile_name")
                .addSetting("AWSRegion", "us-east-x")
                .addSetting("SubnetIds", "s-foobar")
                .addSetting("SecurityGroupIds", "sg-foobar")
                .build();

        assertThat(pluginSettings.getGoServerUrl()).isEqualTo("https://your.server.url");
        assertThat(pluginSettings.getAccessKeyId()).isEqualTo("some_access_key");
        assertThat(pluginSettings.getSecretAccessKey()).isEqualTo("some_secret_key");
        assertThat(pluginSettings.getClusterName()).isEqualTo("sample_cluster");
        assertThat(pluginSettings.getEnvironmentVariables()).contains("TZ=PST");
        assertThat(pluginSettings.getContainerAutoregisterTimeout()).isEqualTo(new Period().withMinutes(3));
        assertThat(pluginSettings.getKeyPairName()).isEqualTo("your_ssh_key_name");
        assertThat(pluginSettings.getRegion()).isEqualTo("us-east-x");
        assertThat(pluginSettings.getSubnetIds()).contains("s-foobar");
        assertThat(pluginSettings.getSecurityGroupIds()).contains("sg-foobar");
    }

    @Test
    void shouldDeserializeLogConfigurationFromJSON() {
        PluginSettings pluginSettings = new PluginSettingsBuilder()
                .addSetting("LogDriver", "awslogs")
                .addSetting("LogOptions", "awslogs-group=foo-group\nawslogs-region=us-east-1")
                .build();

        assertThat(pluginSettings.logConfiguration().getLogDriver()).isEqualTo("awslogs");
        assertThat(pluginSettings.logConfiguration().getOptions())
                .containsEntry("awslogs-group", "foo-group")
                .containsEntry("awslogs-region", "us-east-1");

    }

    @Test
    void shouldReturnNullIfSshKeyPairNameIsEmptyString() {
        PluginSettings pluginSettings = buildPluginSettings("SshKeyPairName", "");

        assertThat(pluginSettings.getKeyPairName()).isNull();
    }

    @Test
    void shouldReturnSpecifiedContainerDataSizeLimit() {
        PluginSettings pluginSettings = buildPluginSettings("MaxContainerDataVolumeSize", "20");
        assertThat(pluginSettings.getMaxContainerDataVolumeSize()).isEqualTo("20");
    }

    private PluginSettings buildPluginSettings(String key, String value) {
        return new PluginSettingsBuilder().addSetting(key, value).build();
    }

}
