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

package com.thoughtworks.gocd.elasticagent.ecs.requests;

import com.thoughtworks.gocd.elasticagent.ecs.domain.*;
import org.json.JSONException;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class MigrateConfigurationRequestTest {
    @Test
    public void shouldCreateMigrationConfigRequestFromRequestBody() {
        String requestBody = "{" +
                "    \"plugin_settings\":{" +
                "        \"GoServerUrl\":\"https://your.server.url\" " +
                "    }," +
                "    \"cluster_profiles\":[" +
                "        {" +
                "            \"id\":\"cluster_profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"properties\":{" +
                "                \"GoServerUrl\":\"https://your.server.url\" " +
                "            }" +
                "         }" +
                "    ]," +
                "    \"elastic_agent_profiles\":[" +
                "        {" +
                "            \"id\":\"profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"cluster_profile_id\":\"cluster_profile_id\"," +
                "            \"properties\":{" +
                "                \"Image\":\"nginx\"" +
                "            }" +
                "        }" +
                "    ]" +
                "}\n";

        MigrateConfigurationRequest request = MigrateConfigurationRequest.fromJSON(requestBody);

        PluginSettings pluginSettings = new PluginSettingsBuilder()
                .addSetting("GoServerUrl", "https://your.server.url")
                .build();

        ClusterProfile clusterProfile = new ClusterProfile();
        clusterProfile.setId("cluster_profile_id");
        clusterProfile.setPluginId("plugin_id");
        clusterProfile.setClusterProfileProperties(pluginSettings);

        ElasticAgentProfile elasticAgentProfile = new ElasticAgentProfile();
        elasticAgentProfile.setId("profile_id");
        elasticAgentProfile.setPluginId("plugin_id");
        elasticAgentProfile.setClusterProfileId("cluster_profile_id");
        ElasticAgentProfileProperties elasticAgentProfileProperties = new ElasticAgentProfileProperties();
        elasticAgentProfileProperties.image = "nginx";
        elasticAgentProfile.setProperties(elasticAgentProfileProperties);

        assertThat(pluginSettings).isEqualTo(request.getPluginSettings());
        assertThat(Arrays.asList(clusterProfile)).isEqualTo(request.getClusterProfiles());
        assertThat(Arrays.asList(elasticAgentProfile)).isEqualTo(request.getElasticAgentProfiles());
    }

    @Test
    public void shouldCreateMigrationConfigRequestWhenNoConfigurationsAreSpecified() {
        String requestBody = "{" +
                "    \"plugin_settings\":{}," +
                "    \"cluster_profiles\":[]," +
                "    \"elastic_agent_profiles\":[]" +
                "}\n";

        MigrateConfigurationRequest request = MigrateConfigurationRequest.fromJSON(requestBody);

        assertThat(new PluginSettings()).isEqualTo(request.getPluginSettings());
        assertThat(Arrays.asList()).isEqualTo(request.getClusterProfiles());
        assertThat(Arrays.asList()).isEqualTo(request.getElasticAgentProfiles());
    }

    @Test
    public void shouldSerializeToJSONFromMigrationConfigRequest() throws JSONException {
        PluginSettings pluginSettings = new PluginSettingsBuilder()
                .addSetting("GoServerUrl", "https://your.server.url")
                .build();

        ClusterProfile clusterProfile = new ClusterProfile();
        clusterProfile.setId("cluster_profile_id");
        clusterProfile.setPluginId("plugin_id");
        clusterProfile.setClusterProfileProperties(pluginSettings);

        ElasticAgentProfile elasticAgentProfile = new ElasticAgentProfile();
        elasticAgentProfile.setId("profile_id");
        elasticAgentProfile.setPluginId("plugin_id");
        elasticAgentProfile.setClusterProfileId("cluster_profile_id");
        ElasticAgentProfileProperties elasticAgentProfileProperties = new ElasticAgentProfileProperties();
        elasticAgentProfileProperties.image = "nginx";
        elasticAgentProfile.setProperties(elasticAgentProfileProperties);

        MigrateConfigurationRequest request = new MigrateConfigurationRequest(pluginSettings, Arrays.asList(clusterProfile), Arrays.asList(elasticAgentProfile));

        String actual = request.toJSON();

        String expected = "{" +
                "    \"plugin_settings\":{" +
                "        \"GoServerUrl\":\"https://your.server.url\" " +
                "    }," +
                "    \"cluster_profiles\":[" +
                "        {" +
                "            \"id\":\"cluster_profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"properties\":{" +
                "                \"GoServerUrl\":\"https://your.server.url\" " +
                "            }" +
                "         }" +
                "    ]," +
                "    \"elastic_agent_profiles\":[" +
                "        {" +
                "            \"id\":\"profile_id\"," +
                "            \"plugin_id\":\"plugin_id\"," +
                "            \"cluster_profile_id\":\"cluster_profile_id\"," +
                "            \"properties\":{" +
                "                \"Image\":\"nginx\"" +
                "            }" +
                "        }" +
                "    ]" +
                "}\n";

        JSONAssert.assertEquals(expected, actual, false);
    }
}
