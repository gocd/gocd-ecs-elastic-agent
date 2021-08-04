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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.gocd.elasticagent.ecs.domain.annotation.Metadata;
import com.thoughtworks.gocd.elasticagent.ecs.size.Size;
import lombok.EqualsAndHashCode;

import java.text.MessageFormat;
import java.util.Collection;
import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.annotation.FieldType.MEMORY;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.annotation.FieldType.NUMBER;
import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.*;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.numberOfLeadingZeros;
import static java.lang.Integer.parseInt;
import static java.util.Collections.emptyList;
import static org.apache.commons.lang3.StringUtils.*;

@EqualsAndHashCode
public class ElasticAgentProfileProperties {
    public static final String KEY_MAX_MEMORY = "MaxMemory";
    public static final String KEY_RESERVED_MEMORY = "ReservedMemory";
    public static final String AMI = "AMI";
    public static final String INSTANCE_TYPE = "InstanceType";
    public static final String IAM_INSTANCE_PROFILE = "IAMInstanceProfile";
    public static final String TASK_ROLE_ARN = "TaskRoleArn";
    public static final String SECURITY_GROUP_IDS = "SecurityGroupIds";
    public static final String SUBNET_IDS = "SubnetIds";
    public static final String SECRET_NAME = "SecretName";
    public static final String SECRET_VALUE = "SecretValue";
    public static final String EXECUTION_ROLE_ARN = "ExecutionRoleArn";
    public static final String PLATFORM = "Platform";
    public static final String BIND_MOUNT = "BindMount";
    public static final String RUN_AS_SPOT_INSTANCE = "RunAsSpotInstance";
    public static final String SPOT_PRICE = "SpotPrice";
    public static final String SPOT_REQUEST_EXPIRES_AFTER = "SpotRequestExpiresAfter";

    @Expose
    @SerializedName("Image")
    @Metadata(key = "Image", required = true, secure = false)
    public String image;

    @Expose
    @SerializedName("Command")
    @Metadata(key = "Command", required = false, secure = false)
    public String command;

    @Expose
    @SerializedName("Environment")
    @Metadata(key = "Environment", required = false, secure = false)
    public String environment;

    @Expose
    @SerializedName(KEY_MAX_MEMORY)
    @Metadata(key = KEY_MAX_MEMORY, required = true, secure = false, type = MEMORY)
    public String maxMemory;

    @Expose
    @SerializedName(KEY_RESERVED_MEMORY)
    @Metadata(key = KEY_RESERVED_MEMORY, required = false, secure = false, type = MEMORY)
    public String reservedMemory;

    @Expose
    @SerializedName("CPU")
    @Metadata(key = "CPU", required = true, secure = false, type = NUMBER)
    public String cpu;

    @Expose
    @SerializedName("MountDockerSocket")
    @Metadata(key = "MountDockerSocket", required = false, secure = false)
    public String mountDockerSocket;

    @Expose
    @SerializedName("Privileged")
    @Metadata(key = "Privileged", required = false, secure = false)
    public String privileged;

    @Expose
    @SerializedName(TASK_ROLE_ARN)
    @Metadata(key = TASK_ROLE_ARN, required = false, secure = false)
    public String taskRoleArn;

    @Expose
    @SerializedName(AMI)
    @Metadata(key = AMI, required = false, secure = false)
    private String ec2AMI;

    @Expose
    @SerializedName(INSTANCE_TYPE)
    @Metadata(key = INSTANCE_TYPE, required = false, secure = false)
    private String ec2InstanceType;

    @Expose
    @SerializedName(SUBNET_IDS)
    @Metadata(key = SUBNET_IDS, required = false, secure = false)
    private String subnetIds;

    @Expose
    @SerializedName(SECRET_NAME)
    @Metadata(key = SECRET_NAME, required = false, secure = false)
    private String secretName;

    @Expose
    @SerializedName(SECRET_VALUE)
    @Metadata(key = SECRET_VALUE, required = false, secure = false)
    private String secretValue;

    @Expose
    @SerializedName(EXECUTION_ROLE_ARN)
    @Metadata(key = EXECUTION_ROLE_ARN, required = false, secure = false)
    private String executionRoleArn;

