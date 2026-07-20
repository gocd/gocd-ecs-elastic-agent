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
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import software.amazon.awssdk.services.ec2.model.*;

import java.util.ArrayList;
import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LABEL_SERVER_ID;
import static com.thoughtworks.gocd.elasticagent.ecs.validators.VolumeSettingsValidator.VOLUME_TYPE_IO_1;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class RunInstanceRequestBuilder {
    private static final Logger LOG = Logger.getLoggerFor(RunInstanceRequestBuilder.class);

    private static final String DEFAULT_LINUX_OS_DEVICE_NAME = "/dev/xvda";
    private static final String DEFAULT_WINDOWS_OS_DEVICE_NAME = "/dev/sda1";
    private static final String DEFAULT_LINUX_DOCKER_DEVICE_NAME = "/dev/xvdcz";

    private int instanceToCreate;
    private EC2Config ec2Config;
    private Subnet subnet;
    private String serverId;

    public RunInstancesRequest build() {
        return buildWithBaseConfiguration(subnet)
                .imageId(ec2Config.getAmi())
                .instanceType(ec2Config.getInstanceType())
                .securityGroupIds(ec2Config.getSecurityGroups())
                .iamInstanceProfile(ec2Config.getIamInstanceProfile())
                .build();
    }

    private RunInstancesRequest.Builder buildWithBaseConfiguration(Subnet subnet) {
        final String subnetId = subnet != null ? subnet.subnetId() : null;
        final RunInstancesRequest.Builder request = RunInstancesRequest.builder()
                .minCount(instanceToCreate)
                .maxCount(instanceToCreate)
                .keyName(ec2Config.getSSHKeyName())
                .tagSpecifications(tagSpecificationWithServerId())
                .userData(ec2Config.getUserdata())
                .subnetId(subnetId);

        final List<BlockDeviceMapping> blockDeviceMappings = new ArrayList<>();
        blockOperatingSystemVolume(blockDeviceMappings);
        blockDockerVolume(blockDeviceMappings);
        if (!blockDeviceMappings.isEmpty()) {
            request.blockDeviceMappings(blockDeviceMappings);
        }

        return request;
    }

    private TagSpecification tagSpecificationWithServerId() {
        // v2 builders replace list contents rather than appending, so combine the tags manually
        final TagSpecification tagSpecification = ec2Config.getTagSpecification().build();
        final List<Tag> tags = new ArrayList<>(tagSpecification.tags());
        tags.add(Tag.builder().key(LABEL_SERVER_ID).value(serverId).build());
        return tagSpecification.toBuilder().tags(tags).build();
    }


    private void blockOperatingSystemVolume(List<BlockDeviceMapping> blockDeviceMappings) {
        if (isBlank(ec2Config.getOperatingSystemVolumeType()) || "none".equals(ec2Config.getOperatingSystemVolumeType())) {
            return;
        }

        blockDeviceMappings.add(
                blockDeviceMapping(osDeviceName(), ec2Config.getOperatingSystemVolumeType(), Integer.parseInt(ec2Config.getOperationSystemVolumeSize()), ec2Config.getOperationSystemVolumeProvisionedIOPS())
        );
    }

    private BlockDeviceMapping blockDeviceMapping(String deviceName, String volumeType, int volumeSize, Integer provisionedIOPS) {
        EbsBlockDevice.Builder ebsBlockDevice = EbsBlockDevice.builder()
            .deleteOnTermination(true)
            .volumeType(volumeType)
            .volumeSize(volumeSize);
        return BlockDeviceMapping.builder()
            .ebs(withIops(ebsBlockDevice, volumeType, provisionedIOPS))
            .deviceName(deviceName)
            .build();
    }

    private void blockDockerVolume(List<BlockDeviceMapping> blockDeviceMappings) {
        if (ec2Config.getPlatform() == Platform.WINDOWS) {
            LOG.debug("As windows is using root volumes to store everything extra volume for docker is not needed.");
            return;
        }

        if (isBlank(ec2Config.getDockerVolumeType()) || "none".equals(ec2Config.getDockerVolumeType())) {
            return;
        }

        blockDeviceMappings.add(
                blockDeviceMapping(DEFAULT_LINUX_DOCKER_DEVICE_NAME, ec2Config.getDockerVolumeType(), Integer.parseInt(ec2Config.getDockerVolumeSize()), ec2Config.getDockerVolumeProvisionedIOPS())
        );
    }

    private EbsBlockDevice withIops(EbsBlockDevice.Builder ebsBlockDevice, String volumeType, Integer provisionedIOPS) {
        return volumeType.equals(VOLUME_TYPE_IO_1) ? ebsBlockDevice.iops(provisionedIOPS).build() : ebsBlockDevice.build();
    }

    private String osDeviceName() {
        return ec2Config.getPlatform() == Platform.LINUX ? DEFAULT_LINUX_OS_DEVICE_NAME : DEFAULT_WINDOWS_OS_DEVICE_NAME;
    }

    public RunInstanceRequestBuilder instanceToCreate(int instanceToCreate) {
        this.instanceToCreate = instanceToCreate;
        return this;
    }

    public RunInstanceRequestBuilder eC2Config(EC2Config ec2Config) {
        this.ec2Config = ec2Config;
        return this;
    }

    public RunInstanceRequestBuilder subnet(Subnet subnet) {
        this.subnet = subnet;
        return this;
    }

    public RunInstanceRequestBuilder serverId(String serverId) {
        this.serverId = serverId;
        return this;
    }
}
