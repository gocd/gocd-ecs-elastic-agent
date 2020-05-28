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

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.retry.PredefinedRetryPolicies;
import com.amazonaws.retry.RetryPolicy;
import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.AmazonEC2ClientBuilder;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.AmazonECSClientBuilder;
import com.amazonaws.services.ecs.model.LogConfiguration;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.gocd.elasticagent.ecs.aws.AWSCredentialsProviderChain;
import com.thoughtworks.gocd.elasticagent.ecs.aws.StopPolicy;
import com.thoughtworks.gocd.elasticagent.ecs.domain.annotation.Metadata;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.Period;

import java.text.MessageFormat;
import java.util.*;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthType.AUTH_TOKEN;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthType.USERNAME_PASSWORD;
import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.*;
import static java.lang.Integer.parseInt;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.stripToNull;

public class PluginSettings {
    public static final Gson GSON = new GsonBuilder()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .excludeFieldsWithoutExposeAnnotation()
            .create();

    @Expose
    @SerializedName("GoServerUrl")
    @Metadata(key = "GoServerUrl", required = true, secure = false)
    private String goServerUrl;

    @Expose
    @SerializedName("ClusterName")
    @Metadata(key = "ClusterName", required = true, secure = false)
    private String clusterName;

    @Expose
    @SerializedName("AWSRegion")
    @Metadata(key = "AWSRegion", required = false, secure = false)
    private String region;

    @Expose
    @SerializedName("AWSAccessKeyId")
    @Metadata(key = "AWSAccessKeyId", required = false, secure = true)
    private String accessKeyId;

    @Expose
    @SerializedName("AWSSecretAccessKey")
    @Metadata(key = "AWSSecretAccessKey", required = false, secure = true)
    private String secretAccessKey;

    @Expose
    @SerializedName("EnvironmentVariables")
    @Metadata(key = "EnvironmentVariables", required = false, secure = false)
    private String environmentVariables;

    @Expose
    @SerializedName("ContainerAutoregisterTimeout")
    @Metadata(key = "ContainerAutoregisterTimeout", required = false, secure = false)
    private String containerAutoregisterTimeout;

    @Expose
    @SerializedName("MaxContainerDataVolumeSize")
    @Metadata(key = "MaxContainerDataVolumeSize", required = false, secure = false)
    private String maxContainerDataVolumeSize;

    @Expose
    @SerializedName("KeyPairName")
    @Metadata(key = "KeyPairName", required = false, secure = false)
    private String keyPairName;

    @Expose
    @SerializedName("IamInstanceProfile")
    @Metadata(key = "IamInstanceProfile", required = true, secure = false)
    private String iamInstanceProfile;

    @Expose
    @SerializedName("SubnetIds")
    @Metadata(key = "SubnetIds", required = false, secure = false)
    private String subnetIds;

    @Expose
    @SerializedName("SecurityGroupIds")
    @Metadata(key = "SecurityGroupIds", required = false, secure = false)
    private String securityGroupIds;

    @Expose
    @SerializedName("LogDriver")
    @Metadata(key = "LogDriver", required = false, secure = false)
    private String logDriverName;

    @Expose
    @SerializedName("LogOptions")
    @Metadata(key = "LogOptions", required = false, secure = false)
    private String logOptions;

    @Expose
    @SerializedName("LinuxAmi")
    @Metadata(key = "LinuxAmi", required = false, secure = false)
    private String linuxAMI;

    @Expose
    @SerializedName("LinuxInstanceType")
    @Metadata(key = "LinuxInstanceType", required = false, secure = false)
    private String linuxInstanceType;

    @Expose
    @SerializedName("LinuxRegisterTimeout")
    @Metadata(key = "LinuxRegisterTimeout", required = false, secure = false)
    private String linuxRegisterTimeout;

    @Expose
    @SerializedName("MinLinuxInstanceCount")
    @Metadata(key = "MinLinuxInstanceCount", required = false, secure = false)
    private String minLinuxInstanceCount;

    @Expose
    @SerializedName("MaxLinuxInstancesAllowed")
    @Metadata(key = "MaxLinuxInstancesAllowed", required = false, secure = false)
    private String maxLinuxInstancesAllowed;

