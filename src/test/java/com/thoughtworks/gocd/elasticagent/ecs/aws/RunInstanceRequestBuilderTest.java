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

import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LABEL_SERVER_ID;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RunInstanceRequestBuilderTest {

    private RunInstanceRequestBuilder builder;
    private EC2Config ec2Config;
    private Subnet subnet;

    @BeforeEach
    void setUp() {
        ec2Config = mock(EC2Config.class);

        final TagSpecification.Builder tagSpecification = TagSpecification.builder()
                .tags(Tag.builder().key("Foo").value("Bar").build());

        final IamInstanceProfileSpecification iamInstanceProfileSpecification = mock(IamInstanceProfileSpecification.class);

        when(ec2Config.getTagSpecification()).thenReturn(tagSpecification);
        when(ec2Config.getIamInstanceProfile()).thenReturn(iamInstanceProfileSpecification);
        when(ec2Config.getSSHKeyName()).thenReturn("Shared_SSH_Key");
        when(ec2Config.getAmi()).thenReturn("ami-123abcdi");
        when(ec2Config.getInstanceType()).thenReturn(InstanceType.T2_SMALL.toString());
        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-12345", "sg-abcde"));

        subnet = Subnet.builder().subnetId("s-s23cdf").build();

        builder = new RunInstanceRequestBuilder();
    }

    @Test
    void shouldBuildRunInstanceRequestFromEC2Config() {
        final RunInstancesRequest runInstancesRequest = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .instanceToCreate(2)
                .serverId("gocd-server-id")
                .build();

        assertThat(runInstancesRequest.minCount()).isEqualTo(2);
        assertThat(runInstancesRequest.maxCount()).isEqualTo(2);

        assertThat(runInstancesRequest.imageId()).isEqualTo(ec2Config.getAmi());
        assertThat(runInstancesRequest.instanceTypeAsString()).isEqualTo(ec2Config.getInstanceType());
        assertThat(runInstancesRequest.keyName()).isEqualTo(ec2Config.getSSHKeyName());
        assertThat(runInstancesRequest.iamInstanceProfile()).isEqualTo(ec2Config.getIamInstanceProfile());
        assertThat(runInstancesRequest.securityGroupIds()).isEqualTo(ec2Config.getSecurityGroups());
        assertThat(runInstancesRequest.subnetId()).isEqualTo("s-s23cdf");

        assertThat(runInstancesRequest.tagSpecifications()).hasSize(1);
        assertThat(runInstancesRequest.tagSpecifications().getFirst().tags())
                .hasSize(2)
                .contains(Tag.builder().key("Foo").value("Bar").build(), Tag.builder().key(LABEL_SERVER_ID).value("gocd-server-id").build());
    }

    @Test
    void shouldBuildRunInstanceRequestWithUserdata() {
        when(ec2Config.getUserdata()).thenReturn("dummy_userdata_script");

        final RunInstancesRequest runInstancesRequest = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        assertThat(runInstancesRequest.userData()).isEqualTo("dummy_userdata_script");
    }

    @Test
    void shouldBuildRunInstanceRequestWithOperatingSystemDeviceMappingForLinux() {
        when(ec2Config.getOperatingSystemVolumeType()).thenReturn("gp2");
        when(ec2Config.getOperationSystemVolumeSize()).thenReturn("100");
        when(ec2Config.getOperationSystemVolumeProvisionedIOPS()).thenReturn(800);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RunInstancesRequest runInstancesRequest = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/xvda");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.GP2);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(100);
        assertThat(deviceMappings.getFirst().ebs().iops()).isNull();
    }

    @Test
    void shouldBuildRunInstanceRequestWithOperatingSystemDeviceMappingWithProvisionedIopsOnlyWhenVolumeTypeIsIO1ForLinux() {
        when(ec2Config.getOperatingSystemVolumeType()).thenReturn("io1");
        when(ec2Config.getOperationSystemVolumeSize()).thenReturn("100");
        when(ec2Config.getOperationSystemVolumeProvisionedIOPS()).thenReturn(700);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RunInstancesRequest runInstancesRequest = builder
            .subnet(subnet)
            .eC2Config(ec2Config)
            .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/xvda");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.IO1);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(100);
        assertThat(deviceMappings.getFirst().ebs().iops()).isEqualTo(700);
    }

    @Test
    void shouldBuildRunInstanceRequestWithDockerVolumeDeviceMappingForLinux() {
        when(ec2Config.getDockerVolumeType()).thenReturn("gp2");
        when(ec2Config.getDockerVolumeSize()).thenReturn("50");
        when(ec2Config.getDockerVolumeProvisionedIOPS()).thenReturn(1400);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RunInstancesRequest runInstancesRequest = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/xvdcz");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.GP2);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(50);
        assertThat(deviceMappings.getFirst().ebs().iops()).isNull();
    }

    @Test
    void shouldBuildRunInstanceRequestWithDockerVolumeDeviceMappingWithIopsOnlyWhenVolumeTypeIO1ForLinux() {
        when(ec2Config.getDockerVolumeType()).thenReturn("io1");
        when(ec2Config.getDockerVolumeSize()).thenReturn("50");
        when(ec2Config.getDockerVolumeProvisionedIOPS()).thenReturn(1100);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RunInstancesRequest runInstancesRequest = builder
            .subnet(subnet)
            .eC2Config(ec2Config)
            .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/xvdcz");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.IO1);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(50);
        assertThat(deviceMappings.getFirst().ebs().iops()).isEqualTo(1100);
    }

    @Test
    void shouldNotBlockDeviceMappingForWindows() {
        when(ec2Config.getOperatingSystemVolumeType()).thenReturn("gp2");
        when(ec2Config.getOperationSystemVolumeSize()).thenReturn("100");
        when(ec2Config.getPlatform()).thenReturn(Platform.WINDOWS);

        final RunInstancesRequest runInstancesRequest = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/sda1");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.GP2);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(100);
        assertThat(deviceMappings.getFirst().ebs().iops()).isNull();
    }

    @Test
    void shouldHaveServerIdTag() {
        when(ec2Config.getTagSpecification()).thenReturn(TagSpecification.builder());

        final RunInstancesRequest runInstancesRequest = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .instanceToCreate(2)
                .serverId("gocd-server-id")
                .build();

        assertThat(runInstancesRequest.tagSpecifications().getFirst().tags())
                .contains(Tag.builder().key(LABEL_SERVER_ID).value("gocd-server-id").build());
    }
}
