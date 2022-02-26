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

import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.Tag;
import com.thoughtworks.gocd.elasticagent.ecs.domain.*;
import com.thoughtworks.gocd.extensions.FileSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.EC2Config.Builder.PLATFORM;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.Userdata.decodeBase64;
import static org.assertj.core.api.Assertions.assertThat;

class EC2ConfigTest {

    private PluginSettingsBuilder pluginSettingsBuilder;
    private ElasticProfileBuilder profileBuilder;

    @BeforeEach
    void setUp() {
        pluginSettingsBuilder = new PluginSettingsBuilder();
        profileBuilder = new ElasticProfileBuilder();
    }

    @Test
    void shouldUseSecurityGroupIdsFromElasticProfileWhenSpecified() {
        final PluginSettings pluginSettings = pluginSettingsBuilder
                .addSetting("SecurityGroupIds", "sg-foobar")
                .build();

        final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                .addSetting("SecurityGroupIds", "sg-bazboom,sg-abcdef")
                .build();

        final EC2Config ec2Config = new EC2Config.Builder()
                .withSettings(pluginSettings)
                .withProfile(elasticAgentProfileProperties)
                .build();

        assertThat(ec2Config.getSecurityGroups())
                .hasSize(2)
                .contains("sg-bazboom", "sg-abcdef");
    }

    @Test
    void shouldUsePickSecurityGroupIdsFromPluginSettingsWhenNotSpecifiedInProfile() {
        final PluginSettings pluginSettings = pluginSettingsBuilder
                .addSetting("SecurityGroupIds", "sg-foobar")
                .build();

        final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

        final EC2Config ec2Config = new EC2Config.Builder()
                .withSettings(pluginSettings)
                .withProfile(elasticAgentProfileProperties)
                .build();

        assertThat(ec2Config.getSecurityGroups())
                .hasSize(1)
                .contains("sg-foobar");
    }

    @Test
    void shouldUseIAMProfileFromElasticProfileWhenSpecified() {
        final PluginSettings pluginSettings = pluginSettingsBuilder
                .addSetting("EC2IAMInstanceProfile", "iam-1234abcd")
                .build();

        final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                .addSetting("IAMInstanceProfile", "iam-foobar")
                .build();

        final EC2Config ec2Config = new EC2Config.Builder()
                .withSettings(pluginSettings)
                .withProfile(elasticAgentProfileProperties)
                .build();

        assertThat(ec2Config.getIamInstanceProfile())
                .isEqualTo(new IamInstanceProfileSpecification().withName("iam-foobar"));
    }

    @Test
    void shouldUsePickIAMFromPluginSettingsWhenNotSpecifiedInProfile() {
        final PluginSettings pluginSettings = pluginSettingsBuilder
                .addSetting("IamInstanceProfile", "iam-foobar")
                .build();

        final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

        final EC2Config ec2Config = new EC2Config.Builder()
                .withSettings(pluginSettings)
                .withProfile(elasticAgentProfileProperties)
                .build();

        assertThat(ec2Config.getIamInstanceProfile())
                .isEqualTo(new IamInstanceProfileSpecification().withName("iam-foobar"));
    }

    @Test
    void shouldUseKeyPairFromElasticProfileWhenSpecified() {
        final PluginSettings pluginSettings = pluginSettingsBuilder
                .addSetting("KeyPairName", "ecs-shared")
                .build();

        final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                .build();

        final EC2Config ec2Config = new EC2Config.Builder()
                .withSettings(pluginSettings)
                .withProfile(elasticAgentProfileProperties)
                .build();

        assertThat(ec2Config.getSSHKeyName())
                .isEqualTo("ecs-shared");
    }

