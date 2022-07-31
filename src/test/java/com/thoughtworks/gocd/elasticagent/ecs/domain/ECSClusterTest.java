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

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.Cluster;
import com.amazonaws.services.ecs.model.ContainerInstance;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.text.ParseException;
import java.util.Collections;
import java.util.List;

import static com.amazonaws.services.ec2.model.InstanceType.C3Large;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.AWSModelMother.*;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

class ECSClusterTest {

    private Cluster cluster;

    @BeforeEach
    void setUp() {
        cluster = clusterWith("GoCD", 5, 10, 20);
    }

    @Test
    void shouldReturnRegisteredWindowsOnDemandInstanceCount() {
        final List<ContainerInstance> containerInstances = asList(containerInstance("i-foobar1"), containerInstance("i-foobar2"), containerInstance("i-foobar3"));
        final List<Instance> instances = asList(runningWindowsInstance("i-foobar1"), runningLinuxInstance("i-foobar2"), runningWindowsInstance("i-foobar3"));

        final ECSCluster ecsCluster = new ECSCluster(cluster, containerInstances, instances, Collections.emptyList(), 2, 3, 0, 0);

        assertThat(ecsCluster.getRegisteredWindowsOnDemandInstanceCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnRegisteredLinuxOnDemandInstanceCount() {
        final List<ContainerInstance> containerInstances = asList(containerInstance("i-foobar1"), containerInstance("i-foobar2"), containerInstance("i-foobar3"));
        final List<Instance> instances = asList(runningWindowsInstance("i-foobar1"), runningLinuxInstance("i-foobar2"), runningWindowsInstance("i-foobar3"));

        final ECSCluster ecsCluster = new ECSCluster(cluster, containerInstances, instances, Collections.emptyList(), 2, 3, 0, 0);

        assertThat(ecsCluster.getRegisteredLinuxOnDemandInstanceCount()).isEqualTo(1);
    }

    @Test
    void shouldReturnRegisteredWindowsSpotInstanceCount() {
        final List<ContainerInstance> containerInstances = asList(containerInstance("i-foobar1"), containerInstance("i-foobar2"), containerInstance("i-foobar3"), containerInstance("i-foobar4"));
        final List<Instance> instances = asList(runningWindowsSpotInstance("i-foobar1"), runningLinuxSpotInstance("i-foobar2"), runningWindowsInstance("i-foobar3"), runningWindowsSpotInstance("i-foobar4"));

        final ECSCluster ecsCluster = new ECSCluster(cluster, containerInstances, instances, Collections.emptyList(), 2, 3, 2, 3);

        assertThat(ecsCluster.getRegisteredWindowsSpotInstanceCount()).isEqualTo(2);
    }

    @Test
    void shouldReturnRegisteredLinuxSpotInstanceCount() {
        final List<ContainerInstance> containerInstances = asList(containerInstance("i-foobar1"), containerInstance("i-foobar2"), containerInstance("i-foobar3"), containerInstance("i-foobar4"));
        final List<Instance> instances = asList(runningLinuxSpotInstance("i-foobar1"), runningLinuxInstance("i-foobar2"), runningWindowsInstance("i-foobar3"), runningLinuxSpotInstance("i-foobar4"));

        final ECSCluster ecsCluster = new ECSCluster(cluster, containerInstances, instances, Collections.emptyList(), 2, 3, 2, 3);

        assertThat(ecsCluster.getRegisteredLinuxSpotInstanceCount()).isEqualTo(2);
    }

    @Test
    void shouldGetRunningTaskCount() {
        final ECSCluster ecsCluster = new ECSCluster(cluster, emptyList(), emptyList(), emptyList(), 2, 3, 0, 0);

        assertThat(ecsCluster.getRunningTasksCount()).isEqualTo(10);
    }

    @Test
    void shouldGetPendingTaskCount() {
        final ECSCluster ecsCluster = new ECSCluster(cluster, emptyList(), emptyList(), emptyList(), 2, 3, 0, 0);

        assertThat(ecsCluster.getPendingTasksCount()).isEqualTo(20);
    }

    @Test
    void shouldBuildECSClusterWithDetails() throws Exception {
        final Cluster cluster = clusterWith("GoCD", 5, 10, 0);
        final ContainerInstance containerInstance = containerInstance(
                "instance-id-1",
                "arn/container-instance-1",
                "ACTIVE",
                8,
                4096,
                4,
                1024
        );
        final Instance instance = instance("instance-id-1", C3Large, "ami-2dad3da", toDate("13/05/2017 12:50:20"));
        final ECSContainer alpineContainer = containerWith("arn/container-instance-1", "alpine-container", "alpine", 100, 200, "13/05/2017 12:55:00", "13/05/2017 12:56:30");

        final ECSCluster ecsCluster = new ECSCluster(cluster, singletonList(containerInstance), singletonList(instance), singletonList(alpineContainer), 2, 3, 0, 0);

        assertThat(ecsCluster.getName()).isEqualTo("GoCD");
        assertThat(ecsCluster.getRunningTasksCount()).isEqualTo(10);

        assertThat(ecsCluster.getContainerInstances()).hasSize(1).contains(new ECSContainerInstance(containerInstance, instance, singletonList(alpineContainer)));
    }

    @Test
    void shouldBuildECSClusterWithDetailsOfInstanceType() throws ParseException {
        final Cluster cluster = clusterWith("GoCD", 5, 10, 0);
        final ContainerInstance onDemandContainerInstance = containerInstance("instance-id-1");
        final ContainerInstance spotContainerInstance = containerInstance("instance-id-2");

        final Instance onDemandInstance = instance("instance-id-1", C3Large, "ami-2dad3da", toDate("13/05/2017 12:50:20"));
        final Instance spotInstance = spotInstance("instance-id-2", "running", "ami-2dad3da");

        final ECSContainer alpineContainer = containerWith("arn/container-instance-1", "alpine-container", "alpine", 100, 200, "13/05/2017 12:55:00", "13/05/2017 12:56:30");

        final ECSCluster ecsCluster = new ECSCluster(cluster, asList(onDemandContainerInstance, spotContainerInstance), asList(onDemandInstance, spotInstance), singletonList(alpineContainer), 2, 3, 0, 0);

        assertThat(ecsCluster.getEc2InstanceType().size()).isEqualTo(2);
        assertThat(ecsCluster.getEc2InstanceType().get("instance-id-1")).isEqualTo("On-Demand");
        assertThat(ecsCluster.getEc2InstanceType().get("instance-id-2")).isEqualTo("Spot");
    }
}
