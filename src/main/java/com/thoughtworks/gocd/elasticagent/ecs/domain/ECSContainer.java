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

import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import lombok.EqualsAndHashCode;
import software.amazon.awssdk.services.ecs.model.*;

import java.time.Instant;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LABEL_JOB_IDENTIFIER;

@EqualsAndHashCode
public class ECSContainer {

    private final String containerInstanceArn;
    private String containerName;
    private String reason;
    private String lastStatus;
    private Integer exitCode;
    private String name;
    private String image;
    private final String status;
    private Integer cpu;
    private Integer memory;
    private Integer memoryReservation;
    private final Instant createdAt;
    private final Instant startedAt;
    private final String taskName;
    private JobIdentifier jobIdentifier;
    private String containerArn;
    private Map<String, String> dockerLabels;
    private Map<String, String> environments;
    private boolean privileged;
    private String hostname;
    private String dockerCommand;
    private String logDriver;
    private Map<String, String> logOptions;
    private Map<String, String> volumeMounts;
    private final String createdSince;
    private final String startedSince;

    public ECSContainer(Task task, TaskDefinition taskDefinition) {
        this.containerInstanceArn = task.containerInstanceArn();
        this.status = taskDefinition.statusAsString();
        this.createdAt = task.createdAt();
        this.startedAt = task.startedAt();
        this.createdSince = toRelativePeriod(createdAt);
        this.startedSince = toRelativePeriod(startedAt);
        final String[] taskArnParts = task.taskArn().split("/");
        this.taskName = taskArnParts[taskArnParts.length - 1];

        if (!taskDefinition.containerDefinitions().isEmpty()) {
            final ContainerDefinition containerDefinition = taskDefinition.containerDefinitions().getFirst();

            this.cpu = containerDefinition.cpu();
            this.memory = containerDefinition.memory();
            this.memoryReservation = containerDefinition.memoryReservation();
            this.name = containerDefinition.name();
            this.image = containerDefinition.image();
            this.dockerLabels = containerDefinition.dockerLabels();
            this.environments = new LinkedHashMap<>();
            containerDefinition.environment().forEach(pair -> environments.put(pair.name(), pair.value()));
            this.jobIdentifier = JobIdentifier.fromJson(dockerLabels.get(LABEL_JOB_IDENTIFIER));
            this.privileged = containerDefinition.privileged();
            this.hostname = containerDefinition.hostname();
            this.dockerCommand = String.join("\n", containerDefinition.command());
            final LogConfiguration logConfiguration = containerDefinition.logConfiguration();
            this.logDriver = logConfiguration == null ? null : logConfiguration.logDriverAsString();
            this.logOptions = logConfiguration == null ? null : logConfiguration.options();
            this.volumeMounts = initVolumeMounts(taskDefinition);
        }

        if (!task.containers().isEmpty()) {
            final Container container = task.containers().getFirst();
            this.reason = container.reason();
            this.exitCode = container.exitCode();
            this.containerName = container.name();
            this.lastStatus = container.lastStatus();
            this.containerArn = container.containerArn();
        }
    }

    private String toRelativePeriod(Instant date) {
        if (date == null) {
            return null;
        }
        return Util.formatDurationWordsFromNow(date);
    }

    private Map<String, String> initVolumeMounts(TaskDefinition taskDefinition) {
        final List<Volume> volumes = taskDefinition.volumes();
        Map<String, String> volumeMount = new HashMap<>();
        final ContainerDefinition containerDefinition = taskDefinition.containerDefinitions().getFirst();

        volumes.forEach(volume -> {
            final MountPoint mountPoint = containerDefinition.mountPoints().stream().filter(mount -> mount.sourceVolume().equals(volume.name())).findFirst().orElse(null);
            volumeMount.put(volume.host().sourcePath(), mountPoint == null ? "" : mountPoint.containerPath());
        });
        return volumeMount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Long getCreatedAtMillis() {
        return createdAt == null ? null : createdAt.toEpochMilli();
    }

    public Long getStartedAtMillis() {
        return startedAt == null ? null : startedAt.toEpochMilli();
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public String getStatus() {
        return status;
    }

    public Integer getCpu() {
        return cpu;
    }

    public Integer getMemory() {
        return memory;
    }

    public Integer getMemoryReservation() {
        return memoryReservation;
    }

    public String getName() {
        return name;
    }

    public String getImage() {
        return image;
    }

    public String getContainerInstanceArn() {
        return containerInstanceArn;
    }

    public String getTaskName() {
        return taskName;
    }

    public JobIdentifier getJobIdentifier() {
        return jobIdentifier;
    }

    public String getContainerName() {
        return containerName;
    }

    public String getReason() {
        return reason;
    }

    public String getLastStatus() {
        return lastStatus;
    }

    public Integer getExitCode() {
        return exitCode;
    }

    public String getContainerArn() {
        return containerArn;
    }

    public Map<String, String> getDockerLabels() {
        return dockerLabels;
    }

    public Map<String, String> getEnvironments() {
        return environments;
    }

    public boolean isPrivileged() {
        return privileged;
    }

    public String getHostname() {
        return hostname;
    }

    public String getDockerCommand() {
        return dockerCommand;
    }

    public String getLogDriver() {
        return logDriver;
    }

    public Map<String, String> getLogOptions() {
        return logOptions;
    }

    public Map<String, String> getVolumeMounts() {
        return volumeMounts;
    }

    public String getCreatedSince() {
        return createdSince;
    }

    public String getStartedSince() {
        return startedSince;
    }
}
