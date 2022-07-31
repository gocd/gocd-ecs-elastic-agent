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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.GSON;


@EqualsAndHashCode
public class ElasticAgentProfile {
    @Expose
    @SerializedName("id")
    private String id;

    @Expose
    @SerializedName("plugin_id")
    private String pluginId;

    @Expose
    @SerializedName("cluster_profile_id")
    private String clusterProfileId;

    @Expose
    @SerializedName("properties")
    private ElasticAgentProfileProperties properties;

    public static ElasticAgentProfile fromJSON(String json) {
        return GSON.fromJson(json, ElasticAgentProfile.class);
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public String getClusterProfileId() {
        return clusterProfileId;
    }

    public void setClusterProfileId(String clusterProfileId) {
        this.clusterProfileId = clusterProfileId;
    }

    public ElasticAgentProfileProperties getProperties() {
        return properties;
    }

    public void setProperties(ElasticAgentProfileProperties properties) {
        this.properties = properties;
    }
}
