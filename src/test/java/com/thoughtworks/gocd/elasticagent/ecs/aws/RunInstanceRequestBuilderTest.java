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

import com.amazonaws.services.ec2.model.*;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

        final TagSpecification tagSpecification = new TagSpecification()
                .withTags(new Tag("Foo", "Bar"));

        final IamInstanceProfileSpecification iamInstanceProfileSpecification = mock(IamInstanceProfileSpecification.class);

        when(ec2Config.getTagSpecification()).thenReturn(tagSpecification);
        when(ec2Config.getIamInstanceProfile()).thenReturn(iamInstanceProfileSpecification);
        when(ec2Config.getSSHKeyName()).thenReturn("Shared_SSH_Key");
        when(ec2Config.getAmi()).thenReturn("ami-123abcdi");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-12345", "sg-abcde"));

        subnet = new Subnet().withSubnetId("s-s23cdf");

        builder = new RunInstanceRequestBuilder();
    }

    @Test
    void shouldBuildRunInstanceRequestFromEC2Config() {
        final RunInstancesRequest runInstancesRequest = builder
                .withSubnet(subnet)
                .withEC2Config(ec2Config)
                .instanceToCreate(2)
                .withServerId("gocd-server-id")
                .build();

        assertThat(runInstancesRequest.getMinCount()).isEqualTo(2);
        assertThat(runInstancesRequest.getMaxCount()).isEqualTo(2);

        assertThat(runInstancesRequest.getImageId()).isEqualTo(ec2Config.getAmi());
        assertThat(runInstancesRequest.getInstanceType()).isEqualTo(ec2Config.getInstanceType());
        assertThat(runInstancesRequest.getKeyName()).isEqualTo(ec2Config.getSSHKeyName());
        assertThat(runInstancesRequest.getIamInstanceProfile()).isEqualTo(ec2Config.getIamInstanceProfile());
        assertThat(runInstancesRequest.getSecurityGroupIds()).isEqualTo(ec2Config.getSecurityGroups());
        assertThat(runInstancesRequest.getSubnetId()).isEqualTo("s-s23cdf");

        assertThat(runInstancesRequest.getTagSpecifications()).hasSize(1);
        assertThat(runInstancesRequest.getTagSpecifications().get(0).getTags())
                .hasSize(2)
                .contains(new Tag("Foo", "Bar"), new Tag(LABEL_SERVER_ID,"gocd-server-id"));
    }

    @Test
    void shouldBuildRunInstanceRequestWithUserdata() {
        when(ec2Config.getUserdata()).thenReturn("dummy_userdata_script");

        final RunInstancesRequest runInstancesRequest = builder
                .withSubnet(subnet)
                .withEC2Config(ec2Config)
                .build();

        assertThat(runInstancesRequest.getUserData()).isEqualTo("dummy_userdata_script");
    }

    @Test
    void shouldBuildRunInstanceRequestWithOperatingSystemDeviceMappingForLinux() {
        when(ec2Config.getOperatingSystemVolumeType()).thenReturn("gp2");
        when(ec2Config.getOperationSystemVolumeSize()).thenReturn("100");
        when(ec2Config.getOperationSystemVolumeProvisionedIOPS()).thenReturn(800);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RunInstancesRequest runInstancesRequest = builder
                .withSubnet(subnet)
                .withEC2Config(ec2Config)
                .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.getBlockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.get(0).getDeviceName()).isEqualTo("/dev/xvda");
        assertThat(deviceMappings.get(0).getEbs().getDeleteOnTermination()).isTrue();
        assertThat(deviceMappings.get(0).getEbs().getVolumeType()).isEqualTo("gp2");
        assertThat(deviceMappings.get(0).getEbs().getVolumeSize()).isEqualTo(100);
        assertThat(deviceMappings.get(0).getEbs().getIops()).isNull();
    }

    @Test
    void shouldBuildRunInstanceRequestWithOperatingSystemDeviceMappingWithProvisionedIopsOnlyWhenVolumeTypeIsIO1ForLinux() {
        when(ec2Config.getOperatingSystemVolumeType()).thenReturn("io1");
        when(ec2Config.getOperationSystemVolumeSize()).thenReturn("100");
        when(ec2Config.getOperationSystemVolumeProvisionedIOPS()).thenReturn(700);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RunInstancesRequest runInstancesRequest = builder
            .withSubnet(subnet)
            .withEC2Config(ec2Config)
            .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.getBlockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.get(0).getDeviceName()).isEqualTo("/dev/xvda");
        assertThat(deviceMappings.get(0).getEbs().getDeleteOnTermination()).isTrue();
        assertThat(deviceMappings.get(0).getEbs().getVolumeType()).isEqualTo("io1");
        assertThat(deviceMappings.get(0).getEbs().getVolumeSize()).isEqualTo(100);
        assertThat(deviceMappings.get(0).getEbs().getIops()).isEqualTo(700);
    }

    @Test
    void shouldBuildRunInstanceRequestWithDockerVolumeDeviceMappingForLinux() {
        when(ec2Config.getDockerVolumeType()).thenReturn("gp2");
        when(ec2Config.getDockerVolumeSize()).thenReturn("50");
        when(ec2Config.getDockerVolumeProvisionedIOPS()).thenReturn(1400);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RunInstancesRequest runInstancesRequest = builder
                .withSubnet(subnet)
                .withEC2Config(ec2Config)
                .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.getBlockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.get(0).getDeviceName()).isEqualTo("/dev/xvdcz");
        assertThat(deviceMappings.get(0).getEbs().getDeleteOnTermination()).isTrue();
        assertThat(deviceMappings.get(0).getEbs().getVolumeType()).isEqualTo("gp2");
        assertThat(deviceMappings.get(0).getEbs().getVolumeSize()).isEqualTo(50);
        assertThat(deviceMappings.get(0).getEbs().getIops()).isNull();
    }

    @Test
    void shouldBuildRunInstanceRequestWithDockerVolumeDeviceMappingWithIopsOnlyWhenVolumeTypeIO1ForLinux() {
        when(ec2Config.getDockerVolumeType()).thenReturn("io1");
        when(ec2Config.getDockerVolumeSize()).thenReturn("50");
        when(ec2Config.getDockerVolumeProvisionedIOPS()).thenReturn(1100);
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        final RunInstancesRequest runInstancesRequest = builder
            .withSubnet(subnet)
            .withEC2Config(ec2Config)
            .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.getBlockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.get(0).getDeviceName()).isEqualTo("/dev/xvdcz");
        assertThat(deviceMappings.get(0).getEbs().getDeleteOnTermination()).isTrue();
        assertThat(deviceMappings.get(0).getEbs().getVolumeType()).isEqualTo("io1");
        assertThat(deviceMappings.get(0).getEbs().getVolumeSize()).isEqualTo(50);
        assertThat(deviceMappings.get(0).getEbs().getIops()).isEqualTo(1100);
    }

    @Test
    void shouldNotBlockDeviceMappingForWindows() {
        when(ec2Config.getOperatingSystemVolumeType()).thenReturn("gp2");
        when(ec2Config.getOperationSystemVolumeSize()).thenReturn("100");
        when(ec2Config.getPlatform()).thenReturn(Platform.WINDOWS);

        final RunInstancesRequest runInstancesRequest = builder
                .withSubnet(subnet)
                .withEC2Config(ec2Config)
                .build();

        final List<BlockDeviceMapping> deviceMappings = runInstancesRequest.getBlockDeviceMappings();

        assertThat(deviceMappings).hasSize(1);
        assertThat(deviceMappings.get(0).getDeviceName()).isEqualTo("/dev/sda1");
        assertThat(deviceMappings.get(0).getEbs().getDeleteOnTermination()).isTrue();
        assertThat(deviceMappings.get(0).getEbs().getVolumeType()).isEqualTo("gp2");
        assertThat(deviceMappings.get(0).getEbs().getVolumeSize()).isEqualTo(100);
        assertThat(deviceMappings.get(0).getEbs().getIops()).isNull();
    }

    @Test
    void shouldHaveServerIdTag() {
        when(ec2Config.getTagSpecification()).thenReturn(new TagSpecification());

        final RunInstancesRequest runInstancesRequest = builder
                .withSubnet(subnet)
                .withEC2Config(ec2Config)
                .instanceToCreate(2)
                .withServerId("gocd-server-id")
                .build();

        assertThat(runInstancesRequest.getTagSpecifications().get(0).getTags())
                .contains(new Tag(LABEL_SERVER_ID, "gocd-server-id"));
    }
}
