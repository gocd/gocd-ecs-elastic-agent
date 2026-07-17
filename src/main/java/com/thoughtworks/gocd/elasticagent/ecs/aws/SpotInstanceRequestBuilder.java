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

import static com.thoughtworks.gocd.elasticagent.ecs.validators.VolumeSettingsValidator.VOLUME_TYPE_IO_1;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class SpotInstanceRequestBuilder {
    private static final Logger LOG = Logger.getLoggerFor(SpotInstanceRequestBuilder.class);

    private static final String DEFAULT_LINUX_OS_DEVICE_NAME = "/dev/xvda";
    private static final String DEFAULT_WINDOWS_OS_DEVICE_NAME = "/dev/sda1";
    private static final String DEFAULT_LINUX_DOCKER_DEVICE_NAME = "/dev/xvdcz";

    private EC2Config ec2Config;
    private Subnet subnet;

    public RequestSpotInstancesRequest build() {
        return buildWithBaseConfiguration()
                .launchSpecification(launchSpecifications())
                .build();
    }

    public SpotInstanceRequestBuilder eC2Config(EC2Config ec2Config) {
        this.ec2Config = ec2Config;
        return this;
    }

    public SpotInstanceRequestBuilder subnet(Subnet subnet) {
        this.subnet = subnet;
        return this;
    }

    private RequestSpotInstancesRequest.Builder buildWithBaseConfiguration() {
        return RequestSpotInstancesRequest.builder()
                .spotPrice(ec2Config.getSpotPrice())
                .validUntil(ec2Config.getSpotRequestValidUntil());
    }

    private RequestSpotLaunchSpecification launchSpecifications() {
        final String subnetId = this.subnet != null ? this.subnet.subnetId() : null;
        RequestSpotLaunchSpecification.Builder launchSpecification = RequestSpotLaunchSpecification.builder();
        launchSpecification
                .imageId(this.ec2Config.getAmi())
                .instanceType(this.ec2Config.getInstanceType())
                .securityGroupIds(ec2Config.getSecurityGroups())
                .iamInstanceProfile(this.ec2Config.getIamInstanceProfile())
                .keyName(ec2Config.getSSHKeyName())
                .subnetId(subnetId)
                .userData(ec2Config.getUserdata());

        final List<BlockDeviceMapping> blockDeviceMappings = new ArrayList<>();
        blockOperatingSystemVolume(blockDeviceMappings);
        blockDockerVolume(blockDeviceMappings);
        if (!blockDeviceMappings.isEmpty()) {
            launchSpecification.blockDeviceMappings(blockDeviceMappings);
        }

        return launchSpecification.build();
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
            LOG.debug("As windows is using root volumes to store everything extra volume for docker is not need.");
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
}