    @Expose
    @SerializedName("MaxLinuxSpotInstanceAllowed")
    @Metadata(key = "MaxLinuxSpotInstanceAllowed", required = false, secure = false)
    private String maxLinuxSpotInstanceAllowed;

    @Expose
    @SerializedName("LinuxDockerVolumeType")
    @Metadata(key = "LinuxDockerVolumeType", required = false, secure = false)
    private String linuxVolumeType;

    @Expose
    @SerializedName("LinuxDockerVolumeSize")
    @Metadata(key = "LinuxDockerVolumeSize", required = false, secure = false)
    private String linuxVolumeSize;

    @Expose
    @SerializedName("LinuxDockerVolumeProvisionedIOPS")
    @Metadata(key = "LinuxDockerVolumeProvisionedIOPS", required = false, secure = false)
    private String linuxVolumeProvisionedIOPS;

    @Expose
    @SerializedName("LinuxOSVolumeType")
    @Metadata(key = "LinuxOSVolumeType", required = false, secure = false)
    private String linuxOSVolumeType;

    @Expose
    @SerializedName("LinuxOSVolumeSize")
    @Metadata(key = "LinuxOSVolumeSize", required = false, secure = false)
    private String linuxOSVolumeSize;

    @Expose
    @SerializedName("LinuxOSVolumeProvisionedIOPS")
    @Metadata(key = "LinuxOSVolumeProvisionedIOPS", required = false, secure = false)
    private String linuxOSVolumeProvisionedIOPS;

    @Expose
    @SerializedName("LinuxUserdataScript")
    @Metadata(key = "LinuxUserdataScript", required = false, secure = false)
    private String linuxUserdataScript;

    @Expose
    @SerializedName("LinuxStopPolicy")
    @Metadata(key = "LinuxStopPolicy", required = false, secure = false)
    private StopPolicy linuxStopPolicy;

    @Expose
    @SerializedName("StopLinuxInstanceAfter")
    @Metadata(key = "StopLinuxInstanceAfter", required = false, secure = false)
    private String stopLinuxInstanceAfter;

    @Expose
    @SerializedName("TerminateStoppedLinuxInstanceAfter")
    @Metadata(key = "TerminateStoppedLinuxInstanceAfter", required = false, secure = false)
    private String terminateStoppedLinuxInstanceAfter = null;

    @Expose
    @SerializedName("TerminateIdleLinuxSpotInstanceAfter")
    @Metadata(key = "TerminateIdleLinuxSpotInstanceAfter", required = false, secure = false)
    private String terminateIdleLinuxSpotInstanceAfter = null;

    @Expose
    @SerializedName("WindowsAmi")
    @Metadata(key = "WindowsAmi", required = false, secure = false)
    private String windowsAMI;

    @Expose
    @SerializedName("WindowsInstanceType")
    @Metadata(key = "WindowsInstanceType", required = false, secure = false)
    private String windowsInstanceType;

    @Expose
    @SerializedName("WindowsOSVolumeType")
    @Metadata(key = "WindowsOSVolumeType", required = false, secure = false)
    private String windowsVolumeType;

    @Expose
    @SerializedName("WindowsOSVolumeSize")
    @Metadata(key = "WindowsOSVolumeSize", required = false, secure = false)
    private String windowsVolumeSize;

    @Expose
    @SerializedName("WindowsOSVolumeProvisionedIOPS")
    @Metadata(key = "WindowsOSVolumeProvisionedIOPS", required = false, secure = false)
    private String windowsOSVolumeProvisionedIOPS;

    @Expose
    @SerializedName("WindowsRegisterTimeout")
    @Metadata(key = "WindowsRegisterTimeout", required = false, secure = false)
    private String windowsRegisterTimeout;

    @Expose
    @SerializedName("MinWindowsInstanceCount")
    @Metadata(key = "MinWindowsInstanceCount", required = false, secure = false)
    private String minWindowsInstanceCount;

    @Expose
    @SerializedName("MaxWindowsInstancesAllowed")
    @Metadata(key = "MaxWindowsInstancesAllowed", required = false, secure = false)
    private String maxWindowsInstancesAllowed;

    @Expose
    @SerializedName("MaxWindowsSpotInstanceAllowed")
    @Metadata(key = "MaxWindowsSpotInstanceAllowed", required = false, secure = false)
    private String maxWindowsSpotInstanceAllowed;