    @Test
    void shouldUseSubnetFromElasticProfileWhenSpecified() {
        final PluginSettings pluginSettings = pluginSettingsBuilder
                .addSetting("SubnetIds", "sub-foobar")
                .build();

        final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                .addSetting("SubnetIds", "sub-12345,sub-abcde")
                .build();

        final EC2Config ec2Config = new EC2Config.Builder()
                .withSettings(pluginSettings)
                .withProfile(elasticAgentProfileProperties)
                .build();

        assertThat(ec2Config.getSubnetIds())
                .hasSize(2)
                .contains("sub-12345", "sub-abcde");
    }

    @Test
    void shouldUsePickSubnetIDsFromPluginSettingsWhenNotSpecifiedInProfile() {
        final PluginSettings pluginSettings = pluginSettingsBuilder
                .addSetting("SubnetIds", "sub-foobar")
                .build();

        final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

        final EC2Config ec2Config = new EC2Config.Builder()
                .withSettings(pluginSettings)
                .withProfile(elasticAgentProfileProperties)
                .build();

        assertThat(ec2Config.getSubnetIds())
                .hasSize(1)
                .contains("sub-foobar");
    }

    @Nested
    class Linux {
        @BeforeEach
        void setUp() {
            profileBuilder.addSetting("Platform", Platform.LINUX.name());
        }

