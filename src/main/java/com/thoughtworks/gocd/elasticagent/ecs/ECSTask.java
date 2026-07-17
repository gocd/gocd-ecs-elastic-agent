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

import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.JobIdentifier;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.services.ecs.model.Task;
import software.amazon.awssdk.services.ecs.model.TaskDefinition;

import java.time.Instant;

@EqualsAndHashCode
public class ECSTask {
    private final Task task;
    private final Instant createdAt;
    private final String environment;
    private final JobIdentifier jobIdentifier;
    private final TaskDefinition taskDefinition;
    private final ElasticAgentProfileProperties elasticAgentProfileProperties;
    private final String ec2InstanceId;


    public ECSTask(Task task, TaskDefinition taskDefinition, ElasticAgentProfileProperties elasticAgentProfileProperties, JobIdentifier jobIdentifier, String environment, String ec2InstanceId) {
        this.task = task;
        this.jobIdentifier = jobIdentifier;
        this.environment = environment;
        this.taskDefinition = taskDefinition;
        this.elasticAgentProfileProperties = elasticAgentProfileProperties;
        this.ec2InstanceId = ec2InstanceId;
        // startedAt may be absent for a task that has not started yet; treat as "now" as the v1 (Joda) code did
        this.createdAt = this.task.startedAt() == null ? Instant.now() : this.task.startedAt();
    }

    public String name() {
        return taskDefinition.family();
    }

    public Instant createdAt() {
        return createdAt;
    }

    public String environment() {
        return environment;
    }

    public TaskDefinition taskDefinition() {
        return taskDefinition;
    }

    public ElasticAgentProfileProperties elasticProfile() {
        return elasticAgentProfileProperties;
    }

    public String taskArn() {
        return task.taskArn();
    }

    public String taskDefinitionArn() {
        return task.taskDefinitionArn();
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    public Task task() {
        return task;
    }

    public String getEC2InstanceId() {
        return ec2InstanceId;
    }
}