    @Expose
    @SerializedName("WindowsUserdataScript")
    @Metadata(key = "WindowsUserdataScript", required = false, secure = false)
    private String windowsUserdataScript;

    @Expose
    @SerializedName("WindowsStopPolicy")
    @Metadata(key = "WindowsStopPolicy", required = false, secure = false)
    private StopPolicy windowsStopPolicy;

    @Expose
    @SerializedName("StopWindowsInstanceAfter")
    @Metadata(key = "StopWindowsInstanceAfter", required = false, secure = false)
    private String stopWindowsInstanceAfter;

    @Expose
    @SerializedName("TerminateStoppedWindowsInstanceAfter")
    @Metadata(key = "TerminateStoppedWindowsInstanceAfter", required = false, secure = false)
    private String terminateStoppedWindowsInstanceAfter;

    @Expose
    @SerializedName("TerminateIdleWindowsSpotInstanceAfter")
    @Metadata(key = "TerminateIdleWindowsSpotInstanceAfter", required = false, secure = false)
    private String terminateIdleWindowsSpotInstanceAfter;

    @Expose
    @SerializedName("PrivateDockerRegistryAuthType")
    @Metadata(key = "PrivateDockerRegistryAuthType", required = false, secure = false)
    private String privateDockerRegistryAuthType;

    @Expose
    @SerializedName("PrivateDockerRegistryAuthToken")
    @Metadata(key = "PrivateDockerRegistryAuthToken", required = false, secure = false)
    private String privateDockerRegistryAuthToken;

    @Expose
    @SerializedName("PrivateDockerRegistryUrl")
    @Metadata(key = "PrivateDockerRegistryUrl", required = false, secure = false)
    private String privateDockerRegistryUrl;

    @Expose
    @SerializedName("PrivateDockerRegistryEmail")
    @Metadata(key = "PrivateDockerRegistryEmail", required = false, secure = false)
    private String privateDockerRegistryEmail;

    @Expose
    @SerializedName("PrivateDockerRegistryUsername")
    @Metadata(key = "PrivateDockerRegistryUsername", required = false, secure = false)
    private String privateDockerRegistryUsername;

    @Expose
    @SerializedName("PrivateDockerRegistryPassword")
    @Metadata(key = "PrivateDockerRegistryPassword", required = false, secure = true)
    private String privateDockerRegistryPassword;

    @Expose
    @SerializedName("EfsDnsOrIP")
    @Metadata(key = "EfsDnsOrIP", required = false, secure = false)
    private String efsDnsOrIP;

    public String getAccessKeyId() {
        return accessKeyId;
    }

    public String getSecretAccessKey() {
        return secretAccessKey;
    }

    public String getGoServerUrl() {
        return goServerUrl;
    }

    public Collection<String> getEnvironmentVariables() {
        return Util.splitIntoLinesAndTrimSpaces(environmentVariables);
    }

    public Period getContainerAutoregisterTimeout() {
        if (StringUtils.isBlank(containerAutoregisterTimeout)) {
            return Period.ZERO;
        }

        return new Period().withMinutes(parseInt(containerAutoregisterTimeout));
    }

    public String getMaxContainerDataVolumeSize() {
        return maxContainerDataVolumeSize;
    }

    public StopPolicy getLinuxStopPolicy() {
        return linuxStopPolicy;
    }

    public StopPolicy getWindowsStopPolicy() {
        return windowsStopPolicy;
    }

    public String getClusterName() {
        return clusterName;
    }

    public String getRegion() {
        return region;
    }

    public List<String> getSubnetIds() {
        return listFromCommaSeparatedString(subnetIds);
    }

    public List<String> getSecurityGroupIds() {
        return listFromCommaSeparatedString(securityGroupIds);
    }

    public LogConfiguration logConfiguration() {
        if (isBlank(logDriverName)) {
            return null;
        }
        return new LogConfiguration().withLogDriver(logDriverName).withOptions(getLogOptions());
    }

    public String getKeyPairName() {
        return stripToNull(keyPairName);
    }

    public String getIAMInstanceProfile() {
        return iamInstanceProfile;
    }

    public String getLinuxAMI() {
        return linuxAMI;
    }

    public String getLinuxInstanceType() {
        return linuxInstanceType;
    }

