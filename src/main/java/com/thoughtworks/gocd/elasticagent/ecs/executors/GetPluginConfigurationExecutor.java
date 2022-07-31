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

package com.thoughtworks.gocd.elasticagent.ecs.executors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.RequestExecutor;
import com.thoughtworks.gocd.elasticagent.ecs.fields.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.fields.Field.next;
import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.toMap;

public class GetPluginConfigurationExecutor implements RequestExecutor {
    private static final String GO_SERVER_URL = "GoServerUrl";
    public static final String AWS_ACCESS_KEY_ID = "AWSAccessKeyId";
    public static final String AWS_SECRET_ACCESS_KEY = "AWSSecretAccessKey";
    private static final String AWS_REGION = "AWSRegion";
    private static final String ENVIRONMENT_VARIABLES = "EnvironmentVariables";
    private static final String CLUSTER_NAME = "ClusterName";
    private static final String CONTAINER_AUTOREGISTER_TIMEOUT = "ContainerAutoregisterTimeout";
    private static final String CONTAINER_DATA_VOLUME_SIZE = "MaxContainerDataVolumeSize";
    private static final String KEY_PAIR_NAME = "KeyPairName";


    private static final String IAM_INSTANCE_PROFILE = "IamInstanceProfile";
    private static final String SUBNET_IDS = "SubnetIds";
    private static final String SECURITY_GROUP_IDS = "SecurityGroupIds";
    public static final String LINUX_AMI = "LinuxAmi";
    public static final String LINUX_INSTANCE_TYPE = "LinuxInstanceType";