    @Expose
    @SerializedName(SECURITY_GROUP_IDS)
    @Metadata(key = SECURITY_GROUP_IDS, required = false, secure = false)
    private String securityGroupIds;

    @Expose
    @SerializedName(IAM_INSTANCE_PROFILE)
    @Metadata(key = IAM_INSTANCE_PROFILE, required = false, secure = false)
    private String iamInstanceProfile;

    @Expose
    @SerializedName(PLATFORM)
    @Metadata(key = PLATFORM, required = false, secure = false)
    private String platform;

    @Expose
    @SerializedName(BIND_MOUNT)
    @Metadata(key = BIND_MOUNT, required = false, secure = false)
    private String bindMount;

    @Expose
    @SerializedName(RUN_AS_SPOT_INSTANCE)
    @Metadata(key = RUN_AS_SPOT_INSTANCE, required = false, secure = false)
    private boolean runAsSpotInstance = false;

    @Expose
    @SerializedName(SPOT_PRICE)
    @Metadata(key = SPOT_PRICE, required = false, secure = false)
    private String spotPrice;

    @Expose
    @SerializedName(SPOT_REQUEST_EXPIRES_AFTER)
    @Metadata(key = SPOT_REQUEST_EXPIRES_AFTER, required = false, secure = false)
    private String spotRequestExpiresAfter;

    public static ElasticAgentProfileProperties fromJson(String json) {
        return GSON.fromJson(json, ElasticAgentProfileProperties.class);
    }

    public List<BindMount> bindMounts() {
        if(isEmpty(this.bindMount)) {
            return emptyList();
        }

        return GSON.fromJson(this.bindMount,  new TypeToken<List<BindMount>>(){}.getType());
    }

    public String getImage() {
        return stripToEmpty(image);
    }

    public Collection<String> getCommand() {
        return splitIntoLinesAndTrimSpaces(command);
    }

    public Collection<String> getEnvironment() {
        return splitIntoLinesAndTrimSpaces(environment);
    }

    public Integer getMaxMemory() {
        return parseMemory(stripToEmpty(maxMemory), KEY_MAX_MEMORY);
    }

    public Integer getReservedMemory() {
        return parseMemory(stripToEmpty(reservedMemory), KEY_RESERVED_MEMORY);
    }

    public String getAmiID() {
        return stripToEmpty(ec2AMI);
    }

    public String getInstanceType() {
        return stripToEmpty(ec2InstanceType);
    }

    public List<String> getSubnetIds() {
        return listFromCommaSeparatedString(subnetIds);
    }

    public String getSecretName() {
        return stripToEmpty(secretName);
    }

    public String getSecretValue() {
        return stripToEmpty(secretValue);
    }

    public String getExecutionRoleArn() {
        return executionRoleArn;
    }

    public List<String> getSecurityGroupIds() {
        return listFromCommaSeparatedString(securityGroupIds);
    }

    public String getEC2IamInstanceProfile() {
        return stripToEmpty(iamInstanceProfile);
    }

    private Integer parseMemory(String memory, String fieldName) {
        try {
            return Size.parse(memory).toMegabytes().intValue();
        } catch (Exception e) {
            LOG.error(MessageFormat.format("Failed to parse `{0}`:", fieldName), e);
            return null;
        }
    }

    public String getTaskRoleArn() {
        return taskRoleArn;
    }

    public Integer getCpu() {
        return getIntOrDefault(cpu, null);
    }

    public boolean isMountDockerSocket() {
        return parseBoolean(mountDockerSocket);
    }

    public boolean isPrivileged() {
        return parseBoolean(privileged);
    }

    public Platform platform() {
        return Platform.from(platform);
    }

    public boolean runAsSpotInstance() {
        return runAsSpotInstance;
    }

    public String getSpotPrice() {
        return spotPrice;
    }

    public Integer getSpotRequestExpiresAfter() {
        return getIntOrDefault(spotRequestExpiresAfter, 5);
    }

    public String toJson() {
        return GSON.toJson(this);
    }

    public static ElasticAgentProfileProperties empty(Platform platform) {
        final ElasticAgentProfileProperties elasticAgentProfileProperties = new ElasticAgentProfileProperties();
        elasticAgentProfileProperties.platform = platform.name();
        return elasticAgentProfileProperties;
    }
}
