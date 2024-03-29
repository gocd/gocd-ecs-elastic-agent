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


import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

public class Capabilities {
    private static final Gson GSON = new GsonBuilder()
            .excludeFieldsWithoutExposeAnnotation()
            .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
            .create();

    @Expose
    @SerializedName("supports_plugin_status_report")
    private final boolean supportsPluginStatusReport;

    @Expose
    @SerializedName("supports_cluster_status_report")
    private final boolean supportsClusterStatusReport;

    @Expose
    @SerializedName("supports_agent_status_report")
    private final boolean supportsAgentStatusReport;


    public Capabilities(boolean supportsPluginStatusReport, boolean supportsClusterStatusReport, boolean supportsAgentStatusReport) {
        this.supportsPluginStatusReport = supportsPluginStatusReport;
        this.supportsClusterStatusReport = supportsClusterStatusReport;
        this.supportsAgentStatusReport = supportsAgentStatusReport;
    }

    public static Capabilities fromJSON(String json) {
        return GSON.fromJson(json, Capabilities.class);
    }

    public String toJSON() {
        return GSON.toJson(this);
    }
}
