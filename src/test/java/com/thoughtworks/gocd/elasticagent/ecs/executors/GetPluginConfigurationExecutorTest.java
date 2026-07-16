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
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Map;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

class GetPluginConfigurationExecutorTest {

    @Test
    void shouldSerializeAllFields() {
        GoPluginApiResponse response = new GetPluginConfigurationExecutor().execute();
        Map<String, Map<String, String>> hashMap = new Gson().fromJson(response.responseBody(), new TypeToken<Map<String, Map<String, String>>>() {
        }.getType());
        assertThat(hashMap).describedAs("Are you using anonymous inner classes — see https://github.com/google/gson/issues/298").hasSize(GetPluginConfigurationExecutor.FIELDS_MAP.size());
    }

    @Test
    void assertJsonStructure() throws Exception {
        GoPluginApiResponse response = new GetPluginConfigurationExecutor().execute();

        assertThat(response.responseCode()).isEqualTo(200);

        int expectedFieldNumber = 48;
        String expectedJSON = """
                {
                  "GoServerUrl": {
                    "display-name": "Go Server URL",
                    "required": true,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "AWSAccessKeyId": {
                    "display-name": "AWS Access Key ID",
                    "required": false,
                    "secure": true,
                    "display-order": "%d"
                  },
                  "AWSSecretAccessKey": {
                    "display-name": "AWS Secret Access Key",
                    "required": false,
                    "secure": true,
                    "display-order": "%d"
                  },
                  "AWSAssumeRoleArn": {
                    "display-name": "AWS Assume Role ARN",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "AWSRegion": {
                    "display-name": "AWS Region",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "ClusterName": {
                    "display-name": "AWS Cluster Name",
                    "required": true,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "EnvironmentVariables": {
                    "display-name": "Environment Variables",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "ContainerAutoregisterTimeout": {
                    "display-name": "Container auto-register timeout (in minutes)",
                    "required": true,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "KeyPairName": {
                    "display-name": "KeyPair name",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "IamInstanceProfile": {
                    "display-name": "Agent IAM instance profile",
                    "required": true,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "SubnetIds": {
                    "display-name": "Subnet id(s)",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "SecurityGroupIds": {
                    "display-name": "Security Group Id(s)",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxAmi": {
                    "display-name": "Ami id",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxInstanceType": {
                    "display-name": "Instance type",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxRegisterTimeout": {
                    "display-name": "Instance creation timeout (in minutes)",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "MinLinuxInstanceCount": {
                    "display-name": "Minimum instances required in cluster",
                    "default-value": "0",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "MaxLinuxInstancesAllowed": {
                    "display-name": "Maximum instances allowed",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxOSVolumeType": {
                    "display-name": "Additional volume for operating system",
                    "default-value": "none",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxOSVolumeSize": {
                    "display-name": "Volume size",
                    "default-value": "8",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxOSVolumeProvisionedIOPS": {
                    "display-name": "Provisioned IOPS",
                    "default-value": "400",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxDockerVolumeType": {
                    "display-name": "Additional volume",
                    "default-value": "none",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxDockerVolumeSize": {
                    "display-name": "Volume size",
                    "default-value": "22",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxDockerVolumeProvisionedIOPS": {
                    "display-name": "Provisioned IOPS",
                    "default-value": "400",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxStopPolicy": {
                    "display-name": "Stop instance policy",
                    "default-value": "StopIdleInstance",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "StopLinuxInstanceAfter": {
                    "display-name": "Stop instance after (in minutes)",
                    "default-value": "10",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "TerminateStoppedLinuxInstanceAfter": {
                    "display-name": "Terminate stopped instance after (in minutes)",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LinuxUserdataScript": {
                    "display-name": "Userdata script",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "WindowsAmi": {
                    "display-name": "Ami id",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "WindowsInstanceType": {
                    "display-name": "Instance type",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "WindowsOSVolumeType": {
                    "display-name": "Additional volume",
                    "default-value": "none",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "WindowsOSVolumeSize": {
                    "display-name": "Volume size",
                    "default-value": "50",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "WindowsOSVolumeProvisionedIOPS": {
                    "display-name": "Provisioned IOPS",
                    "default-value": "400",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "WindowsRegisterTimeout": {
                    "display-name": "Instance creation timeout (in minutes)",
                    "default-value": "15",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "MinWindowsInstanceCount": {
                    "display-name": "Minimum instances required in cluster",
                    "default-value": "0",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "MaxWindowsInstancesAllowed": {
                    "display-name": "Maximum instances allowed",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "WindowsStopPolicy": {
                    "display-name": "Stop instance policy",
                    "default-value": "StopIdleInstance",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "StopWindowsInstanceAfter": {
                    "display-name": "Stop instance after (in minutes)",
                    "default-value": "10",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "TerminateStoppedWindowsInstanceAfter": {
                    "display-name": "Terminate stopped instance after (in minutes)",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "WindowsUserdataScript": {
                    "display-name": "Userdata script",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "PrivateDockerRegistryAuthType": {
                    "display-name": "Default Docker Registry",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "PrivateDockerRegistryUrl": {
                    "display-name": "Private docker registry url",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "PrivateDockerRegistryEmail": {
                    "display-name": "Email",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "PrivateDockerRegistryUsername": {
                    "display-name": "Docker registry username",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "PrivateDockerRegistryPassword": {
                    "display-name": "Docker registry password",
                    "required": false,
                    "secure": true,
                    "display-order": "%d"
                  },
                  "PrivateDockerRegistryAuthToken": {
                    "display-name": "Docker registry auth token",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LogDriver": {
                    "display-name": "Log driver name",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "LogOptions": {
                    "display-name": "Log options",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  },
                  "EfsDnsOrIP": {
                    "display-name": "Additional volume",
                    "default-value": "none",
                    "required": false,
                    "secure": false,
                    "display-order": "%d"
                  }
                }""".formatted(IntStream.range(0, expectedFieldNumber).boxed().toArray());

        JSONAssert.assertEquals(expectedJSON, response.responseBody(), JSONCompareMode.STRICT);
    }
}
