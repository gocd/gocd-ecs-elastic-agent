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

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.GSON;
import static org.assertj.core.api.Assertions.assertThat;

class ElasticAgentProfilePropertiesTest {

    @Test
    void shouldCreateValidMetadata() throws Exception {
        final String actualJSON = GSON.toJson(new MetadataExtractor().extract(ElasticAgentProfileProperties.class));

        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"Image\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"Command\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"Environment\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"MaxMemory\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"ReservedMemory\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"CPU\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": true,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"MountDockerSocket\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"Privileged\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"TaskRoleArn\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"AMI\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"InstanceType\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"SubnetIds\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"SecretName\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"SecretValue\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"ExecutionRoleArn\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"SecurityGroupIds\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"IAMInstanceProfile\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"Platform\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"BindMount\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"RunAsSpotInstance\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"SpotPrice\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"SpotRequestExpiresAfter\",\n" +
                "    \"metadata\": {\n" +
                "      \"required\": false,\n" +
                "      \"secure\": false\n" +
                "    }\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, actualJSON, true);
    }

    @Test
    void shouldParseJsonToElasticProfile() {
        String json = "{\n" +
                "  \"Image\": \"nginx\",\n" +
                "  \"Command\": \"ls\\n-al\",\n" +
                "  \"Environment\": \"JAVA_HOME=/java\\nANT_HOME=/ant\",\n" +
                "  \"MaxMemory\": \"200G\",\n" +
                "  \"ReservedMemory\": \"150M\",\n" +
                "  \"AMI\": \"ami-123456\",\n" +
                "  \"InstanceType\": \"t2.small\",\n" +
                "  \"SubnetIds\": \"subnet-abc045we\",\n" +
                "  \"SecretName\": \"secret-name\",\n" +
                "  \"SecretValue\": \"secret-value\",\n" +
                "  \"ExecutionRoleArn\": \"executionRoleArn\",\n" +
                "  \"SecurityGroupIds\": \"sg-ec33sl0,sg-ec33sl2,sg-ec33sl1\",\n" +
                "  \"EC2TerminateAfter\": \"240\",\n" +
                "  \"IAMInstanceProfile\": \"ecsInstanceRole\",\n" +
                "  \"Privileged\": \"true\",\n" +
                "  \"MountDockerSocket\": \"true\"\n" +
                "}";

        ElasticAgentProfileProperties elasticAgentProfileProperties = ElasticAgentProfileProperties.fromJson(json);

        assertThat(elasticAgentProfileProperties.getImage()).isEqualTo("nginx");
        assertThat(elasticAgentProfileProperties.getCommand()).contains("ls", "-al");
        assertThat(elasticAgentProfileProperties.getEnvironment()).contains("JAVA_HOME=/java", "ANT_HOME=/ant");


        assertThat(elasticAgentProfileProperties.getMaxMemory()).isEqualTo(204800);
        assertThat(elasticAgentProfileProperties.getReservedMemory()).isEqualTo(150);
        assertThat(elasticAgentProfileProperties.getAmiID()).isEqualTo("ami-123456");
        assertThat(elasticAgentProfileProperties.getInstanceType()).isEqualTo("t2.small");
        assertThat(elasticAgentProfileProperties.getSubnetIds()).contains("subnet-abc045we");
        assertThat(elasticAgentProfileProperties.getSecretName()).isEqualTo("secret-name");
        assertThat(elasticAgentProfileProperties.getSecretValue()).isEqualTo("secret-value");
        assertThat(elasticAgentProfileProperties.getExecutionRoleArn()).isEqualTo("executionRoleArn");
        assertThat(elasticAgentProfileProperties.getSecurityGroupIds()).contains("sg-ec33sl0", "sg-ec33sl2", "sg-ec33sl1");
        assertThat(elasticAgentProfileProperties.getEC2IamInstanceProfile()).isEqualTo("ecsInstanceRole");
        assertThat(elasticAgentProfileProperties.isPrivileged()).isEqualTo(true);
        assertThat(elasticAgentProfileProperties.isMountDockerSocket()).isEqualTo(true);
    }

    @Test
    void shouldParseJSONWithBindMount() {
        String json = "{\n" +
                "\"BindMount\": \"[\n" +
                "    {\n" +
                "      \\\"Name\\\": \\\"data\\\",\n" +
                "      \\\"SourcePath\\\": \\\"/ecs/data\\\",\n" +
                "      \\\"ContainerPath\\\": \\\"/var/data\\\"\n" +
                "    }\n" +
                "  ]\"\t\n" +
                "}";

        ElasticAgentProfileProperties elasticAgentProfileProperties = ElasticAgentProfileProperties.fromJson(json);

        assertThat(elasticAgentProfileProperties.bindMounts().size()).isEqualTo(1);

        BindMount bindMount = elasticAgentProfileProperties.bindMounts().get(0);
        assertThat(bindMount.getName()).isEqualTo("data");
        assertThat(bindMount.getContainerPath()).isEqualTo("/var/data");
        assertThat(bindMount.getSourcePath()).isEqualTo("/ecs/data");
    }

    @Test
    void bindMountShouldBeAnEmptyListIfDataNotProvided() {
        ElasticAgentProfileProperties elasticAgentProfileProperties = ElasticAgentProfileProperties.fromJson("{\"BindMount\": \"[]\"}");

        assertThat(elasticAgentProfileProperties.bindMounts()).isEmpty();

        elasticAgentProfileProperties = ElasticAgentProfileProperties.fromJson("{}");

        assertThat(elasticAgentProfileProperties.bindMounts()).isEmpty();
    }
}
