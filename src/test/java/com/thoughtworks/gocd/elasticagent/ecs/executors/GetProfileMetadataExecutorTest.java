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
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.MetadataExtractor;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetProfileMetadataExecutorTest {

    @Test
    void shouldSerializeAllFields() {
        GoPluginApiResponse response = new GetProfileMetadataExecutor().execute();
        List<Map<String, Object>> list = new Gson().fromJson(response.responseBody(), new TypeToken<List<Map<String, Object>>>() {
        }.getType());

        assertThat(new MetadataExtractor().extract(ElasticAgentProfileProperties.class)).hasSize(list.size());
    }

    @Test
    void assertJsonStructure() throws Exception {
        GoPluginApiResponse response = new GetProfileMetadataExecutor().execute();

        assertThat(response.responseCode()).isEqualTo(200);
        String expectedJSON = """
                [
                  {
                    "key": "Image",
                    "metadata": {
                      "required": true,
                      "secure": false
                    }
                  },
                  {
                    "key": "Command",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "Environment",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "MaxMemory",
                    "metadata": {
                      "required": true,
                      "secure": false
                    }
                  },
                  {
                    "key": "ReservedMemory",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "CPU",
                    "metadata": {
                      "required": true,
                      "secure": false
                    }
                  },
                  {
                    "key": "MountDockerSocket",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "Privileged",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "TaskRoleArn",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "AMI",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "InstanceType",
                    "metadata": {
                      "required": false,
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
                    "key": "IAMInstanceProfile",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "Platform",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "BindMount",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "RunAsSpotInstance",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "SpotPrice",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  },
                  {
                    "key": "SpotRequestExpiresAfter",
                    "metadata": {
                      "required": false,
                      "secure": false
                    }
                  }
                ]""";

        JSONAssert.assertEquals(expectedJSON, response.responseBody(), true);
    }
}