    public int getMinLinuxInstanceCount() {
        return getIntOrDefault(minLinuxInstanceCount, 0);
    }

    public int getMaxLinuxInstancesAllowed() {
        return getIntOrDefault(this.maxLinuxInstancesAllowed, 5);
    }

    public int getMaxLinuxSpotInstanceAllowed() {
        return getIntOrDefault(this.maxLinuxSpotInstanceAllowed, 10);
    }

    public String getLinuxVolumeType() {
        return linuxVolumeType;
    }

    public String getLinuxVolumeSize() {
        return linuxVolumeSize;
    }

    public Integer getLinuxVolumeProvisionedIOPS() {
        return getIntOrDefault(linuxVolumeProvisionedIOPS, null);
    }

    public Period getLinuxRegisterTimeout() {
        return new Period().withMinutes(getIntOrDefault(this.linuxRegisterTimeout, 5));
    }

    public String getWindowsAMI() {
        return windowsAMI;
    }

    public String getWindowsInstanceType() {
        return windowsInstanceType;
    }

    public String getWindowsOSVolumeType() {
        return windowsVolumeType;
    }

    public String getWindowsOSVolumeSize() {
        return windowsVolumeSize;
    }

    public Integer getWindowsOSVolumeProvisionedIOPS() {
        return getIntOrDefault(windowsOSVolumeProvisionedIOPS, null);
    }

    public Period getWindowsRegisterTimeout() {
        return new Period().withMinutes(getIntOrDefault(this.windowsRegisterTimeout, 15));
    }

    public int getMinWindowsInstanceCount() {
        return getIntOrDefault(minWindowsInstanceCount, 0);
    }

    public int getMaxWindowsInstancesAllowed() {
        return getIntOrDefault(this.maxWindowsInstancesAllowed, 5);
    }

    public int getMaxWindowsSpotInstanceAllowed() {
        return getIntOrDefault(this.maxWindowsSpotInstanceAllowed, 10);
    }

    public String efsDnsOrIP() {
        return efsDnsOrIP;
    }

    public String efsMountLocation() {
        return "/efs";
    }

    public DockerRegistryAuthType getPrivateDockerRegistryAuthType() {
        return DockerRegistryAuthType.from(privateDockerRegistryAuthType);
    }

    public DockerRegistryAuthData getPrivateDockerRegistryAuthData() {
        final DockerRegistryAuthType authType = getPrivateDockerRegistryAuthType();
        if (authType == AUTH_TOKEN) {
            return new DockerRegistryAuthData(privateDockerRegistryUrl, privateDockerRegistryAuthToken, privateDockerRegistryEmail);
        } else if (authType == USERNAME_PASSWORD) {
            return new DockerRegistryAuthData(privateDockerRegistryUrl, privateDockerRegistryUsername, privateDockerRegistryPassword, privateDockerRegistryEmail);
        }
        return null;
    }

    public String getLinuxUserdataScript() {
        return linuxUserdataScript;
    }

    public String getWindowsUserdataScript() {
        return windowsUserdataScript;
    }

    public String getLinuxOSVolumeType() {
        return linuxOSVolumeType;
    }

    public String getLinuxOSVolumeSize() {
        return linuxOSVolumeSize;
    }

    public Integer getLinuxOSVolumeProvisionedIOPS() {
        return getIntOrDefault(linuxOSVolumeProvisionedIOPS, null);
    }

    public Period stopLinuxInstanceAfter() {
        return new Period().withMinutes(getIntOrDefault(stopLinuxInstanceAfter, 5));
    }

    public Period stopWindowsInstanceAfter() {
        return new Period().withMinutes(getIntOrDefault(stopWindowsInstanceAfter, 5));
    }

    public Period terminateStoppedLinuxInstanceAfter() {
        return new Period().withMinutes(getIntOrDefault(terminateStoppedLinuxInstanceAfter, 5));
    }

    public Period terminateStoppedWindowsInstanceAfter() {
        return new Period().withMinutes(getIntOrDefault(terminateStoppedWindowsInstanceAfter, 5));
    }

    public Period terminateIdleLinuxSpotInstanceAfter() {
        return new Period().withMinutes(getIntOrDefault(terminateIdleLinuxSpotInstanceAfter, 30));
    }

