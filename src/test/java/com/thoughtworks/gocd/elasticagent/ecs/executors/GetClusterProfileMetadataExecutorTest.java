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
import com.thoughtworks.gocd.elasticagent.ecs.domain.MetadataExtractor;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetClusterProfileMetadataExecutorTest {
    @Test
    void shouldSerializeAllFields() {
        GoPluginApiResponse response = new GetClusterProfileMetadataExecutor().execute();
        List<Map<String, Object>> list = new Gson().fromJson(response.responseBody(), new TypeToken<List<Map<String, Object>>>() {
        }.getType());

        assertThat(new MetadataExtractor().extract(PluginSettings.class)).hasSize(list.size());
    }

    @Test
    void assertJsonStructure() throws Exception {
        GoPluginApiResponse response = new GetClusterProfileMetadataExecutor().execute();

        assertThat(response.responseCode()).isEqualTo(200);
        String expectedJSON = """
                [
                  {
                    "key": "GoServerUrl",
                    "metadata": {
                      "required": true,
                      "secure": false
                    }
                  },
                  {
                    "key": "ClusterName",
                    "metadata": {
                      "required": true,
                      "secure": false
                    }
                  },
                  {
                    "key": "AWSRegion",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "AWSAccessKeyId",
                    "metadata": {
                      "required": false,
                      "secure": true
                    }
                  },
                  {
                    "key": "AWSSecretAccessKey",
                    "metadata": {
                      "required": false,
                      "secure": true
                    }
                  },
                  {
                    "key": "AWSAssumeRoleArn",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "EnvironmentVariables",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "ContainerAutoregisterTimeout",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "KeyPairName",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "IamInstanceProfile",
                    "metadata": {
                      "required": true,
                      "secure": false
                    }
                  },
                  {
                    "key": "SubnetIds",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "SecurityGroupIds",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LogDriver",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LogOptions",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxAmi",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxInstanceType",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxRegisterTimeout",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "MinLinuxInstanceCount",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "MaxLinuxInstancesAllowed",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "MaxLinuxSpotInstanceAllowed",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxDockerVolumeType",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxDockerVolumeSize",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxDockerVolumeProvisionedIOPS",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxOSVolumeType",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxOSVolumeSize",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxOSVolumeProvisionedIOPS",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxUserdataScript",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "LinuxStopPolicy",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "StopLinuxInstanceAfter",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "TerminateStoppedLinuxInstanceAfter",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "TerminateIdleLinuxSpotInstanceAfter",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "WindowsAmi",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "WindowsInstanceType",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "WindowsOSVolumeType",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "WindowsOSVolumeSize",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "WindowsOSVolumeProvisionedIOPS",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "WindowsRegisterTimeout",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "MinWindowsInstanceCount",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "MaxWindowsInstancesAllowed",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "MaxWindowsSpotInstanceAllowed",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "WindowsUserdataScript",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "WindowsStopPolicy",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "StopWindowsInstanceAfter",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "TerminateStoppedWindowsInstanceAfter",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "TerminateIdleWindowsSpotInstanceAfter",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "PrivateDockerRegistryAuthType",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "PrivateDockerRegistryAuthToken",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "PrivateDockerRegistryUrl",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "PrivateDockerRegistryEmail",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "PrivateDockerRegistryUsername",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "PrivateDockerRegistryPassword",
                    "metadata": {
                      "required": false,
                      "secure": true
                    }
                  },
                  {
                    "key": "EfsDnsOrIP",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  }
                ]
                """;

        JSONAssert.assertEquals(expectedJSON, response.responseBody(), true);
    }
}
