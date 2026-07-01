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

import java.util.Map;

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

        String expectedJSON = """
                {
                  "GoServerUrl": {
                    "display-name": "Go Server URL",
                    "required": true,
                    "secure": false,
                    "display-order": "0"
                  },
                  "AWSAccessKeyId": {
                    "display-name": "AWS Access Key ID",
                    "required": false,
                    "secure": true,
                    "display-order": "1"
                  },
                  "AWSSecretAccessKey": {
                    "display-name": "AWS Secret Access Key",
                    "required": false,
                    "secure": true,
                    "display-order": "2"
                  },
                  "AWSRegion": {
                    "display-name": "AWS Region",
                    "required": false,
                    "secure": false,
                    "display-order": "3"
                  },
                  "ClusterName": {
                    "display-name": "AWS Cluster Name",
                    "required": true,
                    "secure": false,
                    "display-order": "4"
                  },
                  "EnvironmentVariables": {
                    "display-name": "Environment Variables",
                    "required": false,
                    "secure": false,
                    "display-order": "5"
                  },
                  "ContainerAutoregisterTimeout": {
                    "display-name": "Container auto-register timeout (in minutes)",
                    "required": true,
                    "secure": false,
                    "display-order": "6"
                  },
                  "MaxContainerDataVolumeSize": {
                    "display-name": "Container data volume size",
                    "default-value": "10",
                    "required": false,
                    "secure": false,
                    "display-order": "7"
                  },
                  "KeyPairName": {
                    "display-name": "KeyPair name",
                    "required": false,
                    "secure": false,
                    "display-order": "8"
                  },
                  "IamInstanceProfile": {
                    "display-name": "Iam instance profile",
                    "required": true,
                    "secure": false,
                    "display-order": "9"
                  },
                  "SubnetIds": {
                    "display-name": "Subnet id(s)",
                    "required": false,
                    "secure": false,
                    "display-order": "10"
                  },
                  "SecurityGroupIds": {
                    "display-name": "Security Group Id(s)",
                    "required": false,
                    "secure": false,
                    "display-order": "11"
                  },
                  "LinuxAmi": {
                    "display-name": "Ami id",
                    "required": false,
                    "secure": false,
                    "display-order": "12"
                  },
                  "LinuxInstanceType": {
                    "display-name": "Instance type",
                    "required": false,
                    "secure": false,
                    "display-order": "13"
                  },
                  "LinuxRegisterTimeout": {
                    "display-name": "Instance creation timeout (in minutes)",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "14"
                  },
                  "MinLinuxInstanceCount": {
                    "display-name": "Minimum instance required in cluster",
                    "default-value": "0",
                    "required": false,
                    "secure": false,
                    "display-order": "15"
                  },
                  "MaxLinuxInstancesAllowed": {
                    "display-name": "Maximum instances allowed",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "16"
                  },
                  "LinuxOSVolumeType": {
                    "display-name": "Additional volume for operating system",
                    "default-value": "none",
                    "required": false,
                    "secure": false,
                    "display-order": "17"
                  },
                  "LinuxOSVolumeSize": {
                    "display-name": "Volume size",
                    "default-value": "8",
                    "required": false,
                    "secure": false,
                    "display-order": "18"
                  },
                  "LinuxOSVolumeProvisionedIOPS": {
                    "display-name": "Provisioned IOPS",
                    "default-value": "400",
                    "required": false,
                    "secure": false,
                    "display-order": "19"
                  },
                  "LinuxDockerVolumeType": {
                    "display-name": "Additional volume",
                    "default-value": "none",
                    "required": false,
                    "secure": false,
                    "display-order": "20"
                  },
                  "LinuxDockerVolumeSize": {
                    "display-name": "Volume size",
                    "default-value": "22",
                    "required": false,
                    "secure": false,
                    "display-order": "21"
                  },
                  "LinuxDockerVolumeProvisionedIOPS": {
                    "display-name": "Provisioned IOPS",
                    "default-value": "400",
                    "required": false,
                    "secure": false,
                    "display-order": "22"
                  },
                  "LinuxStopPolicy": {
                    "display-name": "Stop instance policy",
                    "default-value": "StopIdleInstance",
                    "required": false,
                    "secure": false,
                    "display-order": "23"
                  },
                  "StopLinuxInstanceAfter": {
                    "display-name": "Stop instance after(in minutes)",
                    "default-value": "10",
                    "required": false,
                    "secure": false,
                    "display-order": "24"
                  },
                  "TerminateStoppedLinuxInstanceAfter": {
                    "display-name": "Terminate stopped instance after(in minutes)",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "25"
                  },
                  "LinuxUserdataScript": {
                    "display-name": "Userdata script",
                    "required": false,
                    "secure": false,
                    "display-order": "26"
                  },
                  "WindowsAmi": {
                    "display-name": "Ami id",
                    "required": false,
                    "secure": false,
                    "display-order": "27"
                  },
                  "WindowsInstanceType": {
                    "display-name": "Instance type",
                    "required": false,
                    "secure": false,
                    "display-order": "28"
                  },
                  "WindowsOSVolumeType": {
                    "display-name": "Additional volume",
                    "default-value": "none",
                    "required": false,
                    "secure": false,
                    "display-order": "29"
                  },
                  "WindowsOSVolumeSize": {
                    "display-name": "Volume size",
                    "default-value": "50",
                    "required": false,
                    "secure": false,
                    "display-order": "30"
                  },
                  "WindowsOSVolumeProvisionedIOPS": {
                    "display-name": "Provisioned IOPS",
                    "default-value": "400",
                    "required": false,
                    "secure": false,
                    "display-order": "31"
                  },
                  "WindowsRegisterTimeout": {
                    "display-name": "Instance creation timeout (in minutes)",
                    "default-value": "15",
                    "required": false,
                    "secure": false,
                    "display-order": "32"
                  },
                  "MinWindowsInstanceCount": {
                    "display-name": "Minimum instances required in cluster",
                    "default-value": "0",
                    "required": false,
                    "secure": false,
                    "display-order": "33"
                  },
                  "MaxWindowsInstancesAllowed": {
                    "display-name": "Maximum instances allowed",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "34"
                  },
                  "WindowsStopPolicy": {
                    "display-name": "Stop instance policy",
                    "default-value": "StopIdleInstance",
                    "required": false,
                    "secure": false,
                    "display-order": "35"
                  },
                  "StopWindowsInstanceAfter": {
                    "display-name": "Stop instance after(in minutes)",
                    "default-value": "10",
                    "required": false,
                    "secure": false,
                    "display-order": "36"
                  },
                  "TerminateStoppedWindowsInstanceAfter": {
                    "display-name": "Terminate stopped instance after(in minutes)",
                    "default-value": "5",
                    "required": false,
                    "secure": false,
                    "display-order": "37"
                  },
                  "WindowsUserdataScript": {
                    "display-name": "Userdata script",
                    "required": false,
                    "secure": false,
                    "display-order": "38"
                  },
                  "PrivateDockerRegistryAuthType": {
                    "display-name": "Default Docker Registry",
                    "required": false,
                    "secure": false,
                    "display-order": "39"
                  },
                  "PrivateDockerRegistryUrl": {
                    "display-name": "Private docker registry url",
                    "required": false,
                    "secure": false,
                    "display-order": "40"
                  },
                  "PrivateDockerRegistryEmail": {
                    "display-name": "Email",
                    "required": false,
                    "secure": false,
                    "display-order": "41"
                  },
                  "PrivateDockerRegistryUsername": {
                    "display-name": "Docker registry username",
                    "required": false,
                    "secure": false,
                    "display-order": "42"
                  },
                  "PrivateDockerRegistryPassword": {
                    "display-name": "Docker registry password",
                    "required": false,
                    "secure": true,
                    "display-order": "43"
                  },
                  "PrivateDockerRegistryAuthToken": {
                    "display-name": "Docker registry auth token",
                    "required": false,
                    "secure": false,
                    "display-order": "44"
                  },
                  "LogDriver": {
                    "display-name": "Log driver name",
                    "required": false,
                    "secure": false,
                    "display-order": "45"
                  },
                  "LogOptions": {
                    "display-name": "Log options",
                    "required": false,
                    "secure": false,
                    "display-order": "46"
                  },
                  "EfsDnsOrIP": {
                    "display-name": "Additional volume",
                    "default-value": "none",
                    "required": false,
                    "secure": false,
                    "display-order": "47"
                  }
                }""";

        JSONAssert.assertEquals(expectedJSON, response.responseBody(), true);
    }
}
