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

import com.amazonaws.services.ec2.model.IamInstanceProfileSpecification;
import com.amazonaws.services.ec2.model.ResourceType;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.TagSpecification;
import com.thoughtworks.gocd.elasticagent.ecs.Constants;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;

import java.util.Collection;
import java.util.Date;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.getServerId;
import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.getOrDefault;
import static org.joda.time.DateTime.now;

@ToString
public class EC2Config {
    private String instanceName;
    private String ami;
    private String instanceType;
    private String keyPair;
    private Collection<String> securityGroups;
    private String iamInstanceProfile;
    private Collection<String> subnetIds;
    private TagSpecification tagSpecification;
    private String userdata;
    private String operatingSystemVolumeType;
    private String operationSystemVolumeSize;
    private Integer operationSystemVolumeProvisionedIOPS;
    private Platform platform;
    private Period registerTimeOut;
    private Integer maxInstancesAllowed;
    private Integer minInstanceCount;
    private String dockerVolumeType;
    private String dockerVolumeSize;
    private Integer dockerVolumeProvisionedIOPS;
    private boolean runAsSpotInstance;
    private String spotPrice;
    private Integer spotRequestExpiresAfter;

    public String getAmi() {
        return ami;
    }

    public String getInstanceType() {
        return instanceType;
    }

    public String getSSHKeyName() {
        return keyPair;
    }

    public Collection<String> getSecurityGroups() {
        return securityGroups;
    }

    public IamInstanceProfileSpecification getIamInstanceProfile() {
        return new IamInstanceProfileSpecification().withName(iamInstanceProfile);
    }

    public Collection<String> getSubnetIds() {
        return subnetIds;
    }

    public TagSpecification getTagSpecification() {
        return tagSpecification;
    }

    public String getUserdata() {
        return userdata;
    }

    public String getOperatingSystemVolumeType() {
        return operatingSystemVolumeType;
    }

    public String getOperationSystemVolumeSize() {
        return operationSystemVolumeSize;
    }

    public Platform getPlatform() {
        return platform;
    }

    public Period getRegisterTimeOut() {
        return registerTimeOut;
    }

    public int getMaxInstancesAllowed() {
        return maxInstancesAllowed;
    }

    public int getMinInstanceCount() {
        return minInstanceCount;
    }

    public String getDockerVolumeType() {
        return dockerVolumeType;
    }

    public String getDockerVolumeSize() {
        return dockerVolumeSize;
    }

    public Integer getOperationSystemVolumeProvisionedIOPS() {
        return operationSystemVolumeProvisionedIOPS;
    }

    public Integer getDockerVolumeProvisionedIOPS() {
        return dockerVolumeProvisionedIOPS;
    }

    public String getInstanceName() {
        return this.instanceName;
    }

    public boolean runAsSpotInstance() {
        return runAsSpotInstance;
    }

    public String getSpotPrice() {
        return spotPrice;
    }

    public Date getSpotRequestValidUntil() {
        return now().plusMinutes(spotRequestExpiresAfter).toDate();
    }

    public static class Builder {
        static final String NAME = "Name";
        static final String CREATOR = "Creator";
        static final String PLATFORM = "Platform";
        private PluginSettings pluginSettings;
        private ElasticAgentProfileProperties elasticAgentProfileProperties;

        public Builder withSettings(PluginSettings pluginSettings) {
            this.pluginSettings = pluginSettings;
            return this;
        }

        public Builder withProfile(ElasticAgentProfileProperties elasticAgentProfileProperties) {
            this.elasticAgentProfileProperties = elasticAgentProfileProperties;
            return this;
        }

        public EC2Config build() {
            switch (elasticAgentProfileProperties.platform()) {
                case LINUX:
                    return createLinuxConfig();
                case WINDOWS:
                    return createWindowsConfig();
            }

            throw new RuntimeException("Platform must be specified at elastic profile level.");
        }

        private EC2Config createWindowsConfig() {
            final EC2Config ec2Config = new EC2Config();
            ec2Config.platform = elasticAgentProfileProperties.platform();
            ec2Config.ami = getOrDefault(elasticAgentProfileProperties.getAmiID(), pluginSettings.getWindowsAMI());
            ec2Config.instanceType = getOrDefault(elasticAgentProfileProperties.getInstanceType(), pluginSettings.getWindowsInstanceType());

            ec2Config.operatingSystemVolumeType = pluginSettings.getWindowsOSVolumeType();
            ec2Config.operationSystemVolumeSize = pluginSettings.getWindowsOSVolumeSize();
            ec2Config.operationSystemVolumeProvisionedIOPS = pluginSettings.getWindowsOSVolumeProvisionedIOPS();

            ec2Config.registerTimeOut = pluginSettings.getWindowsRegisterTimeout();
            ec2Config.maxInstancesAllowed = pluginSettings.getMaxWindowsInstancesAllowed();
            ec2Config.minInstanceCount = pluginSettings.getMinWindowsInstanceCount();

            ec2Config.securityGroups = getOrDefault(elasticAgentProfileProperties.getSecurityGroupIds(), pluginSettings.getSecurityGroupIds());
            ec2Config.iamInstanceProfile = getOrDefault(elasticAgentProfileProperties.getEC2IamInstanceProfile(), pluginSettings.getIAMInstanceProfile());
            ec2Config.subnetIds = getOrDefault(elasticAgentProfileProperties.getSubnetIds(), pluginSettings.getSubnetIds());
            ec2Config.keyPair = pluginSettings.getKeyPairName();
            ec2Config.tagSpecification = getTagSpecification();
            ec2Config.userdata = getEncodedUserData(pluginSettings.getWindowsUserdataScript());
            ec2Config.runAsSpotInstance = elasticAgentProfileProperties.runAsSpotInstance();
            ec2Config.spotPrice = elasticAgentProfileProperties.getSpotPrice();
            ec2Config.spotRequestExpiresAfter = elasticAgentProfileProperties.getSpotRequestExpiresAfter();

            return ec2Config;
        }

