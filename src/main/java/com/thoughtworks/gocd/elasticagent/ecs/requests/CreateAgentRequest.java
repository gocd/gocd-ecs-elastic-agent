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

import com.amazonaws.services.ecs.model.KeyValuePair;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.gocd.elasticagent.ecs.*;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.executors.CreateAgentRequestExecutor;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class CreateAgentRequest {
    private static final Gson GSON = new GsonBuilder().setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES).create();

    @Expose
    @SerializedName("auto_register_key")
    private String autoRegisterKey;

    @SerializedName("elastic_agent_profile_properties")
    private ElasticAgentProfileProperties elasticAgentProfileProperties;

    @Expose
    @SerializedName("environment")
    private String environment;

    @Expose
    @SerializedName("job_identifier")
    private JobIdentifier jobIdentifier;

    @Expose
    @SerializedName("cluster_profile_properties")
    private ClusterProfileProperties clusterProfileProperties;

    public CreateAgentRequest() {
    }

    public CreateAgentRequest(String autoRegisterKey, ElasticAgentProfileProperties elasticAgentProfileProperties, String environment, ClusterProfileProperties clusterProfileProperties) {
        this.autoRegisterKey = autoRegisterKey;
        this.elasticAgentProfileProperties = elasticAgentProfileProperties;
        this.environment = environment;
        this.clusterProfileProperties = clusterProfileProperties;
    }

    public static CreateAgentRequest fromJSON(String json) {
        return GSON.fromJson(json, CreateAgentRequest.class);
    }

    public String autoRegisterKey() {
        return autoRegisterKey;
    }

    public ElasticAgentProfileProperties elasticProfile() {
        return elasticAgentProfileProperties;
    }

    public String environment() {
        return environment;
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    public ClusterProfileProperties clusterProfileProperties() {
        return clusterProfileProperties;
    }

    public RequestExecutor executor(AgentInstances<ECSTask> agentInstances, PluginRequest pluginRequest, EventStream eventStream) {
        return new CreateAgentRequestExecutor(this, agentInstances, pluginRequest, eventStream);
    }

    public Set<KeyValuePair> autoRegisterPropertiesAsEnvironmentVars(String elasticAgentId) {
        Set<KeyValuePair> properties = new HashSet<>();
        if (isNotBlank(autoRegisterKey())) {
            properties.add(new KeyValuePair().withName("GO_EA_AUTO_REGISTER_KEY").withValue(autoRegisterKey()));
        }
        if (isNotBlank(environment())) {
            properties.add(new KeyValuePair().withName("GO_EA_AUTO_REGISTER_ENVIRONMENT").withValue(environment()));
        }

        properties.add(new KeyValuePair().withName("GO_EA_AUTO_REGISTER_ELASTIC_AGENT_ID").withValue(elasticAgentId));
        properties.add(new KeyValuePair().withName("GO_EA_AUTO_REGISTER_ELASTIC_PLUGIN_ID").withValue(Constants.PLUGIN_ID));
        properties.add(new KeyValuePair().withName("GO_EA_GUID").withValue(UUID.randomUUID().toString()));

        return properties;
    }
}