    public Period terminateIdleWindowsSpotInstanceAfter() {
        return new Period().withMinutes(getIntOrDefault(terminateIdleWindowsSpotInstanceAfter, 30));
    }

    public AmazonECS ecsClient() {
        return AmazonECSClientBuilder
                .standard()
                .withCredentials(credentials())
                .withRegion(getRegion())
                .withClientConfiguration(new ClientConfiguration().withRetryPolicy(retryPolicy()))
                .build();
    }

    public AmazonEC2 ec2Client() {
        return AmazonEC2ClientBuilder
                .standard()
                .withCredentials(credentials())
                .withRegion(getRegion())
                .withClientConfiguration(new ClientConfiguration().withRetryPolicy(retryPolicy()))
                .build();
    }

    public static PluginSettings fromJSON(String json) {
        return GSON.fromJson(json, PluginSettings.class);
    }

    private RetryPolicy retryPolicy() {
        return new RetryPolicy(new PredefinedRetryPolicies.SDKDefaultRetryCondition(),
                PredefinedRetryPolicies.DEFAULT_BACKOFF_STRATEGY,
                PredefinedRetryPolicies.DEFAULT_MAX_ERROR_RETRY,
                true);
    }

    private AWSCredentialsProvider credentials() {
        return new AWSCredentialsProviderChain().getAWSCredentialsProvider(accessKeyId, secretAccessKey);
    }

    private Map<String, String> getLogOptions() {
        Map<String, String> logOptionMap = new HashMap<>();
        splitIntoLinesAndTrimSpaces(logOptions).forEach(variable -> {
            if (StringUtils.contains(variable, "=")) {
                String[] pair = variable.split("=", 2);
                logOptionMap.put(pair[0], pair[1]);
            } else {
                LOG.warn(MessageFormat.format("Ignoring variable {0} as it is not in acceptable format, Variable must follow `VARIABLE_NAME=VARIABLE_VALUE` format.", variable));
            }
        });

        return logOptionMap;
    }

