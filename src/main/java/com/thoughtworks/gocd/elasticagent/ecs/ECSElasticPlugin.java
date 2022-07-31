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

package com.thoughtworks.gocd.elasticagent.ecs;

import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.GoPlugin;
import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.go.plugin.api.annotation.Extension;
import com.thoughtworks.go.plugin.api.annotation.Load;
import com.thoughtworks.go.plugin.api.exceptions.UnhandledRequestTypeException;
import com.thoughtworks.go.plugin.api.info.PluginContext;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.go.plugin.api.request.GoPluginApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.TaskHelper;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.executors.*;
import com.thoughtworks.gocd.elasticagent.ecs.info.PluginProperties;
import com.thoughtworks.gocd.elasticagent.ecs.requests.*;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Extension
public class ECSElasticPlugin implements GoPlugin {
    public static final Logger LOG = Logger.getLoggerFor(ECSElasticPlugin.class);

    private PluginRequest pluginRequest;
    private static String serverId;
    private Map<String, ECSTasks> clusterSpecificAgentInstances;

    @Override
    public void initializeGoApplicationAccessor(GoApplicationAccessor accessor) {
        pluginRequest = new PluginRequest(accessor);
        clusterSpecificAgentInstances = new HashMap<>();
    }

    private void fetchServerIdFromServer() {
        if (StringUtils.isNotBlank(serverId)) {
            return;
        }

        LOG.info("Fetching server id " + StringUtils.isNotBlank(serverId));
        serverId = pluginRequest.getSeverInfo().getServerId();
        LOG.info("Got server id from the server: " + StringUtils.isNotBlank(serverId));
    }

    @Load
    public void onLoad(PluginContext ctx) {
        PluginProperties pluginProperties = PluginProperties.instance();
        LOG.info("Loading plugin " + pluginProperties.get("id") + " version " + pluginProperties.get("version"));
    }