    public static final String LINUX_OS_VOLUME_TYPE = "LinuxOSVolumeType";
    public static final String LINUX_OS_VOLUME_SIZE = "LinuxOSVolumeSize";
    public static final String LINUX_OS_VOLUME_PROVISIONED_IOPS = "LinuxOSVolumeProvisionedIOPS";
    public static final String LINUX_DOCKER_VOLUME_TYPE = "LinuxDockerVolumeType";
    public static final String LINUX_DOCKER_VOLUME_SIZE = "LinuxDockerVolumeSize";
    public static final String LINUX_DOCKER_VOLUME_PROVISIONED_IOPS = "LinuxDockerVolumeProvisionedIOPS";
    public static final String WINDOWS_OS_VOLUME_TYPE = "WindowsOSVolumeType";
    public static final String WINDOWS_OS_VOLUME_SIZE = "WindowsOSVolumeSize";
    public static final String WINDOWS_OS_VOLUME_PROVISIONED_IOPS = "WindowsOSVolumeProvisionedIOPS";
    public static final String MIN_LINUX_INSTANCE_COUNT = "MinLinuxInstanceCount";
    private static final String MAX_LINUX_INSTANCES_ALLOWED = "MaxLinuxInstancesAllowed";
    private static final String MAX_WINDOWS_INSTANCES_ALLOWED = "MaxWindowsInstancesAllowed";
    public static final String MIN_WINDOWS_INSTANCE_COUNT = "MinWindowsInstanceCount";
    public static final String WINDOWS_AMI = "WindowsAmi";
    public static final String WINDOWS_INSTANCE_TYPE = "WindowsInstanceType";
    public static final String PRIVATE_DOCKER_REGISTRY_AUTH_TYPE = "PrivateDockerRegistryAuthType";
    public static final String PRIVATE_DOCKER_REGISTRY_URL = "PrivateDockerRegistryUrl";
    public static final String PRIVATE_DOCKER_REGISTRY_EMAIL = "PrivateDockerRegistryEmail";
    public static final String PRIVATE_DOCKER_REGISTRY_USERNAME = "PrivateDockerRegistryUsername";
    public static final String PRIVATE_DOCKER_REGISTRY_PASSWORD = "PrivateDockerRegistryPassword";
    public static final String PRIVATE_DOCKER_REGISTRY_AUTH_TOKEN = "PrivateDockerRegistryAuthToken";
    public static final List<Field> FIELD_LIST = Arrays.asList(
            new NonBlankField(GO_SERVER_URL, "Go Server URL", false, next()),
            new Field(AWS_ACCESS_KEY_ID, "AWS Access Key ID", null, false, true, next()),
            new Field(AWS_SECRET_ACCESS_KEY, "AWS Secret Access Key", null, false, true, next()),
            new Field(AWS_REGION, "AWS Region", null, false, false, next()),
            new NonBlankField(CLUSTER_NAME, "AWS Cluster Name", false, next()),
            new Field(ENVIRONMENT_VARIABLES, "Environment Variables", null, false, false, next()),
            new PositiveNumberField(CONTAINER_AUTOREGISTER_TIMEOUT, "Container auto-register timeout (in minutes)", null, true, next()),
            new ContainerDataVolumeField(CONTAINER_DATA_VOLUME_SIZE, next()),

            // Common configurations for instance
            new Field(KEY_PAIR_NAME, "KeyPair name", null, false, false, next()),
            new NonBlankField(IAM_INSTANCE_PROFILE, "Iam instance profile", false, next()),
            new Field(SUBNET_IDS, "Subnet id(s)", null, false, false, next()),
            new Field(SECURITY_GROUP_IDS, "Security Group Id(s)", null, false, false, next()),

            //Linux configurations used for auto scaling
            new Field(LINUX_AMI, "Ami id", null, false, false, next()),
            new Field(LINUX_INSTANCE_TYPE, "Instance type", null, false, false, next()),
            new PositiveNumberField("LinuxRegisterTimeout", "Instance creation timeout (in minutes)", "5", false, next()),
            new IntegerRangeField(MIN_LINUX_INSTANCE_COUNT, "Minimum instance required in cluster", "0", false, next()),
            new IntegerRangeField(MAX_LINUX_INSTANCES_ALLOWED, "Maximum instances allowed", "5", false, next()),
            new Field(LINUX_OS_VOLUME_TYPE, "Additional volume for operating system", "none", false, false, next()),
            new IntegerRangeField(LINUX_OS_VOLUME_SIZE, "Volume size", "8", false, 8, next()),
            new IntegerRangeField(LINUX_OS_VOLUME_PROVISIONED_IOPS, "Provisioned IOPS", "400", false, 100, next()),
            new Field(LINUX_DOCKER_VOLUME_TYPE, "Additional volume", "none", false, false, next()),
            new IntegerRangeField(LINUX_DOCKER_VOLUME_SIZE, "Volume size", "22", false, 22, next()),
            new IntegerRangeField(LINUX_DOCKER_VOLUME_PROVISIONED_IOPS, "Provisioned IOPS", "400", false, 100, next()),
            new Field("LinuxStopPolicy", "Stop instance policy", "StopIdleInstance", false, false, next()),
            new IntegerRangeField("StopLinuxInstanceAfter", "Stop instance after(in minutes)", "10", false, next()),
            new IntegerRangeField("TerminateStoppedLinuxInstanceAfter", "Terminate stopped instance after(in minutes)", "5", false, next()),
            new Field("LinuxUserdataScript", "Userdata script", null, false, false, next()),

            //windows configurations used for auto scaling
            new Field(WINDOWS_AMI, "Ami id", null, false, false, next()),
            new Field(WINDOWS_INSTANCE_TYPE, "Instance type", null, false, false, next()),
            new Field(WINDOWS_OS_VOLUME_TYPE, "Additional volume", "none", false, false, next()),
            new IntegerRangeField(WINDOWS_OS_VOLUME_SIZE, "Volume size", "50", false, 50, next()),
            new IntegerRangeField(WINDOWS_OS_VOLUME_PROVISIONED_IOPS, "Provisioned IOPS", "400", false, 100, next()),
            new PositiveNumberField("WindowsRegisterTimeout", "Instance creation timeout (in minutes)", "15", false, next()),
            new IntegerRangeField(MIN_WINDOWS_INSTANCE_COUNT, "Minimum instances required in cluster", "0", false, next()),
            new IntegerRangeField(MAX_WINDOWS_INSTANCES_ALLOWED, "Maximum instances allowed", "5", false, next()),
            new Field("WindowsStopPolicy", "Stop instance policy", "StopIdleInstance", false, false, next()),
            new IntegerRangeField("StopWindowsInstanceAfter", "Stop instance after(in minutes)", "10", false, next()),
            new IntegerRangeField("TerminateStoppedWindowsInstanceAfter", "Terminate stopped instance after(in minutes)", "5", false, next()),
            new Field("WindowsUserdataScript", "Userdata script", null, false, false, next()),

            new Field(PRIVATE_DOCKER_REGISTRY_AUTH_TYPE, "Default Docker Registry", null, false, false, next()),
            new Field(PRIVATE_DOCKER_REGISTRY_URL, "Private docker registry url", null, false, false, next()),
            new Field(PRIVATE_DOCKER_REGISTRY_EMAIL, "Email", null, false, false, next()),
            new Field(PRIVATE_DOCKER_REGISTRY_USERNAME, "Docker registry username", null, false, false, next()),
            new Field(PRIVATE_DOCKER_REGISTRY_PASSWORD, "Docker registry password", null, false, true, next()),
            new Field(PRIVATE_DOCKER_REGISTRY_AUTH_TOKEN, "Docker registry auth token", null, false, false, next()),

            new LogDriverNameField(next()),
            new Field("LogOptions", "Log options", null, false, false, next()),
            new Field("EfsDnsOrIP", "Additional volume", "none", false, false, next())
    );

    public static final Map<String, Field> FIELDS_MAP = toMap(FIELD_LIST, Field::key, self -> self);
    private static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public GoPluginApiResponse execute() {
        return new DefaultGoPluginApiResponse(200, GSON.toJson(FIELDS_MAP));
    }

}