        private EC2Config createLinuxConfig() {
            final EC2Config ec2Config = new EC2Config();
            ec2Config.platform = elasticAgentProfileProperties.platform();
            ec2Config.ami = getOrDefault(elasticAgentProfileProperties.getAmiID(), pluginSettings.getLinuxAMI());
            ec2Config.instanceType = getOrDefault(elasticAgentProfileProperties.getInstanceType(), pluginSettings.getLinuxInstanceType());
            ec2Config.operatingSystemVolumeType = pluginSettings.getLinuxOSVolumeType();
            ec2Config.operationSystemVolumeSize = pluginSettings.getLinuxOSVolumeSize();
            ec2Config.operationSystemVolumeProvisionedIOPS = pluginSettings.getLinuxOSVolumeProvisionedIOPS();

            ec2Config.dockerVolumeType = pluginSettings.getLinuxVolumeType();
            ec2Config.dockerVolumeSize = pluginSettings.getLinuxVolumeSize();
            ec2Config.dockerVolumeProvisionedIOPS = pluginSettings.getLinuxVolumeProvisionedIOPS();
            ec2Config.registerTimeOut = pluginSettings.getLinuxRegisterTimeout();
            ec2Config.maxInstancesAllowed = pluginSettings.getMaxLinuxInstancesAllowed();
            ec2Config.minInstanceCount = pluginSettings.getMinLinuxInstanceCount();

            ec2Config.securityGroups = getOrDefault(elasticAgentProfileProperties.getSecurityGroupIds(), pluginSettings.getSecurityGroupIds());
            ec2Config.iamInstanceProfile = getOrDefault(elasticAgentProfileProperties.getEC2IamInstanceProfile(), pluginSettings.getIAMInstanceProfile());
            ec2Config.subnetIds = getOrDefault(elasticAgentProfileProperties.getSubnetIds(), pluginSettings.getSubnetIds());
            ec2Config.keyPair = pluginSettings.getKeyPairName();
            ec2Config.tagSpecification = getTagSpecification();
            ec2Config.userdata = getEncodedUserData(pluginSettings.getLinuxUserdataScript());
            ec2Config.runAsSpotInstance = elasticAgentProfileProperties.runAsSpotInstance();
            ec2Config.spotPrice = elasticAgentProfileProperties.getSpotPrice();
            ec2Config.spotRequestExpiresAfter = elasticAgentProfileProperties.getSpotRequestExpiresAfter();

            return ec2Config;
        }

        private TagSpecification getTagSpecification() {
            return new TagSpecification().withTags(
                    new Tag(NAME, String.format("%s_%s_INSTANCE", pluginSettings.getClusterName(), elasticAgentProfileProperties.platform())),
                    new Tag(CREATOR, Constants.PLUGIN_ID),
                    new Tag(PLATFORM, elasticAgentProfileProperties.platform().name())
            ).withResourceType(ResourceType.Instance);
        }

        private String getEncodedUserData(String userdataScript) {

            final Userdata userdata = new Userdata()
                    .platform(elasticAgentProfileProperties.platform())
                    .clusterName(pluginSettings.getClusterName())
                    .cleanupTaskAfter(1, TimeUnit.MINUTES)
                    .imageCleanupAge(24, TimeUnit.HOURS)
                    .dockerRegistry(pluginSettings.getPrivateDockerRegistryAuthType(), pluginSettings.getPrivateDockerRegistryAuthData())
                    .attribute(Constants.LABEL_SERVER_ID, getServerId())
                    .efs(pluginSettings.efsDnsOrIP(), pluginSettings.efsMountLocation())
                    .initScript(StringUtils.stripToEmpty(userdataScript));

            if (StringUtils.isNotBlank(pluginSettings.getMaxContainerDataVolumeSize())) {
                userdata.storageOption("dm.basesize", pluginSettings.getMaxContainerDataVolumeSize() + "G");
            }

            return userdata.toBase64();
        }
    }
}
