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

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;

import static com.amazonaws.services.ecs.model.TaskDefinitionStatus.ACTIVE;
import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LABEL_JOB_IDENTIFIER;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class AWSModelMother {
    private final static SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy hh:mm:ss");

    public static ECSContainer containerWith(String containerInstanceArn, String name, String image, int memoryReservation, int memory, String createdAt, String startedAt) throws ParseException {
        return containerWith(containerInstanceArn, name, image, memoryReservation, memory, createdAt, startedAt, new JobIdentifier());
    }

    public static ECSContainer containerWith(String containerInstanceArn, String name, String image, int memoryReservation, int memory, String createdAt, String startedAt, JobIdentifier jobIdentifier) throws ParseException {
        final Container container = new Container()
                .withName(name)
                .withContainerArn("foo")
                .withLastStatus("Running");

        final Task task = new Task()
                .withContainerInstanceArn(containerInstanceArn)
                .withCreatedAt(toDate(createdAt))
                .withStartedAt(toDate(startedAt))
                .withContainers(container)
                .withLastStatus("Running")
                .withTaskArn("task-arn/" + name);

        final ContainerDefinition containerDefinition = new ContainerDefinition()
                .withMemory(memory)
                .withMemoryReservation(memoryReservation)
                .withImage(image)
                .withCpu(0)
                .withPrivileged(false)
                .withHostname("hostname")
                .withEnvironment(new KeyValuePair().withName("ENV_FOO").withValue("ENV_FOO_VALUE"))
                .withDockerLabels(Collections.singletonMap(LABEL_JOB_IDENTIFIER, jobIdentifier.toJson()))
                .withLogConfiguration(new LogConfiguration().withLogDriver("awslogs").withOptions(Collections.singletonMap("log-group", "build-logs")))
                .withName(name);

        final TaskDefinition taskDefinition = new TaskDefinition()
                .withStatus(ACTIVE)
                .withContainerDefinitions(containerDefinition);

        return new ECSContainer(task, taskDefinition);
    }

    public static Date toDate(String date) throws ParseException {
        return isBlank(date) ? null : sdf.parse(date);
    }

    public static Cluster clusterWith(String name, int containerInstancesCount, int runningTasksCount, int pendingTasksCount) {
        return new Cluster().withClusterName(name).withRegisteredContainerInstancesCount(containerInstancesCount).withRunningTasksCount(runningTasksCount).withPendingTasksCount(pendingTasksCount);
    }
}
