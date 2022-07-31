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

import com.amazonaws.services.ecs.model.Task;
import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.AgentInstances;
import com.thoughtworks.gocd.elasticagent.ecs.ECSTask;
import com.thoughtworks.gocd.elasticagent.ecs.aws.TaskHelper;
import com.thoughtworks.gocd.elasticagent.ecs.builders.PluginStatusReportViewBuilder;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ECSContainer;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ServerRequestFailedException;
import com.thoughtworks.gocd.elasticagent.ecs.reports.StatusReportGenerationErrorHandler;
import com.thoughtworks.gocd.elasticagent.ecs.reports.StatusReportGenerationException;
import com.thoughtworks.gocd.elasticagent.ecs.requests.AgentStatusReportRequest;
import freemarker.template.Template;

import java.util.Optional;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AgentStatusReportExecutor {
    private final AgentStatusReportRequest request;
    private final PluginStatusReportViewBuilder statusReportViewBuilder;
    private final TaskHelper taskHelper;
    private final AgentInstances<ECSTask> agentInstances;

    public AgentStatusReportExecutor(AgentStatusReportRequest request, AgentInstances<ECSTask> agentInstances) {
        this(request, agentInstances, PluginStatusReportViewBuilder.instance(), new TaskHelper());
    }

    public AgentStatusReportExecutor(AgentStatusReportRequest request,
                                     AgentInstances<ECSTask> agentInstances, PluginStatusReportViewBuilder builder, TaskHelper taskHelper) {
        this.request = request;
        this.statusReportViewBuilder = builder;
        this.taskHelper = taskHelper;
        this.agentInstances = agentInstances;
    }

    public GoPluginApiResponse execute() {
        String elasticAgentId = request.getElasticAgentId();
        JobIdentifier jobIdentifier = request.jobIdentifier();
        LOG.info(String.format("[status-report] Generating status report for agent: %s with job: %s", elasticAgentId, jobIdentifier));

        try {
            agentInstances.refreshAll(request.clusterProfileProperties());
            final ECSContainer container = getECSContainer(elasticAgentId, jobIdentifier);

            final Template template = statusReportViewBuilder.getTemplate("agent-status-report.template.ftlh");
            final String statusReportView = statusReportViewBuilder.build(template, container);

            JsonObject responseJSON = new JsonObject();
            responseJSON.addProperty("view", statusReportView);

            return DefaultGoPluginApiResponse.success(responseJSON.toString());
        } catch (Exception e) {
            return StatusReportGenerationErrorHandler.handle(statusReportViewBuilder, e);
        }
    }

    private ECSContainer getECSContainer(String elasticAgentId, JobIdentifier jobIdentifier) throws ServerRequestFailedException {
        return isBlank(elasticAgentId) ? findUsingJobIdentifier(jobIdentifier) : findUsingElasticAgentId(elasticAgentId);
    }

    private ECSContainer findUsingElasticAgentId(String elasticAgentId) throws ServerRequestFailedException {
        final ECSTask ecsTask = agentInstances.find(elasticAgentId);

        if (ecsTask != null) {
            final Optional<Task> task = taskHelper.refreshTask(request.clusterProfileProperties(), ecsTask.taskArn());
            return new ECSContainer(task.isPresent() ? task.get() : ecsTask.task(), ecsTask.taskDefinition());
        }

        throw StatusReportGenerationException.noRunningTask(elasticAgentId);
    }

    private ECSContainer findUsingJobIdentifier(JobIdentifier jobIdentifier) throws ServerRequestFailedException {
        final ECSTask ecsTask = agentInstances.findByJobIdentifier(jobIdentifier);

        if (ecsTask != null) {
            final Optional<Task> task = taskHelper.refreshTask(request.clusterProfileProperties(), ecsTask.taskArn());
            return new ECSContainer(task.isPresent() ? task.get() : ecsTask.task(), ecsTask.taskDefinition());
        }

        throw StatusReportGenerationException.noRunningTask(jobIdentifier);
    }
}
