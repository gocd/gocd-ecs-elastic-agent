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

import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.validators.VolumeSettingsValidator.VOLUME_TYPE_IO_1;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class SpotInstanceRequestBuilder {
    private static final String DEFAULT_LINUX_OS_DEVICE_NAME = "/dev/xvda";
    private static final String DEFAULT_WINDOWS_OS_DEVICE_NAME = "/dev/sda1";
    private static final String DEFAULT_LINUX_DOCKER_DEVICE_NAME = "/dev/xvdcz";

    private EC2Config ec2Config;
    private Subnet subnet;

    public RequestSpotInstancesRequest build() {
        return buildWithBaseConfiguration()
                .withLaunchSpecification(launchSpecifications());
    }

    public SpotInstanceRequestBuilder withEC2Config(EC2Config ec2Config) {
        this.ec2Config = ec2Config;
        return this;
    }

    public SpotInstanceRequestBuilder withSubnet(Subnet subnet) {
        this.subnet = subnet;
        return this;
    }

    private RequestSpotInstancesRequest buildWithBaseConfiguration() {
        final RequestSpotInstancesRequest request = new RequestSpotInstancesRequest()
                .withSpotPrice(ec2Config.getSpotPrice())
                .withValidUntil(ec2Config.getSpotRequestValidUntil());

        return request;
    }

    private LaunchSpecification launchSpecifications() {
        final String subnetId = this.subnet != null ? this.subnet.getSubnetId() : null;
        LaunchSpecification launchSpecification = new LaunchSpecification();
        launchSpecification
                .withImageId(this.ec2Config.getAmi())
                .withInstanceType(this.ec2Config.getInstanceType())
                .withAllSecurityGroups(securityGroups())
                .withIamInstanceProfile(this.ec2Config.getIamInstanceProfile())
                .withKeyName(ec2Config.getSSHKeyName())
                .withSubnetId(subnetId)
                .withUserData(ec2Config.getUserdata());

        blockOperatingSystemVolume(launchSpecification);
        blockDockerVolume(launchSpecification);

        return launchSpecification;
    }

    private List<GroupIdentifier> securityGroups() {
        return ec2Config.getSecurityGroups().stream()
                .map(s -> new GroupIdentifier().withGroupId(s))
                .collect(toList());
    }

    private void blockOperatingSystemVolume(LaunchSpecification launchSpecification) {
        if (isBlank(ec2Config.getOperatingSystemVolumeType()) || "none".equals(ec2Config.getOperatingSystemVolumeType())) {
            return;
        }

        launchSpecification.withBlockDeviceMappings(
                blockDeviceMapping(osDeviceName(), ec2Config.getOperatingSystemVolumeType(), Integer.parseInt(ec2Config.getOperationSystemVolumeSize()), ec2Config.getOperationSystemVolumeProvisionedIOPS())
        );
    }

    private BlockDeviceMapping blockDeviceMapping(String deviceName, String volumeType, int volumeSize, Integer provisionedIOPS) {
        EbsBlockDevice ebsBlockDevice = new EbsBlockDevice()
                .withDeleteOnTermination(true)
                .withVolumeType(volumeType)
                .withVolumeSize(volumeSize);
        return new BlockDeviceMapping()
                .withEbs(withIops(ebsBlockDevice, volumeType, provisionedIOPS))
                .withDeviceName(deviceName);
    }

    private void blockDockerVolume(LaunchSpecification launchSpecification) {
        if (ec2Config.getPlatform() == Platform.WINDOWS) {
            LOG.debug("As windows is using root volumes to store everything extra volume for docker is not need.");
            return;
        }

        if (isBlank(ec2Config.getDockerVolumeType()) || "none".equals(ec2Config.getDockerVolumeType())) {
            return;
        }

        launchSpecification.withBlockDeviceMappings(
                blockDeviceMapping(DEFAULT_LINUX_DOCKER_DEVICE_NAME, ec2Config.getDockerVolumeType(), Integer.parseInt(ec2Config.getDockerVolumeSize()), ec2Config.getDockerVolumeProvisionedIOPS())
        );
    }

    private EbsBlockDevice withIops(EbsBlockDevice ebsBlockDevice, String volumeType, Integer provisionedIOPS) {
        return volumeType.equals(VOLUME_TYPE_IO_1) ? ebsBlockDevice.withIops(provisionedIOPS) : ebsBlockDevice;
    }

    private String osDeviceName() {
        return ec2Config.getPlatform() == Platform.LINUX ? DEFAULT_LINUX_OS_DEVICE_NAME : DEFAULT_WINDOWS_OS_DEVICE_NAME;
    }
}
