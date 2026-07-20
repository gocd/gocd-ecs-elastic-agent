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

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.ec2.model.InstanceType.T2_SMALL;

class SpotInstanceRequestBuilderTest {

    private SpotInstanceRequestBuilder builder;
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
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-12345", "sg-abcde"));

        subnet = Subnet.builder().subnetId("s-s23cdf").build();

        builder = new SpotInstanceRequestBuilder();
    }

    @Test
    void shouldBuildRequestSpotInstancesRequestFromEC2Config() {
        final RequestSpotInstancesRequest request = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        assertThat(request.spotPrice()).isEqualTo(ec2Config.getSpotPrice());
        assertThat(request.validUntil()).isEqualTo(ec2Config.getSpotRequestValidUntil());
    }

    @Test
    void shouldBuildSpotInstancesRequestWithEc2InstanceSpecifications() {
        final RequestSpotInstancesRequest request = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        RequestSpotLaunchSpecification specification = request.launchSpecification();
        assertThat(specification.imageId()).isEqualTo(ec2Config.getAmi());
        assertThat(specification.instanceTypeAsString()).isEqualTo(ec2Config.getInstanceType());
        assertThat(specification.keyName()).isEqualTo(ec2Config.getSSHKeyName());
        assertThat(specification.iamInstanceProfile()).isEqualTo(ec2Config.getIamInstanceProfile());
        assertThat(specification.subnetId()).isEqualTo("s-s23cdf");
    }

    @Test
    void shouldBuildSpotInstancesRequestWithSecurityGroups() {
        final RequestSpotInstancesRequest request = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        RequestSpotLaunchSpecification specification = request.launchSpecification();

        assertThat(specification.securityGroupIds().size()).isEqualTo(2);
        assertThat(specification.securityGroupIds()).contains("sg-12345");
        assertThat(specification.securityGroupIds()).contains("sg-abcde");
    }

    @Test
    void shouldBuildSpotInstancesRequestWithUserdata() {
        when(ec2Config.getUserdata()).thenReturn("dummy_userdata_script");

        final RequestSpotInstancesRequest request = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        assertThat(request.launchSpecification().userData()).isEqualTo("dummy_userdata_script");
    }

    @Test
    void shouldBuildSpotInstanceRequestWithOperatingSystemDeviceMappingForLinux() {
        when(ec2Config.getOperatingSystemVolumeType()).thenReturn("gp2");
        when(ec2Config.getOperationSystemVolumeSize()).thenReturn("100");
        when(ec2Config.getOperationSystemVolumeProvisionedIOPS()).thenReturn(800);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RequestSpotInstancesRequest request = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        final List<BlockDeviceMapping> deviceMappings = request.launchSpecification().blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/xvda");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.GP2);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(100);
        assertThat(deviceMappings.getFirst().ebs().iops()).isNull();
    }

    @Test
    void shouldBuildSpotInstanceRequestWithOperatingSystemDeviceMappingWithProvisionedIopsOnlyWhenVolumeTypeIsIO1ForLinux() {
        when(ec2Config.getOperatingSystemVolumeType()).thenReturn("io1");
        when(ec2Config.getOperationSystemVolumeSize()).thenReturn("100");
        when(ec2Config.getOperationSystemVolumeProvisionedIOPS()).thenReturn(700);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RequestSpotInstancesRequest request = builder
            .subnet(subnet)
            .eC2Config(ec2Config)
            .build();

        final List<BlockDeviceMapping> deviceMappings = request.launchSpecification().blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/xvda");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.IO1);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(100);
        assertThat(deviceMappings.getFirst().ebs().iops()).isEqualTo(700);
    }

    @Test
    void shouldBuildSpotInstanceRequestWithDockerVolumeDeviceMappingForLinux() {
        when(ec2Config.getDockerVolumeType()).thenReturn("gp2");
        when(ec2Config.getDockerVolumeSize()).thenReturn("50");
        when(ec2Config.getDockerVolumeProvisionedIOPS()).thenReturn(1400);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RequestSpotInstancesRequest request = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        final List<BlockDeviceMapping> deviceMappings = request.launchSpecification().blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/xvdcz");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.GP2);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(50);
        assertThat(deviceMappings.getFirst().ebs().iops()).isNull();
    }

    @Test
    void shouldBuildSpotInstanceRequestWithDockerVolumeDeviceMappingWithIopsOnlyWhenVolumeTypeIO1ForLinux() {
        when(ec2Config.getDockerVolumeType()).thenReturn("io1");
        when(ec2Config.getDockerVolumeSize()).thenReturn("50");
        when(ec2Config.getDockerVolumeProvisionedIOPS()).thenReturn(1100);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RequestSpotInstancesRequest request = builder
            .subnet(subnet)
            .eC2Config(ec2Config)
            .build();

        final List<BlockDeviceMapping> deviceMappings = request.launchSpecification().blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/xvdcz");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.IO1);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(50);
        assertThat(deviceMappings.getFirst().ebs().iops()).isEqualTo(1100);
    }

    @Test
    void shouldHaveBlockDeviceMappingForWindows() {
        when(ec2Config.getOperatingSystemVolumeType()).thenReturn("gp2");
        when(ec2Config.getOperationSystemVolumeSize()).thenReturn("100");
        when(ec2Config.getPlatform()).thenReturn(Platform.WINDOWS);

        final RequestSpotInstancesRequest request = builder
                .subnet(subnet)
                .eC2Config(ec2Config)
                .build();

        final List<BlockDeviceMapping> deviceMappings = request.launchSpecification().blockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.getFirst().deviceName()).isEqualTo("/dev/sda1");
        assertThat(deviceMappings.getFirst().ebs().deleteOnTermination()).isTrue();
        assertThat(deviceMappings.getFirst().ebs().volumeType()).isEqualTo(VolumeType.GP2);
        assertThat(deviceMappings.getFirst().ebs().volumeSize()).isEqualTo(100);
        assertThat(deviceMappings.getFirst().ebs().iops()).isNull();
    }
}