    public boolean isConfigured() {
        return StringUtils.isNotBlank(goServerUrl);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PluginSettings that = (PluginSettings) o;
        return Objects.equals(goServerUrl, that.goServerUrl) &&
                Objects.equals(clusterName, that.clusterName) &&
                Objects.equals(region, that.region) &&
                Objects.equals(accessKeyId, that.accessKeyId) &&
                Objects.equals(secretAccessKey, that.secretAccessKey) &&
                Objects.equals(environmentVariables, that.environmentVariables) &&
                Objects.equals(containerAutoregisterTimeout, that.containerAutoregisterTimeout) &&
                Objects.equals(maxContainerDataVolumeSize, that.maxContainerDataVolumeSize) &&
                Objects.equals(keyPairName, that.keyPairName) &&
                Objects.equals(iamInstanceProfile, that.iamInstanceProfile) &&
                Objects.equals(subnetIds, that.subnetIds) &&
                Objects.equals(securityGroupIds, that.securityGroupIds) &&
                Objects.equals(logDriverName, that.logDriverName) &&
                Objects.equals(logOptions, that.logOptions) &&
                Objects.equals(linuxAMI, that.linuxAMI) &&
                Objects.equals(linuxInstanceType, that.linuxInstanceType) &&
                Objects.equals(linuxRegisterTimeout, that.linuxRegisterTimeout) &&
                Objects.equals(minLinuxInstanceCount, that.minLinuxInstanceCount) &&
                Objects.equals(maxLinuxInstancesAllowed, that.maxLinuxInstancesAllowed) &&
                Objects.equals(maxLinuxSpotInstanceAllowed, that.maxLinuxSpotInstanceAllowed) &&
                Objects.equals(linuxVolumeType, that.linuxVolumeType) &&
                Objects.equals(linuxVolumeSize, that.linuxVolumeSize) &&
                Objects.equals(linuxVolumeProvisionedIOPS, that.linuxVolumeProvisionedIOPS) &&
                Objects.equals(linuxOSVolumeType, that.linuxOSVolumeType) &&
                Objects.equals(linuxOSVolumeSize, that.linuxOSVolumeSize) &&
                Objects.equals(linuxOSVolumeProvisionedIOPS, that.linuxOSVolumeProvisionedIOPS) &&
                Objects.equals(linuxUserdataScript, that.linuxUserdataScript) &&
                linuxStopPolicy == that.linuxStopPolicy &&
                Objects.equals(stopLinuxInstanceAfter, that.stopLinuxInstanceAfter) &&
                Objects.equals(terminateStoppedLinuxInstanceAfter, that.terminateStoppedLinuxInstanceAfter) &&
                Objects.equals(terminateIdleLinuxSpotInstanceAfter, that.terminateIdleLinuxSpotInstanceAfter) &&
                Objects.equals(windowsAMI, that.windowsAMI) &&
                Objects.equals(windowsInstanceType, that.windowsInstanceType) &&
                Objects.equals(windowsVolumeType, that.windowsVolumeType) &&
                Objects.equals(windowsVolumeSize, that.windowsVolumeSize) &&
                Objects.equals(windowsOSVolumeProvisionedIOPS, that.windowsOSVolumeProvisionedIOPS) &&
                Objects.equals(windowsRegisterTimeout, that.windowsRegisterTimeout) &&
                Objects.equals(minWindowsInstanceCount, that.minWindowsInstanceCount) &&
                Objects.equals(maxWindowsInstancesAllowed, that.maxWindowsInstancesAllowed) &&
                Objects.equals(maxWindowsSpotInstanceAllowed, that.maxWindowsSpotInstanceAllowed) &&
                Objects.equals(windowsUserdataScript, that.windowsUserdataScript) &&
                windowsStopPolicy == that.windowsStopPolicy &&
                Objects.equals(stopWindowsInstanceAfter, that.stopWindowsInstanceAfter) &&
                Objects.equals(terminateStoppedWindowsInstanceAfter, that.terminateStoppedWindowsInstanceAfter) &&
                Objects.equals(terminateIdleWindowsSpotInstanceAfter, that.terminateIdleWindowsSpotInstanceAfter) &&
                Objects.equals(privateDockerRegistryAuthType, that.privateDockerRegistryAuthType) &&
                Objects.equals(privateDockerRegistryAuthToken, that.privateDockerRegistryAuthToken) &&
                Objects.equals(privateDockerRegistryUrl, that.privateDockerRegistryUrl) &&
                Objects.equals(privateDockerRegistryEmail, that.privateDockerRegistryEmail) &&
                Objects.equals(privateDockerRegistryUsername, that.privateDockerRegistryUsername) &&
                Objects.equals(privateDockerRegistryPassword, that.privateDockerRegistryPassword) &&
                Objects.equals(efsDnsOrIP, that.efsDnsOrIP);
    }

    @Override
    public int hashCode() {
        return Objects.hash(goServerUrl, clusterName, region, accessKeyId, secretAccessKey, environmentVariables, containerAutoregisterTimeout, maxContainerDataVolumeSize, keyPairName, iamInstanceProfile, subnetIds, securityGroupIds, logDriverName, logOptions, linuxAMI, linuxInstanceType, linuxRegisterTimeout, minLinuxInstanceCount, maxLinuxInstancesAllowed, maxLinuxSpotInstanceAllowed, linuxVolumeType, linuxVolumeSize, linuxVolumeProvisionedIOPS, linuxOSVolumeType, linuxOSVolumeSize, linuxOSVolumeProvisionedIOPS, linuxUserdataScript, linuxStopPolicy, stopLinuxInstanceAfter, terminateStoppedLinuxInstanceAfter, terminateIdleLinuxSpotInstanceAfter, windowsAMI, windowsInstanceType, windowsVolumeType, windowsVolumeSize, windowsOSVolumeProvisionedIOPS, windowsRegisterTimeout, minWindowsInstanceCount, maxWindowsInstancesAllowed, maxWindowsSpotInstanceAllowed, windowsUserdataScript, windowsStopPolicy, stopWindowsInstanceAfter, terminateStoppedWindowsInstanceAfter, terminateIdleWindowsSpotInstanceAfter, privateDockerRegistryAuthType, privateDockerRegistryAuthToken, privateDockerRegistryUrl, privateDockerRegistryEmail, privateDockerRegistryUsername, privateDockerRegistryPassword, efsDnsOrIP);
    }

    public String uuid() {
        return Integer.toHexString(Objects.hash(this));
    }
}
