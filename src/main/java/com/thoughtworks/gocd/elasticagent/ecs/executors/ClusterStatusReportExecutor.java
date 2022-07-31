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

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTasks;
import com.thoughtworks.gocd.elasticagent.ecs.RequestExecutor;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.TaskHelper;
import com.thoughtworks.gocd.elasticagent.ecs.builders.PluginStatusReportViewBuilder;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ClusterProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ECSCluster;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ECSContainer;
import com.thoughtworks.gocd.elasticagent.ecs.events.Event;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventFingerprint;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.info.PluginProperties;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ClusterStatusReportRequest;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;

public class ClusterStatusReportExecutor implements RequestExecutor {
    private ClusterStatusReportRequest request;
    private final ECSTasks agentInstances;
    private final ContainerInstanceHelper helper;
    private final TaskHelper taskHelper;
    private final PluginStatusReportViewBuilder pluginStatusReportViewBuilder;
    private final EventStream eventStream;

    public ClusterStatusReportExecutor(ClusterStatusReportRequest request, EventStream eventStream, ECSTasks agentInstances) {
        this(request, agentInstances, new ContainerInstanceHelper(), new TaskHelper(), PluginStatusReportViewBuilder.instance(), eventStream);
    }

    ClusterStatusReportExecutor(ClusterStatusReportRequest request, ECSTasks agentInstances, ContainerInstanceHelper helper, TaskHelper taskHelper, PluginStatusReportViewBuilder pluginStatusReportViewBuilder, EventStream eventStream) {
        this.request = request;
        this.agentInstances = agentInstances;
        this.helper = helper;
        this.taskHelper = taskHelper;
        this.pluginStatusReportViewBuilder = pluginStatusReportViewBuilder;
        this.eventStream = eventStream;
    }

    @Override
    public GoPluginApiResponse execute() throws IOException, TemplateException {
        final Map<String, Object> dataModel = new HashMap<>();
        try {
            final ClusterProfileProperties clusterProfileProperties = request.clusterProfileProperties();
            agentInstances.refreshAll(clusterProfileProperties);

            if (clusterProfileProperties == null || !clusterProfileProperties.isConfigured()) {
                throw new RuntimeException("Please configure the cluster profile properly before using the plugin.");
            }

            final Cluster cluster = helper.getCluster(clusterProfileProperties);
            final List<ContainerInstance> containerInstances = new ArrayList<>();
            final List<Instance> instances = new ArrayList<>();
            final List<ECSContainer> ecsContainers = new ArrayList<>();

            if (cluster.getRegisteredContainerInstancesCount() != 0) {
                containerInstances.addAll(helper.getContainerInstances(clusterProfileProperties));
                instances.addAll(helper.ec2InstancesFromContainerInstances(clusterProfileProperties, containerInstances));
                ecsContainers.addAll(taskHelper.allRunningContainers(clusterProfileProperties));
            }

            dataModel.put("region", clusterProfileProperties.getRegion());
            final ECSCluster ecsCluster = new ECSCluster(cluster, containerInstances, instances, ecsContainers,
                    clusterProfileProperties.getMaxLinuxInstancesAllowed(), clusterProfileProperties.getMaxWindowsInstancesAllowed(),
                    clusterProfileProperties.getMaxLinuxSpotInstanceAllowed(), clusterProfileProperties.getMaxWindowsSpotInstanceAllowed());
            dataModel.put("cluster", ecsCluster);
            eventStream.remove(EventFingerprint.forStatusReport());
        } catch (Exception e) {
            LOG.error("[status report] Error accessing ECS cluster details", e);
            final String errorMessage = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            eventStream.update(Event.errorEvent(EventFingerprint.forStatusReport(), "Error accessing ECS cluster details", errorMessage));
        }

        addPluginVersionInformation(dataModel);
        dataModel.put("errors", eventStream.allErrors());
        dataModel.put("warnings", eventStream.allWarnings());

        final Template template = pluginStatusReportViewBuilder.getTemplate("status-report.template.ftlh");
        final String view = pluginStatusReportViewBuilder.build(template, dataModel);

        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("view", view);

        return DefaultGoPluginApiResponse.success(jsonObject.toString());
    }

    private void addPluginVersionInformation(Map<String, Object> dataModel) {
        try {
            PluginProperties instance = PluginProperties.instance();
            dataModel.put("fullVersion", instance.get("version"));
        } catch (Exception e) {
            LOG.error("Failed to add plugin version information to status report: " + e.getMessage());
        }
    }
}
