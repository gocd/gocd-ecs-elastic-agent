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

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LABEL_SERVER_ID;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.validators.VolumeSettingsValidator.VOLUME_TYPE_IO_1;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class RunInstanceRequestBuilder {
    private static final String DEFAULT_LINUX_OS_DEVICE_NAME = "/dev/xvda";
    private static final String DEFAULT_WINDOWS_OS_DEVICE_NAME = "/dev/sda1";
    private static final String DEFAULT_LINUX_DOCKER_DEVICE_NAME = "/dev/xvdcz";

    private int instanceToCreate;
    private EC2Config ec2Config;
    private Subnet subnet;
    private String serverId;

    public RunInstancesRequest build() {
        return buildWithBaseConfiguration(subnet)
                .withImageId(ec2Config.getAmi())
                .withInstanceType(ec2Config.getInstanceType())
                .withSecurityGroupIds(ec2Config.getSecurityGroups())
                .withIamInstanceProfile(ec2Config.getIamInstanceProfile());
    }

    private RunInstancesRequest buildWithBaseConfiguration(Subnet subnet) {
        final String subnetId = subnet != null ? subnet.getSubnetId() : null;
        final RunInstancesRequest request = new RunInstancesRequest()
                .withMinCount(instanceToCreate)
                .withMaxCount(instanceToCreate)
                .withKeyName(ec2Config.getSSHKeyName())
                .withTagSpecifications(ec2Config.getTagSpecification().withTags(new Tag(LABEL_SERVER_ID, serverId)))
                .withUserData(ec2Config.getUserdata())
                .withSubnetId(subnetId);

        blockOperatingSystemVolume(request);
        blockDockerVolume(request);

        return request;
    }


    private void blockOperatingSystemVolume(RunInstancesRequest request) {
        if (isBlank(ec2Config.getOperatingSystemVolumeType()) || "none".equals(ec2Config.getOperatingSystemVolumeType())) {
            return;
        }

        request.withBlockDeviceMappings(
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

    private void blockDockerVolume(RunInstancesRequest request) {
        if (ec2Config.getPlatform() == Platform.WINDOWS) {
            LOG.debug("As windows is using root volumes to store everything extra volume for docker is not need.");
            return;
        }

        if (isBlank(ec2Config.getDockerVolumeType()) || "none".equals(ec2Config.getDockerVolumeType())) {
            return;
        }

        request.withBlockDeviceMappings(
                blockDeviceMapping(DEFAULT_LINUX_DOCKER_DEVICE_NAME, ec2Config.getDockerVolumeType(), Integer.parseInt(ec2Config.getDockerVolumeSize()), ec2Config.getDockerVolumeProvisionedIOPS())
        );
    }

    private EbsBlockDevice withIops(EbsBlockDevice ebsBlockDevice, String volumeType, Integer provisionedIOPS) {
        return volumeType.equals(VOLUME_TYPE_IO_1) ? ebsBlockDevice.withIops(provisionedIOPS) : ebsBlockDevice;
    }

    private String osDeviceName() {
        return ec2Config.getPlatform() == Platform.LINUX ? DEFAULT_LINUX_OS_DEVICE_NAME : DEFAULT_WINDOWS_OS_DEVICE_NAME;
    }

    public RunInstanceRequestBuilder instanceToCreate(int instanceToCreate) {
        this.instanceToCreate = instanceToCreate;
        return this;
    }

    public RunInstanceRequestBuilder withEC2Config(EC2Config ec2Config) {
        this.ec2Config = ec2Config;
        return this;
    }

    public RunInstanceRequestBuilder withSubnet(Subnet subnet) {
        this.subnet = subnet;
        return this;
    }

    public RunInstanceRequestBuilder withServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }
}