        @Test
        void shouldUseAMIFromElasticProfileWhenSpecified() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("ami", "ami-foobar")
                    .addSetting("WindowsAmi", "ami-windows")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                    .addSetting("AMI", "ami-1234abcdi")
                    .build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getPlatform()).isEqualTo(Platform.LINUX);
            assertThat(ec2Config.getAmi()).isEqualTo("ami-1234abcdi");
        }

        @Test
        void shouldUsePickAMIFromPluginSettingsWhenNotSpecifiedInProfile() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("LinuxAmi", "ami-foobar")
                    .addSetting("WindowsAmi", "ami-windows")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getPlatform()).isEqualTo(Platform.LINUX);
            assertThat(ec2Config.getAmi()).isEqualTo("ami-foobar");
        }

        @Test
        void shouldUseInstanceTypeFromElasticProfileWhenSpecified() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("EC2InstanceType", "t2.small")
                    .addSetting("WindowsInstanceType", "m5.2xlarge")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                    .addSetting("InstanceType", "t2.medium")
                    .build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getInstanceType()).isEqualTo("t2.medium");
        }

        @Test
        void shouldUsePickInstanceTypeFromPluginSettingsWhenNotSpecifiedInProfile() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("LinuxInstanceType", "t2.small")
                    .addSetting("WindowsInstanceType", "m5.2xlarge")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getInstanceType()).isEqualTo("t2.small");
        }

        @Test
        void shouldUseOperatingSystemVolumeInfo() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("LinuxOSVolumeType", "io1")
                    .addSetting("LinuxOSVolumeSize", "8G")
                    .addSetting("LinuxOSVolumeProvisionedIOPS", "2000")
                    .addSetting("WindowsOSVolumeType", "SSD")
                    .addSetting("WindowsOSVolumeSize", "50G")
                    .addSetting("WindowsOSVolumeProvisionedIOPS", "1000")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getOperatingSystemVolumeType()).isEqualTo("io1");
            assertThat(ec2Config.getOperationSystemVolumeSize()).isEqualTo("8G");
            assertThat(ec2Config.getOperationSystemVolumeProvisionedIOPS()).isEqualTo(2000);
        }

        @Test
        void shouldUseDockerVolumeInfo() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("LinuxDockerVolumeType", "gp2")
                    .addSetting("LinuxDockerVolumeSize", "22G")
                    .addSetting("LinuxDockerVolumeProvisionedIOPS", "450")
                    .addSetting("WindowsOSVolumeType", "SSD")
                    .addSetting("WindowsOSVolumeSize", "50G")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getDockerVolumeType()).isEqualTo("gp2");
            assertThat(ec2Config.getDockerVolumeSize()).isEqualTo("22G");
            assertThat(ec2Config.getDockerVolumeProvisionedIOPS()).isEqualTo(450);
        }

        @Test
        void shouldNotUseVolumeInfoWhenNotSpecified() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("WindowsOSVolumeType", "SSD")
                    .addSetting("WindowsOSVolumeSize", "50G")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getOperatingSystemVolumeType()).isEqualTo(null);
            assertThat(ec2Config.getOperationSystemVolumeSize()).isEqualTo(null);
        }

        @Test
        void shouldCreateTagSpecificationWithPlatform() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getTagSpecification()).isNotNull();
            assertThat(ec2Config.getTagSpecification().getTags())
                    .hasSize(3)
                    .contains(new Tag(PLATFORM, Platform.LINUX.name()));
        }

        @ParameterizedTest
        @FileSource(files = "/userdata/userdata-from-ec2-config.sh")
        void shouldCreateEncodedUserdata(String expectedUserdataScript) {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("ClusterName", "ECS")
                    .addSetting("LinuxUserdataScript", "linux-script")
                    .addSetting("WindowsUserdataScript", "windows-script")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(decodeBase64(ec2Config.getUserdata())).isEqualTo(expectedUserdataScript);
            assertThat(decodeBase64(ec2Config.getUserdata())).doesNotContain("windows-script");
        }

        @Test
        void shouldGetMaxAllowedInstances() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("MaxEC2InstancesAllowed", "5")
                    .addSetting("MaxWindowsInstancesAllowed", "2")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getMaxInstancesAllowed()).isEqualTo(5);
        }

        @Test
        void shouldGetRegisterTimeout() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("LinuxRegisterTimeout", "2")
                    .addSetting("WindowsRegisterTimeout", "10")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getRegisterTimeOut().getMinutes()).isEqualTo(2);
        }

        @Test
        void shouldGetMinInstanceCount() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("MinLinuxInstanceCount", "2")
                    .addSetting("MinWindowsInstanceCount", "10")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getMinInstanceCount()).isEqualTo(2);
        }

        @Test
        void shouldBuildWithSpotInstanceConfigurations() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .build();
            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                    .addSetting("RunAsSpotInstance", "true")
                    .addSetting("SpotPrice", "0.03")
                    .addSetting("SpotRequestExpiresAfter", "5")
                    .build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.runAsSpotInstance()).isTrue();
            assertThat(ec2Config.getSpotPrice()).isEqualTo("0.03");
        }
    }

    @Nested
    class Windows {
        @BeforeEach
        void setUp() {
            profileBuilder.addSetting("Platform", Platform.WINDOWS.name());
        }

        @Test
        void shouldUseAMIFromElasticProfileWhenSpecified() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("ami", "ami-foobar")
                    .addSetting("WindowsAmi", "ami-windows")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                    .addSetting("AMI", "ami-1234abcdi")
                    .build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getPlatform()).isEqualTo(Platform.WINDOWS);
            assertThat(ec2Config.getAmi()).isEqualTo("ami-1234abcdi");
        }

        @Test
        void shouldUsePickAMIFromPluginSettingsWhenNotSpecifiedInProfile() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("ami", "ami-foobar")
                    .addSetting("WindowsAmi", "ami-windows")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getPlatform()).isEqualTo(Platform.WINDOWS);
            assertThat(ec2Config.getAmi()).isEqualTo("ami-windows");
        }

        @Test
        void shouldUseInstanceTypeFromElasticProfileWhenSpecified() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("EC2InstanceType", "t2.small")
                    .addSetting("WindowsInstanceType", "m5.2xlarge")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                    .addSetting("InstanceType", "t2.medium")
                    .build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getInstanceType()).isEqualTo("t2.medium");
        }

        @Test
        void shouldUsePickInstanceTypeFromPluginSettingsWhenNotSpecifiedInProfile() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("EC2InstanceType", "t2.small")
                    .addSetting("WindowsInstanceType", "m5.2xlarge")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getInstanceType()).isEqualTo("m5.2xlarge");
        }

        @Test
        void shouldUseVolumeInfo() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("VolumeType", "gp2")
                    .addSetting("VolumeSize", "22G")
                    .addSetting("WindowsOSVolumeType", "io1")
                    .addSetting("WindowsOSVolumeSize", "50G")
                    .addSetting("WindowsOSVolumeProvisionedIOPS", "1100")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getOperatingSystemVolumeType()).isEqualTo("io1");
            assertThat(ec2Config.getOperationSystemVolumeSize()).isEqualTo("50G");
            assertThat(ec2Config.getOperationSystemVolumeProvisionedIOPS()).isEqualTo(1100);
        }

        @Test
        void shouldNotUseVolumeInfoWhenNotSpecified() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("VolumeType", "gp2")
                    .addSetting("VolumeSize", "22G")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getOperatingSystemVolumeType()).isEqualTo(null);
            assertThat(ec2Config.getOperationSystemVolumeSize()).isEqualTo(null);
        }

        @Test
        void shouldGetMaxAllowedInstances() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("MaxEC2InstancesAllowed", "5")
                    .addSetting("MaxWindowsInstancesAllowed", "2")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getMaxInstancesAllowed()).isEqualTo(2);
        }

        @Test
        void shouldGetRegisterTimeout() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("EC2RegisterTimeout", "2")
                    .addSetting("WindowsRegisterTimeout", "10")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getRegisterTimeOut().getMinutes()).isEqualTo(10);
        }


        @Test
        void shouldGetMinInstanceCount() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("MinEC2InstanceCount", "2")
                    .addSetting("MinWindowsInstanceCount", "10")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getMinInstanceCount()).isEqualTo(10);
        }

        @Test
        void shouldCreateTagSpecificationWithPlatform() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.getTagSpecification()).isNotNull();
            assertThat(ec2Config.getTagSpecification().getTags())
                    .hasSize(3)
                    .contains(new Tag(PLATFORM, Platform.WINDOWS.name()));
        }

        @Test
        void shouldCreateEncodedUserdata() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .addSetting("ClusterName", "ECS")
                    .addSetting("LinuxUserdataScript", "linux-script")
                    .addSetting("WindowsUserdataScript", "windows-script")
                    .build();

            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder.build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            final String expectedUserdataScript = "<powershell>\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_CLUSTER\", \"ECS\", \"Machine\")\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_ENGINE_TASK_CLEANUP_WAIT_DURATION\", \"1m\", \"Machine\")\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_IMAGE_MINIMUM_CLEANUP_AGE\", \"24h\", \"Machine\")\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_INSTANCE_ATTRIBUTES\", \"{`\"server-id`\":`\"`\"}\", \"Machine\")\n" +
                    "Import-Module ECSTools\n" +
                    "Initialize-ECSAgent -Cluster 'ECS' -EnableTaskIAMRole\n" +
                    "windows-script\n" +
                    "</powershell>";

            assertThat(decodeBase64(ec2Config.getUserdata())).isEqualTo(expectedUserdataScript);
            assertThat(decodeBase64(ec2Config.getUserdata())).doesNotContain("linux-script");
        }

        @Test
        void shouldBuildWithSpotInstanceConfigurations() {
            final PluginSettings pluginSettings = pluginSettingsBuilder
                    .build();
            final ElasticAgentProfileProperties elasticAgentProfileProperties = profileBuilder
                    .addSetting("RunAsSpotInstance", "true")
                    .addSetting("SpotPrice", "0.03")
                    .addSetting("SpotRequestExpiresAfter", "5")
                    .build();

            final EC2Config ec2Config = new EC2Config.Builder()
                    .withSettings(pluginSettings)
                    .withProfile(elasticAgentProfileProperties)
                    .build();

            assertThat(ec2Config.runAsSpotInstance()).isTrue();
            assertThat(ec2Config.getSpotPrice()).isEqualTo("0.03");
        }
    }
}