    @Override
    public GoPluginApiResponse handle(GoPluginApiRequest request) {
        ClusterProfileProperties clusterProfileProperties;
        try {
            fetchServerIdFromServer();
            LOG.debug("Request from server: " + request.requestName());
            switch (Request.fromString(request.requestName())) {
                case PLUGIN_SETTINGS_GET_ICON:
                    return new GetPluginSettingsIconExecutor().execute();
                case REQUEST_GET_ELASTIC_AGENT_PROFILE_METADATA:
                    return new GetProfileMetadataExecutor().execute();
                case REQUEST_GET_ELASTIC_AGENT_PROFILE_VIEW:
                    return new GetProfileViewExecutor().execute();
                case REQUEST_VALIDATE_ELASTIC_AGENT_PROFILE:
                    return ProfileValidateRequest.fromJSON(request.requestBody()).executor().execute();
                case REQUEST_GET_CLUSTER_PROFILE_METADATA:
                    return new GetClusterProfileMetadataExecutor().execute();
                case REQUEST_GET_CLUSTER_PROFILE_VIEW:
                    return new GetClusterProfileViewRequestExecutor().execute();
                case REQUEST_VALIDATE_CLUSTER_PROFILE_CONFIGURATION:
                    return ValidateClusterProfileRequest.fromJSON(request.requestBody()).executor().execute();
                case REQUEST_CREATE_AGENT:
                    CreateAgentRequest createAgentRequest = CreateAgentRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = createAgentRequest.clusterProfileProperties();
                    refreshInstancesForCluster(clusterProfileProperties);
                    return createAgentRequest.executor(getAgentInstancesFor(clusterProfileProperties), pluginRequest, getEventStreamFor(clusterProfileProperties)).execute();
                case REQUEST_SHOULD_ASSIGN_WORK:
                    ShouldAssignWorkRequest shouldAssignWorkRequest = ShouldAssignWorkRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = shouldAssignWorkRequest.clusterProfileProperties();
                    refreshInstancesForCluster(clusterProfileProperties);
                    return shouldAssignWorkRequest.executor(getAgentInstancesFor(clusterProfileProperties)).execute();
                case REQUEST_JOB_COMPLETION:
                    JobCompletionRequest jobCompletionRequest = JobCompletionRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = jobCompletionRequest.clusterProfileProperties();
                    refreshInstancesForCluster(clusterProfileProperties);
                    return jobCompletionRequest.executor(getAgentInstancesFor(clusterProfileProperties), pluginRequest).execute();
                case REQUEST_SERVER_PING:
                    ServerPingRequest serverPingRequest = ServerPingRequest.fromJSON(request.requestBody());
                    List<ClusterProfileProperties> listOfClusterProfileProperties = serverPingRequest.allClusterProfileProperties();
                    refreshInstancesForAllClusters(listOfClusterProfileProperties);
                    return serverPingRequest.executor(clusterSpecificAgentInstances, pluginRequest).execute();
                case REQUEST_CLUSTER_STATUS_REPORT:
                    ClusterStatusReportRequest clusterStatusReportRequest = ClusterStatusReportRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = clusterStatusReportRequest.clusterProfileProperties();
                    refreshInstancesForCluster(clusterProfileProperties);
                    return clusterStatusReportRequest.executor(getAgentInstancesFor(clusterProfileProperties), getEventStreamFor(clusterProfileProperties)).execute();
                case REQUEST_AGENT_STATUS_REPORT:
                    AgentStatusReportRequest statusReportRequest = AgentStatusReportRequest.fromJSON(request.requestBody());
                    clusterProfileProperties = statusReportRequest.clusterProfileProperties();
                    refreshInstancesForCluster(clusterProfileProperties);
                    return statusReportRequest.executor(getAgentInstancesFor(clusterProfileProperties)).execute();
                case REQUEST_GET_CAPABILITIES:
                    return new GetCapabilitiesExecutor().execute();
                case REQUEST_CLUSTER_PROFILE_CHANGED:
                    return new DefaultGoPluginApiResponse(200);
                case REQUEST_MIGRATE_CONFIGURATION:
                    return MigrateConfigurationRequest.fromJSON(request.requestBody()).executor().execute();
                default:
                    throw new UnhandledRequestTypeException(request.requestName());
            }
        } catch (AmazonEC2Exception e) {
            LOG.error("Failed to handle request " + request.requestName() + " due to:" + e.getMessage());
            return DefaultGoPluginApiResponse.error("Failed to handle request " + request.requestName() + " due to:" + e.getMessage());
        } catch (Exception e) {
            LOG.error("Failed to handle request " + request.requestName() + " due to:", e);
            return DefaultGoPluginApiResponse.error("Failed to handle request " + request.requestName() + " due to:" + e.getMessage());
        }
    }

    private void refreshInstancesForAllClusters(List<ClusterProfileProperties> listOfClusterProfileProperties) {
        for (ClusterProfileProperties clusterProfileProperties : listOfClusterProfileProperties) {
            refreshInstancesForCluster(clusterProfileProperties);
        }
    }

    private ECSTasks getAgentInstancesFor(ClusterProfileProperties clusterProfileProperties) {
        return clusterSpecificAgentInstances.get(clusterProfileProperties.uuid());
    }

    private EventStream getEventStreamFor(ClusterProfileProperties clusterProfileProperties) {
        return getAgentInstancesFor(clusterProfileProperties).getEventStream();
    }

    private void refreshInstancesForCluster(ClusterProfileProperties clusterProfileProperties) {
        ECSTasks ecsTasks = clusterSpecificAgentInstances.getOrDefault(clusterProfileProperties.uuid(), new ECSTasks(new TaskHelper(), new ContainerInstanceHelper(), new EventStream()));
        ecsTasks.refreshAll(clusterProfileProperties);

        clusterSpecificAgentInstances.put(clusterProfileProperties.uuid(), ecsTasks);
    }

    @Override
    public GoPluginIdentifier pluginIdentifier() {
        return Constants.PLUGIN_IDENTIFIER;
    }

    public static String getServerId() {
        return serverId;
    }
}
