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

package com.thoughtworks.gocd.elasticagent.ecs.requests;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfile;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfile;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.executors.MigrateConfigurationRequestExecutor;
import lombok.EqualsAndHashCode;

import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.GSON;

@EqualsAndHashCode
public class MigrateConfigurationRequest {
    @Expose
    @SerializedName("plugin_settings")
    private PluginSettings pluginSettings;

    @Expose
    @SerializedName("cluster_profiles")
    private List<ClusterProfile> clusterProfiles;

    @Expose
    @SerializedName("elastic_agent_profiles")
    private List<ElasticAgentProfile> elasticAgentProfiles;

    public MigrateConfigurationRequest() {
    }

    public MigrateConfigurationRequest(PluginSettings pluginSettings, List<ClusterProfile> clusterProfiles, List<ElasticAgentProfile> elasticAgentProfiles) {
        this.pluginSettings = pluginSettings;
        this.clusterProfiles = clusterProfiles;
        this.elasticAgentProfiles = elasticAgentProfiles;
    }

    public static MigrateConfigurationRequest fromJSON(String requestBody) {
        return GSON.fromJson(requestBody, MigrateConfigurationRequest.class);
    }

    public String toJSON() {
        return GSON.toJson(this);
    }

    public MigrateConfigurationRequestExecutor executor() {
        return new MigrateConfigurationRequestExecutor(this);
    }

    public PluginSettings getPluginSettings() {
        return pluginSettings;
    }

    public void setPluginSettings(PluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
    }

    public List<ClusterProfile> getClusterProfiles() {
        return clusterProfiles;
    }

    public void setClusterProfiles(List<ClusterProfile> clusterProfiles) {
        this.clusterProfiles = clusterProfiles;
    }

    public List<ElasticAgentProfile> getElasticAgentProfiles() {
        return elasticAgentProfiles;
    }

    public void setElasticAgentProfiles(List<ElasticAgentProfile> elasticAgentProfiles) {
        this.elasticAgentProfiles = elasticAgentProfiles;
    }
}
