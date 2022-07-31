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
import com.google.gson.reflect.TypeToken;
import lombok.EqualsAndHashCode;

import java.lang.reflect.Type;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.GSON;

@EqualsAndHashCode
public class ClusterProfile {
    @Expose
    @SerializedName("id")
    private String id;

    @Expose
    @SerializedName("plugin_id")
    private String pluginId;

    @Expose
    @SerializedName("properties")
    private ClusterProfileProperties clusterProfileProperties;


    public ClusterProfile() {
    }

    public ClusterProfile(String id, String pluginId, PluginSettings pluginSettings) {
        this.id = id;
        this.pluginId = pluginId;
        setClusterProfileProperties(pluginSettings);
    }

    public static ClusterProfile fromJSON(String json) {
        return GSON.fromJson(json, ClusterProfile.class);
    }

    public String getId() {
        return id;
    }

    public String getPluginId() {
        return pluginId;
    }

    public ClusterProfileProperties getClusterProfileProperties() {
        return clusterProfileProperties;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setPluginId(String pluginId) {
        this.pluginId = pluginId;
    }

    public void setClusterProfileProperties(ClusterProfileProperties clusterProfileProperties) {
        this.clusterProfileProperties = clusterProfileProperties;
    }

    public void setClusterProfileProperties(PluginSettings pluginSettings) {
        final Type type = new TypeToken<Map<String, String>>() {
        }.getType();
        this.clusterProfileProperties = ClusterProfileProperties.fromConfiguration(GSON.fromJson(GSON.toJson(pluginSettings), type));
    }
}
