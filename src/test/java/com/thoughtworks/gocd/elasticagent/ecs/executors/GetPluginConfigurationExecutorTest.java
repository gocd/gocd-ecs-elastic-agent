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

        String expectedJSON = "{\n" +
                "  \"GoServerUrl\": {\n" +
                "    \"display-name\": \"Go Server URL\",\n" +
                "    \"required\": true,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"0\"\n" +
                "  },\n" +
                "  \"AWSAccessKeyId\": {\n" +
                "    \"display-name\": \"AWS Access Key ID\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": true,\n" +
                "    \"display-order\": \"1\"\n" +
                "  },\n" +
                "  \"AWSSecretAccessKey\": {\n" +
                "    \"display-name\": \"AWS Secret Access Key\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": true,\n" +
                "    \"display-order\": \"2\"\n" +
                "  },\n" +
                "  \"AWSRegion\": {\n" +
                "    \"display-name\": \"AWS Region\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"3\"\n" +
                "  },\n" +
                "  \"ClusterName\": {\n" +
                "    \"display-name\": \"AWS Cluster Name\",\n" +
                "    \"required\": true,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"4\"\n" +
                "  },\n" +
                "  \"EnvironmentVariables\": {\n" +
                "    \"display-name\": \"Environment Variables\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"5\"\n" +
                "  },\n" +
                "  \"ContainerAutoregisterTimeout\": {\n" +
                "    \"display-name\": \"Container auto-register timeout (in minutes)\",\n" +
                "    \"required\": true,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"6\"\n" +
                "  },\n" +
                "  \"MaxContainerDataVolumeSize\": {\n" +
                "    \"display-name\": \"Container data volume size\",\n" +
                "    \"default-value\": \"10\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"7\"\n" +
                "  },\n" +
                "  \"KeyPairName\": {\n" +
                "    \"display-name\": \"KeyPair name\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"8\"\n" +
                "  },\n" +
                "  \"IamInstanceProfile\": {\n" +
                "    \"display-name\": \"Iam instance profile\",\n" +
                "    \"required\": true,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"9\"\n" +
                "  },\n" +
                "  \"SubnetIds\": {\n" +
                "    \"display-name\": \"Subnet id(s)\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"10\"\n" +
                "  },\n" +
                "  \"SecurityGroupIds\": {\n" +
                "    \"display-name\": \"Security Group Id(s)\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"11\"\n" +
                "  },\n" +
                "  \"LinuxAmi\": {\n" +
                "    \"display-name\": \"Ami id\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"12\"\n" +
                "  },\n" +
                "  \"LinuxInstanceType\": {\n" +
                "    \"display-name\": \"Instance type\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"13\"\n" +
                "  },\n" +
                "  \"LinuxRegisterTimeout\": {\n" +
                "    \"display-name\": \"Instance creation timeout (in minutes)\",\n" +
                "    \"default-value\": \"5\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"14\"\n" +
                "  },\n" +
                "  \"MinLinuxInstanceCount\": {\n" +
                "    \"display-name\": \"Minimum instance required in cluster\",\n" +
                "    \"default-value\": \"0\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"15\"\n" +
                "  },\n" +
                "  \"MaxLinuxInstancesAllowed\": {\n" +
                "    \"display-name\": \"Maximum instances allowed\",\n" +
                "    \"default-value\": \"5\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"16\"\n" +
                "  },\n" +
                "  \"LinuxOSVolumeType\": {\n" +
                "    \"display-name\": \"Additional volume for operating system\",\n" +
                "    \"default-value\": \"none\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"17\"\n" +
                "  },\n" +
                "  \"LinuxOSVolumeSize\": {\n" +
                "    \"display-name\": \"Volume size\",\n" +
                "    \"default-value\": \"8\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"18\"\n" +
                "  },\n" +
                "  \"LinuxOSVolumeProvisionedIOPS\": {\n" +
                "    \"display-name\": \"Provisioned IOPS\",\n" +
                "    \"default-value\": \"400\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"19\"\n" +
                "  },\n" +
                "  \"LinuxDockerVolumeType\": {\n" +
                "    \"display-name\": \"Additional volume\",\n" +
                "    \"default-value\": \"none\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"20\"\n" +
                "  },\n" +
                "  \"LinuxDockerVolumeSize\": {\n" +
                "    \"display-name\": \"Volume size\",\n" +
                "    \"default-value\": \"22\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"21\"\n" +
                "  },\n" +
                "  \"LinuxDockerVolumeProvisionedIOPS\": {\n" +
                "    \"display-name\": \"Provisioned IOPS\",\n" +
                "    \"default-value\": \"400\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"22\"\n" +
                "  },\n" +
                "  \"LinuxStopPolicy\": {\n" +
                "    \"display-name\": \"Stop instance policy\",\n" +
                "    \"default-value\": \"StopIdleInstance\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"23\"\n" +
                "  },\n" +
                "  \"StopLinuxInstanceAfter\": {\n" +
                "    \"display-name\": \"Stop instance after(in minutes)\",\n" +
                "    \"default-value\": \"10\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"24\"\n" +
                "  },\n" +
                "  \"TerminateStoppedLinuxInstanceAfter\": {\n" +
                "    \"display-name\": \"Terminate stopped instance after(in minutes)\",\n" +
                "    \"default-value\": \"5\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"25\"\n" +
                "  },\n" +
                "  \"LinuxUserdataScript\": {\n" +
                "    \"display-name\": \"Userdata script\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"26\"\n" +
                "  },\n" +
                "  \"WindowsAmi\": {\n" +
                "    \"display-name\": \"Ami id\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"27\"\n" +
                "  },\n" +
                "  \"WindowsInstanceType\": {\n" +
                "    \"display-name\": \"Instance type\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"28\"\n" +
                "  },\n" +
                "  \"WindowsOSVolumeType\": {\n" +
                "    \"display-name\": \"Additional volume\",\n" +
                "    \"default-value\": \"none\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"29\"\n" +
                "  },\n" +
                "  \"WindowsOSVolumeSize\": {\n" +
                "    \"display-name\": \"Volume size\",\n" +
                "    \"default-value\": \"50\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"30\"\n" +
                "  },\n" +
                "  \"WindowsOSVolumeProvisionedIOPS\": {\n" +
                "    \"display-name\": \"Provisioned IOPS\",\n" +
                "    \"default-value\": \"400\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"31\"\n" +
                "  },\n" +
                "  \"WindowsRegisterTimeout\": {\n" +
                "    \"display-name\": \"Instance creation timeout (in minutes)\",\n" +
                "    \"default-value\": \"15\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"32\"\n" +
                "  },\n" +
                "  \"MinWindowsInstanceCount\": {\n" +
                "    \"display-name\": \"Minimum instances required in cluster\",\n" +
                "    \"default-value\": \"0\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"33\"\n" +
                "  },\n" +
                "  \"MaxWindowsInstancesAllowed\": {\n" +
                "    \"display-name\": \"Maximum instances allowed\",\n" +
                "    \"default-value\": \"5\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"34\"\n" +
                "  },\n" +
                "  \"WindowsStopPolicy\": {\n" +
                "    \"display-name\": \"Stop instance policy\",\n" +
                "    \"default-value\": \"StopIdleInstance\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"35\"\n" +
                "  },\n" +
                "  \"StopWindowsInstanceAfter\": {\n" +
                "    \"display-name\": \"Stop instance after(in minutes)\",\n" +
                "    \"default-value\": \"10\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"36\"\n" +
                "  },\n" +
                "  \"TerminateStoppedWindowsInstanceAfter\": {\n" +
                "    \"display-name\": \"Terminate stopped instance after(in minutes)\",\n" +
                "    \"default-value\": \"5\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"37\"\n" +
                "  },\n" +
                "  \"WindowsUserdataScript\": {\n" +
                "    \"display-name\": \"Userdata script\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"38\"\n" +
                "  },\n" +
                "  \"PrivateDockerRegistryAuthType\": {\n" +
                "    \"display-name\": \"Default Docker Registry\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"39\"\n" +
                "  },\n" +
                "  \"PrivateDockerRegistryUrl\": {\n" +
                "    \"display-name\": \"Private docker registry url\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"40\"\n" +
                "  },\n" +
                "  \"PrivateDockerRegistryEmail\": {\n" +
                "    \"display-name\": \"Email\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"41\"\n" +
                "  },\n" +
                "  \"PrivateDockerRegistryUsername\": {\n" +
                "    \"display-name\": \"Docker registry username\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"42\"\n" +
                "  },\n" +
                "  \"PrivateDockerRegistryPassword\": {\n" +
                "    \"display-name\": \"Docker registry password\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": true,\n" +
                "    \"display-order\": \"43\"\n" +
                "  },\n" +
                "  \"PrivateDockerRegistryAuthToken\": {\n" +
                "    \"display-name\": \"Docker registry auth token\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"44\"\n" +
                "  },\n" +
                "  \"LogDriver\": {\n" +
                "    \"display-name\": \"Log driver name\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"45\"\n" +
                "  },\n" +
                "  \"LogOptions\": {\n" +
                "    \"display-name\": \"Log options\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"46\"\n" +
                "  },\n" +
                "  \"EfsDnsOrIP\": {\n" +
                "    \"display-name\": \"Additional volume\",\n" +
                "    \"default-value\": \"none\",\n" +
                "    \"required\": false,\n" +
                "    \"secure\": false,\n" +
                "    \"display-order\": \"47\"\n" +
                "  }\n" +
                "}";

        JSONAssert.assertEquals(expectedJSON, response.responseBody(), true);
    }
}
