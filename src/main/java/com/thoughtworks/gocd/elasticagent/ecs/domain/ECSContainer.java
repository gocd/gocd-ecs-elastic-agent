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

import com.amazonaws.services.ecs.model.*;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import lombok.EqualsAndHashCode;
import org.joda.time.DateTime;
import org.joda.time.Period;

import java.util.Date;
import java.util.HashMap;
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
    private final Date createdAt;
    private final Date startedAt;
    private final String taskName;
    private JobIdentifier jobIdentifier;
    private String containerArn;
    private Map<String, String> dockerLabels;
    private List<KeyValuePair> environments;
    private boolean privileged;
    private String hostname;
    private String dockerCommand;
    private LogConfiguration logConfiguration;
    private List<MountPoint> mountPoints;
    private Map<String, String> volumeMounts;
    private String createdSince;
    private String startedSince;

    public ECSContainer(Task task, TaskDefinition taskDefinition) {
        this.containerInstanceArn = task.getContainerInstanceArn();
        this.status = taskDefinition.getStatus();
        this.createdAt = task.getCreatedAt();
        this.startedAt = task.getStartedAt();
        this.createdSince = toRelativePeriod(createdAt);
        this.startedSince = toRelativePeriod(startedAt);
        final String[] taskArnParts = task.getTaskArn().split("/");
        this.taskName = taskArnParts[taskArnParts.length - 1];

        if (!taskDefinition.getContainerDefinitions().isEmpty()) {
            final ContainerDefinition containerDefinition = taskDefinition.getContainerDefinitions().get(0);

            this.cpu = containerDefinition.getCpu();
            this.memory = containerDefinition.getMemory();
            this.memoryReservation = containerDefinition.getMemoryReservation();
            this.name = containerDefinition.getName();
            this.image = containerDefinition.getImage();
            this.dockerLabels = containerDefinition.getDockerLabels();
            this.environments = containerDefinition.getEnvironment();
            this.jobIdentifier = JobIdentifier.fromJson(dockerLabels.get(LABEL_JOB_IDENTIFIER));
            this.privileged = containerDefinition.getPrivileged();
            this.hostname = containerDefinition.getHostname();
            this.dockerCommand = String.join("\n", containerDefinition.getCommand());
            this.logConfiguration = containerDefinition.getLogConfiguration();
            this.mountPoints = containerDefinition.getMountPoints();
            this.volumeMounts = initVolumeMounts(taskDefinition);
        }

        if (!task.getContainers().isEmpty()) {
            final Container container = task.getContainers().get(0);
            this.reason = container.getReason();
            this.exitCode = container.getExitCode();
            this.containerName = container.getName();
            this.lastStatus = container.getLastStatus();
            this.containerArn = container.getContainerArn();
        }
    }

    private String toRelativePeriod(Date date) {
        if (date == null) {
            return null;
        }
        return Util.PERIOD_FORMATTER.print(new Period(new DateTime(date), DateTime.now()));
    }

    private Map<String, String> initVolumeMounts(TaskDefinition taskDefinition) {
        final List<Volume> volumes = taskDefinition.getVolumes();
        if (volumes.isEmpty()) {

        }
        Map<String, String> volumeMount = new HashMap<>();
        final ContainerDefinition containerDefinition = taskDefinition.getContainerDefinitions().get(0);

        volumes.stream().forEach(volume -> {
            final MountPoint mountPoint = containerDefinition.getMountPoints().stream().filter(mount -> mount.getSourceVolume().equals(volume.getName())).findFirst().orElse(null);
            volumeMount.put(volume.getHost().getSourcePath(), mountPoint == null ? "" : mountPoint.getContainerPath());
        });
        return volumeMount;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public Date getStartedAt() {
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

    public List<KeyValuePair> getEnvironments() {
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

    public LogConfiguration getLogConfiguration() {
        return logConfiguration;
    }

    public List<MountPoint> getMountPoints() {
        return mountPoints;
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
