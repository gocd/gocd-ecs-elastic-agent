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
import com.thoughtworks.gocd.elasticagent.ecs.AgentInstances;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTask;
import com.thoughtworks.gocd.elasticagent.ecs.RequestExecutor;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Agent;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.executors.ShouldAssignWorkRequestExecutor;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.GSON;

public class ShouldAssignWorkRequest {
    @Expose
    @SerializedName("agent")
    private Agent agent;

    @Expose
    @SerializedName("environment")
    private String environment;

    @Expose
    @SerializedName("elastic_agent_profile_properties")
    private ElasticAgentProfileProperties elasticAgentProfileProperties;

    @Expose
    @SerializedName("job_identifier")
    private JobIdentifier jobIdentifier;

    @Expose
    @SerializedName("cluster_profile_properties")
    private ClusterProfileProperties clusterProfileProperties;

    //used in gson deserialization
    public ShouldAssignWorkRequest() {

    }

    public ShouldAssignWorkRequest(Agent agent, String environment, ElasticAgentProfileProperties elasticAgentProfileProperties, JobIdentifier jobIdentifier, ClusterProfileProperties clusterProfileProperties) {
        this.agent = agent;
        this.environment = environment;
        this.elasticAgentProfileProperties = elasticAgentProfileProperties;
        this.jobIdentifier = jobIdentifier;
        this.clusterProfileProperties = clusterProfileProperties;
    }

    public static ShouldAssignWorkRequest fromJSON(String json) {
        return GSON.fromJson(json, ShouldAssignWorkRequest.class);
    }

    public Agent agent() {
        return agent;
    }

    public String environment() {
        return environment;
    }

    public ClusterProfileProperties clusterProfileProperties() {
        return clusterProfileProperties;
    }

    public ElasticAgentProfileProperties elasticProfile() {
        return elasticAgentProfileProperties;
    }

    public RequestExecutor executor(AgentInstances<ECSTask> agentInstances) {
        return new ShouldAssignWorkRequestExecutor(this, agentInstances);
    }

    public JobIdentifier jobIdentifier() {
        return jobIdentifier;
    }
}
