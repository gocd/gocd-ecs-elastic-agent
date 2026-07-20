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

import software.amazon.awssdk.services.ecs.model.*;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LABEL_JOB_IDENTIFIER;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static software.amazon.awssdk.services.ecs.model.TaskDefinitionStatus.ACTIVE;

public class AWSModelMother {
    private final static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    public static ECSContainer containerWith(String containerInstanceArn, String name, String image, int memoryReservation, int memory, String createdAt, String startedAt) throws ParseException {
        return containerWith(containerInstanceArn, name, image, memoryReservation, memory, createdAt, startedAt, new JobIdentifier());
    }

    public static ECSContainer containerWith(String containerInstanceArn, String name, String image, int memoryReservation, int memory, String createdAt, String startedAt, JobIdentifier jobIdentifier) throws ParseException {
        final Container container = Container.builder()
                .name(name)
                .containerArn("foo")
                .lastStatus("Running")
                .build();

        final Task task = Task.builder()
                .containerInstanceArn(containerInstanceArn)
                .createdAt(toInstant(createdAt))
                .startedAt(toInstant(startedAt))
                .containers(container)
                .lastStatus("Running")
                .taskArn("task-arn/" + name)
                .build();

        final ContainerDefinition containerDefinition = ContainerDefinition.builder()
                .memory(memory)
                .memoryReservation(memoryReservation)
                .image(image)
                .cpu(0)
                .privileged(false)
                .hostname("hostname")
                .environment(KeyValuePair.builder().name("ENV_FOO").value("ENV_FOO_VALUE").build())
                .dockerLabels(jobIdentifier == null ? Collections.emptyMap() : Collections.singletonMap(LABEL_JOB_IDENTIFIER, jobIdentifier.toJson()))
                .logConfiguration(LogConfiguration.builder().logDriver("awslogs").options(Collections.singletonMap("log-group", "build-logs")).build())
                .name(name)
                .build();

        final TaskDefinition taskDefinition = TaskDefinition.builder()
                .status(ACTIVE)
                .containerDefinitions(containerDefinition)
                .build();

        return new ECSContainer(task, taskDefinition);
    }

    public static Instant toInstant(String date) throws ParseException {
        return isBlank(date) ? null : sdf.parse(date).toInstant();
    }

    public static Cluster clusterWith(String name, int containerInstancesCount, int runningTasksCount, int pendingTasksCount) {
        return Cluster.builder()
                .clusterName(name)
                .registeredContainerInstancesCount(containerInstancesCount)
                .runningTasksCount(runningTasksCount)
                .pendingTasksCount(pendingTasksCount)
                .build();
    }
}
