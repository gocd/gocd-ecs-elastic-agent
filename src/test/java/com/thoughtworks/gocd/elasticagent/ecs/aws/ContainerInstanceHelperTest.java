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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

import com.thoughtworks.gocd.elasticagent.ecs.Constants;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.InstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ConsoleLogAppender;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ContainerInstanceFailedToRegisterException;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.LimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.services.ecs.model.*;

import java.text.MessageFormat;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LABEL_SERVER_ID;
import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstanceBuilder;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.*;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class ContainerInstanceHelperTest {
    private EcsClient ecsClient;
    private Ec2Client ec2Client;
    private ContainerInstanceHelper containerInstanceHelper;
    private PluginSettings pluginSettings;
    private String serverId = "gocd-server-id";
    private InstanceMatcher instanceMatcher;
    private ConsoleLogAppender consoleLogAppender;
    private SubnetSelector subnetSelector;
    @Captor
    private ArgumentCaptor<List<Instance>> argumentCaptor;

    @BeforeEach
    void setUp() {
        openMocks(this);
        pluginSettings = mock(PluginSettings.class);
        ecsClient = mock(EcsClient.class);
        ec2Client = mock(Ec2Client.class);
        instanceMatcher = mock(InstanceMatcher.class);
        subnetSelector = mock(SubnetSelector.class);
        consoleLogAppender = mock(ConsoleLogAppender.class);

        when(pluginSettings.ecsClient()).thenReturn(ecsClient);
        when(pluginSettings.ec2Client()).thenReturn(ec2Client);
        when(pluginSettings.getClusterName()).thenReturn("GoCD");

        when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(3);
        when(pluginSettings.getMaxWindowsInstancesAllowed()).thenReturn(3);
        when(pluginSettings.getLinuxRegisterTimeout()).thenReturn(Duration.ofSeconds(10));
        when(pluginSettings.getWindowsRegisterTimeout()).thenReturn(Duration.ofSeconds(10));

        containerInstanceHelper = new ContainerInstanceHelper(() -> serverId, instanceMatcher, subnetSelector);
    }

    @Test
    void shouldRemoveLastSeenIdleTagFromGivenInstances() {
        containerInstanceHelper.removeLastSeenIdleTag(pluginSettings, Arrays.asList("i-foobar1", "i-foobar2"));

        final ArgumentCaptor<DeleteTagsRequest> deleteTagsRequestArgumentCaptor = ArgumentCaptor.forClass(DeleteTagsRequest.class);
        verify(ec2Client).deleteTags(deleteTagsRequestArgumentCaptor.capture());

        final DeleteTagsRequest request = deleteTagsRequestArgumentCaptor.getValue();

        assertThat(request.resources()).hasSize(2).contains("i-foobar1", "i-foobar2");
        assertThat(request.tags()).hasSize(1).contains(Tag.builder().key(LAST_SEEN_IDLE).build());
    }

    @Test
    void shouldGroupByPlatform() {
        final Instance linuxInstance1 = runningLinuxInstance("i-linux1");
        final Instance linuxInstance2 = runningLinuxInstance("i-linux2");
        final Instance windowsInstance1 = runningWindowsInstance("i-windows1");
        final Instance windowsInstance2 = runningWindowsInstance("i-windows2");

        final Map<Platform, List<Instance>> groupByPlatform = ContainerInstanceHelper.groupByPlatform(Arrays.asList(linuxInstance1, linuxInstance2, windowsInstance1, windowsInstance2));

        assertThat(groupByPlatform)
                .hasSize(2)
                .containsEntry(LINUX, Arrays.asList(linuxInstance1, linuxInstance2))
                .containsEntry(WINDOWS, Arrays.asList(windowsInstance1, windowsInstance2));
    }

    @Test
    void shouldFilterInstancesByGivenPredicate() {
        final Instance linuxInstance1 = linuxInstance("i-linux1", InstanceStateName.RUNNING);
        final Instance linuxInstance2 = linuxInstance("i-linux2", InstanceStateName.PENDING);
        final Instance windowsInstance1 = windowsInstance("i-windows1", InstanceStateName.RUNNING);
        final Instance windowsInstance2 = windowsInstance("i-windows2", InstanceStateName.PENDING);
        final List<Instance> instances = Arrays.asList(linuxInstance1, linuxInstance2, windowsInstance1, windowsInstance2);

        final List<Instance> pendingInstances = ContainerInstanceHelper.filterBy(instances, instance -> instance.state().name().equals(InstanceStateName.PENDING));

        assertThat(pendingInstances)
                .hasSize(2)
                .contains(linuxInstance2, windowsInstance2);
    }

    @Test
    void shouldFilterInstancesByPlatform() {
        final Instance linuxInstance1 = linuxInstance("i-linux1", InstanceStateName.RUNNING);
        final Instance linuxInstance2 = linuxInstance("i-linux2", InstanceStateName.PENDING);
        final Instance windowsInstance1 = windowsInstance("i-windows1", InstanceStateName.RUNNING);
        final Instance windowsInstance2 = windowsInstance("i-windows2", InstanceStateName.PENDING);
        final List<Instance> instances = Arrays.asList(linuxInstance1, linuxInstance2, windowsInstance1, windowsInstance2);

        final List<Instance> pendingInstances = ContainerInstanceHelper.filterByPlatform(instances, WINDOWS);

        assertThat(pendingInstances)
                .hasSize(2)
                .contains(windowsInstance1, windowsInstance2);
    }

    @Test
    void shouldFilterInstancesByState() {
        final Instance linuxInstance1 = linuxInstance("i-linux1", InstanceStateName.RUNNING);
        final Instance linuxInstance2 = linuxInstance("i-linux2", InstanceStateName.PENDING);
        final Instance windowsInstance1 = windowsInstance("i-windows1", InstanceStateName.RUNNING);
        final Instance windowsInstance2 = windowsInstance("i-windows2", InstanceStateName.PENDING);
        final List<Instance> instances = Arrays.asList(linuxInstance1, linuxInstance2, windowsInstance1, windowsInstance2);

        final List<Instance> pendingInstances = ContainerInstanceHelper.filterByState(instances, InstanceStateName.RUNNING);

        assertThat(pendingInstances)
                .hasSize(2)
                .contains(linuxInstance1, windowsInstance1);
    }

    @Nested
    class GetCluster {
        @Test
        void shouldClusterInfo() {
            final ArgumentCaptor<DescribeClustersRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeClustersRequest.class);

            when(ecsClient.describeClusters(argumentCaptor.capture())).thenReturn(
                    DescribeClustersResponse.builder().clusters(Cluster.builder().clusterName("GoCD").build()).build()
            );

            final Cluster cluster = containerInstanceHelper.getCluster(pluginSettings);

            assertThat(cluster.clusterName()).isEqualTo(pluginSettings.getClusterName());

            final DescribeClustersRequest describeClustersRequest = argumentCaptor.getValue();
            assertThat(describeClustersRequest.clusters()).hasSize(1).contains(pluginSettings.getClusterName());
        }

        @Test
        void shouldErrorOutIfClusterNotExist() {
            when(ecsClient.describeClusters(any(DescribeClustersRequest.class))).thenReturn(DescribeClustersResponse.builder().build());

            final ClusterNotFoundException clusterNotFoundException = assertThrows(ClusterNotFoundException.class, () -> containerInstanceHelper.getCluster(pluginSettings));

            assertThat(clusterNotFoundException.getMessage()).contains("Cluster GoCD not found");
        }
    }

    @Nested
    class GetContainerInstances {
        @Test
        void shouldReturnEmptyListIfNoContainerInstancesAreRunning() {
            when(ecsClient.listContainerInstances(ArgumentCaptor.forClass(ListContainerInstancesRequest.class).capture()))
                    .thenReturn(listContainerInstancesResponse());

            when(ecsClient.describeContainerInstances(ArgumentCaptor.forClass(DescribeContainerInstancesRequest.class).capture()))
                    .thenReturn(describeContainerInstancesResponse());

            final List<ContainerInstance> containerInstances = containerInstanceHelper.getContainerInstances(pluginSettings);

            assertThat(containerInstances).isEmpty();
        }

        @Test
        void shouldGetAllRegisteredContainerInstancesWithCluster() {
            final ContainerInstance containerInstance1 = containerInstance("i-foobar1");
            final ContainerInstance containerInstance2 = containerInstance("i-foobar2");
            final ContainerInstance containerInstance3 = containerInstance("i-foobar3");

            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("arn1", "arn2", "arn3"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class))).thenReturn(describeContainerInstancesResponse(containerInstance1, containerInstance2, containerInstance3));

            final List<ContainerInstance> containerInstances = containerInstanceHelper.getContainerInstances(pluginSettings);

            assertThat(containerInstances)
                    .hasSize(3)
                    .contains(containerInstance1, containerInstance2, containerInstance3);
        }
    }

    @Nested
    class onDemandContainerInstances {
        @Test
        void shouldGetAllRegisteredContainerInstancesBackedByAOnDemandInstance() {
            final ContainerInstance onDemandContainerInstance = containerInstance("i-foobar1");
            final ContainerInstance spotContainerInstance = containerInstance("i-spot");
            final Instance onDemandInstance = Instance.builder().instanceId(onDemandContainerInstance.ec2InstanceId()).build();
            final Instance spotInstance = Instance.builder().instanceId(spotContainerInstance.ec2InstanceId()).spotInstanceRequestId("spot_id").build();

            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(
                    DescribeInstancesResponse.builder().reservations(
                            Reservation.builder().instances(onDemandInstance).build(),
                            Reservation.builder().instances(spotInstance).build()
                    ).build()
            );
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("arn1", "arn2"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class))).thenReturn(describeContainerInstancesResponse(onDemandContainerInstance, spotContainerInstance));

            final List<ContainerInstance> containerInstances = containerInstanceHelper.onDemandContainerInstances(pluginSettings);

            assertThat(containerInstances)
                    .hasSize(1)
                    .contains(onDemandContainerInstance);
        }
    }

    @Nested
    class spotContainerInstances {
        @Test
        void shouldGetAllRegisteredContainerInstancesBackedByASpotInstance() {
            final ContainerInstance onDemandContainerInstance = containerInstance("i-foobar1");
            final ContainerInstance spotContainerInstance = containerInstance("i-spot");
            final Instance onDemandInstance = Instance.builder().instanceId(onDemandContainerInstance.ec2InstanceId()).build();
            final Instance spotInstance = Instance.builder().instanceId(spotContainerInstance.ec2InstanceId()).spotInstanceRequestId("spot_id").build();

            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(
                    DescribeInstancesResponse.builder().reservations(
                            Reservation.builder().instances(onDemandInstance).build(),
                            Reservation.builder().instances(spotInstance).build()
                    ).build()
            );
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("arn1", "arn2"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class))).thenReturn(describeContainerInstancesResponse(onDemandContainerInstance, spotContainerInstance));

            final List<ContainerInstance> containerInstances = containerInstanceHelper.spotContainerInstances(pluginSettings);

            assertThat(containerInstances)
                    .hasSize(1)
                    .contains(spotContainerInstance);
        }
    }

    private Attribute serverIdAttribute(String serverId) {
        return Attribute.builder().name(LABEL_SERVER_ID).value(serverId).build();
    }

    @Test
    void shouldTerminateContainerInstance() {
        final ContainerInstance containerInstance = ContainerInstance.builder()
                .containerInstanceArn("container-instance-arn")
                .ec2InstanceId("ec2-instance-id")
                .build();

        pluginSettings.ecsClient().deregisterContainerInstance(DeregisterContainerInstanceRequest.builder()
                .containerInstance(containerInstance.containerInstanceArn())
                .cluster(pluginSettings.getClusterName())
                .force(true)
                .build()
        );

        pluginSettings.ec2Client().terminateInstances(TerminateInstancesRequest.builder().instanceIds(containerInstance.ec2InstanceId()).build());

        final ArgumentCaptor<DeregisterContainerInstanceRequest> deregisterContainerInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(DeregisterContainerInstanceRequest.class);
        verify(ecsClient).deregisterContainerInstance(deregisterContainerInstanceRequestArgumentCaptor.capture());

        final DeregisterContainerInstanceRequest deregisterContainerInstanceRequest = deregisterContainerInstanceRequestArgumentCaptor.getValue();

        assertThat(deregisterContainerInstanceRequest).isNotNull();
        assertThat(deregisterContainerInstanceRequest.cluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(deregisterContainerInstanceRequest.containerInstance()).isEqualTo(containerInstance.containerInstanceArn());

        final ArgumentCaptor<TerminateInstancesRequest> terminateInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        verify(ec2Client).terminateInstances(terminateInstancesRequestArgumentCaptor.capture());

        final TerminateInstancesRequest terminateInstancesRequest = terminateInstancesRequestArgumentCaptor.getValue();

        assertThat(terminateInstancesRequest).isNotNull();
        assertThat(terminateInstancesRequest.instanceIds()).contains(containerInstance.ec2InstanceId());
    }

    @Test
    void shouldGetEC2InstancesForContainerInstances() {
        final ContainerInstance containerInstance1 = ContainerInstance.builder()
                .containerInstanceArn("container-instance-arn-1")
                .ec2InstanceId("ec2-instance-id-1")
                .build();
        final Instance instance1 = Instance.builder().instanceId(containerInstance1.ec2InstanceId()).build();

        final ContainerInstance containerInstance2 = ContainerInstance.builder()
                .containerInstanceArn("container-instance-arn-1")
                .ec2InstanceId("ec2-instance-id-1")
                .build();
        final Instance instance2 = Instance.builder().instanceId(containerInstance2.ec2InstanceId()).build();

        final ArgumentCaptor<DescribeInstancesRequest> describeInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeInstancesRequest.class);
        when(ec2Client.describeInstances(describeInstancesRequestArgumentCaptor.capture())).thenReturn(
                DescribeInstancesResponse.builder().reservations(
                        Reservation.builder().instances(instance1).build(),
                        Reservation.builder().instances(instance2).build()
                ).build()
        );

        final List<Instance> instances = containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, Arrays.asList(containerInstance1, containerInstance2));

        assertThat(instances).hasSize(2);
        assertThat(instances).contains(instance1, instance2);
        assertThat(describeInstancesRequestArgumentCaptor.getValue().instanceIds()).contains(instance1.instanceId(), instance2.instanceId());
    }

    @Nested
    class GetAllEC2Instances {
        @Test
        void shouldMakeDescribeInstancesRequest() {
            final ArgumentCaptor<DescribeInstancesRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeInstancesRequest.class);

            when(ec2Client.describeInstances(argumentCaptor.capture())).thenReturn(describeInstancesResponse(windowsInstance("i-foobar1", InstanceStateName.RUNNING)));
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-foobar1"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class))).thenReturn(describeContainerInstancesResponse(containerInstance("i-foobar1")));

            final List<Instance> instances = containerInstanceHelper.getAllInstances(pluginSettings);

            assertThat(argumentCaptor.getValue().filters())
                    .hasSize(2)
                    .contains(
                            filter("tag:Creator", Constants.PLUGIN_ID),
                            filter("instance-state-name", InstanceStateName.PENDING.toString(), InstanceStateName.RUNNING.toString(), InstanceStateName.STOPPING.toString(), InstanceStateName.STOPPED.toString())
                    );
            assertThat(instances).hasSize(1);
        }

        @Test
        void shouldCollectInstancesFromMultipleReservations() {
            final DescribeInstancesResponse describeInstancesResponse = DescribeInstancesResponse.builder()
                    .reservations(
                            Reservation.builder().instances(windowsInstance("i-foobar1", InstanceStateName.RUNNING)).build(),
                            Reservation.builder().instances(linuxInstance("i-foobar2", InstanceStateName.RUNNING)).build()
                    ).build();

            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse);
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-foobar1", "i-foobar2"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class))).thenReturn(describeContainerInstancesResponse(
                    containerInstance("i-foobar1"),
                    containerInstance("i-foobar2")
            ));

            assertThat(containerInstanceHelper.getAllInstances(pluginSettings)).hasSize(2);
        }

        @Test
        void shouldReturnInstancesRegisteredWithClusterOrCreatedByTheSameServer() {
            final Instance registeredWithCluster = windowsInstance("i-foobar1", InstanceStateName.RUNNING);
            final Instance createdWithServerIdTag = linuxInstanceWithTag("i-foobar2", Tag.builder().key(LABEL_SERVER_ID).value(serverId).build());

            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(registeredWithCluster, createdWithServerIdTag));
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-foobar1"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class))).thenReturn(describeContainerInstancesResponse(containerInstance("i-foobar1")));

            assertThat(containerInstanceHelper.getAllInstances(pluginSettings))
                    .hasSize(2)
                    .contains(registeredWithCluster, createdWithServerIdTag);
        }
    }

    private Filter filter(String name, String... values) {
        return Filter.builder().name(name).values(values).build();
    }

    @Test
    void shouldUpdateLastSeenIdleTimeOnEC2IfItIsIdle() {
        final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);
        final ContainerInstance containerInstance = containerInstanceBuilder("i-123abcd", serverIdAttribute(serverId))
                .pendingTasksCount(0).runningTasksCount(0).build();

        when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("foo"));
        when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class))).thenReturn(describeContainerInstancesResponse(containerInstance));

        containerInstanceHelper.checkAndMarkEC2InstanceIdle(pluginSettings, "i-123abcd");

        verify(ec2Client, times(1)).createTags(argumentCaptor.capture());

        final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
        assertThat(createTagsRequest.resources())
                .hasSize(1)
                .contains("i-123abcd");

        assertThat(createTagsRequest.tags()).hasSize(1);
        assertThat(createTagsRequest.tags().getFirst().key()).isEqualTo(LAST_SEEN_IDLE);
    }

    @Test
    void shouldNotUpdateLastSeenIdleTimeOnEC2IfItIsNotIdle() {
        final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);
        final ContainerInstance containerInstance = containerInstanceBuilder("i-123abcd").pendingTasksCount(1).runningTasksCount(0).build();

        when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("foo"));
        when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class))).thenReturn(describeContainerInstancesResponse(containerInstance));

        containerInstanceHelper.checkAndMarkEC2InstanceIdle(pluginSettings, "i-123abcd");

        verify(ec2Client, times(0)).createTags(argumentCaptor.capture());
    }

    @Nested
    class StartInstance {
        @Test
        void shouldStartStoppedInstancesWhenItMatchesTheConfiguration() {
            final ContainerInstance.Builder instanceMatchingConfig = containerInstanceBuilder("i-abcd3", "arn-3");

            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-abcd1", "i-abcd2", "i-abcd3", "i-abcd4"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(
                            containerInstanceBuilder("i-abcd1", "arn-1").agentConnected(false).build(),
                            containerInstanceBuilder("i-abcd2", "arn-2").agentConnected(false).build(),
                            containerInstanceBuilder("i-abcd4", "arn-4").agentConnected(false).build(),
                            containerInstanceBuilder("i-abcd3", "arn-3").agentConnected(false).build())
                    )
                    .thenReturn(describeContainerInstancesResponse(instanceMatchingConfig.agentConnected(true).build()));

            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(
                    instance("i-abcd1", InstanceStateName.RUNNING, LINUX.name()),
                    instance("i-abcd2", InstanceStateName.STOPPED, WINDOWS.name()),
                    instance("i-abcd3", InstanceStateName.STOPPED, LINUX.name()),
                    instance("i-abcd4", InstanceStateName.STOPPING, LINUX.name())
            ));

            final Optional<List<ContainerInstance>> containerInstanceOptional = containerInstanceHelper.startInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender);

            assertThat(containerInstanceOptional.isPresent()).isTrue();
            assertThat(containerInstanceOptional.get())
                    .hasSize(1)
                    .contains(instanceMatchingConfig.build());

            verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
            verify(ecsClient, never()).deregisterContainerInstance(any(DeregisterContainerInstanceRequest.class));
            verify(ec2Client, never()).runInstances(any(RunInstancesRequest.class));

            verify(ec2Client).deleteTags(DeleteTagsRequest.builder().tags(Tag.builder().key(LAST_SEEN_IDLE).build()).resources("i-abcd3").build());
            verify(ec2Client).startInstances(StartInstancesRequest.builder().instanceIds("i-abcd3").build());

            InOrder inOrder = inOrder(consoleLogAppender);

            inOrder.verify(consoleLogAppender, times(1)).accept("Found existing stopped instance(s) matching platform configurations. Starting ([i-abcd3]) instances to schedule ECS Task.");
            inOrder.verify(consoleLogAppender, times(1)).accept("Waiting for instance(s) ([i-abcd3]) to register with cluster.");

            verifyNoMoreInteractions(consoleLogAppender);
        }

        @Test
        void shouldNeverStartASpotInstance() {
            final ContainerInstance.Builder spotInstanceMatchingConfig = containerInstanceBuilder("i-abcd2", "arn-2");

            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-abcd2"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(spotInstanceMatchingConfig.agentConnected(true).build()));

            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(
                    spotInstance("i-abcd2", InstanceStateName.STOPPED, LINUX.name())
            ));

            final Optional<List<ContainerInstance>> containerInstanceOptional = containerInstanceHelper.startInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender);

            assertThat(containerInstanceOptional.isPresent()).isFalse();

            verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
            verify(ecsClient, never()).deregisterContainerInstance(any(DeregisterContainerInstanceRequest.class));
            verify(ec2Client, never()).runInstances(any(RunInstancesRequest.class));

            verify(ec2Client, never()).deleteTags(any(DeleteTagsRequest.class));
            verify(ec2Client, never()).startInstances(any(StartInstancesRequest.class));
            verify(consoleLogAppender, never()).accept(any());
        }

        @Test
        @Timeout(value = 10, unit = SECONDS)
        void shouldDeleteInstanceIfItFailsToStartInTimeSpecifiedInPluginSettings() {
            final Instance instance = instance("i-abcd3", InstanceStateName.STOPPED, LINUX.name());
            final ContainerInstance.Builder instanceMatchingConfig = containerInstanceBuilder("i-abcd3", "arn-3")
                    .agentConnected(false);

            when(pluginSettings.getLinuxRegisterTimeout()).thenReturn(Duration.ofSeconds(1));
            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-abcd1", "i-abcd2", "i-abcd3", "i-abcd4"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(instanceMatchingConfig.build()))
                    .thenReturn(describeContainerInstancesResponse(instanceMatchingConfig.agentConnected(false).build()));

            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(instance));

            final ContainerInstanceFailedToRegisterException exception = assertThrows(ContainerInstanceFailedToRegisterException.class,
                    () -> containerInstanceHelper.startInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender));

            assertThat(exception.getMessage()).isEqualTo("EC2Instance failed to register with the ECS cluster: GoCD within 1 second. Terminated un-registered instance(s).");

            verify(ec2Client, never()).runInstances(any(RunInstancesRequest.class));

            verify(ec2Client).startInstances(StartInstancesRequest.builder().instanceIds("i-abcd3").build());
            verify(ecsClient).deregisterContainerInstance(DeregisterContainerInstanceRequest.builder().cluster("GoCD").containerInstance("arn-3").force(true).build());
            verify(ec2Client).terminateInstances(TerminateInstancesRequest.builder().instanceIds("i-abcd3").build());
        }
    }

    @Nested
    class CreateInstance {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldNotCreateNewEC2IfClusterIsAlreadyRunningMaxEC2Instance(Platform platform) {
            when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(1);
            when(pluginSettings.getMaxWindowsInstancesAllowed()).thenReturn(1);

            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-abcd1", "i-abcd2"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(containerInstance("i-abcd1"), containerInstance("i-abcd2")));
            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(
                    runningLinuxInstance("i-abcd1"), runningWindowsInstance("i-abcd2")
            ));

            final LimitExceededException exception = assertThrows(LimitExceededException.class,
                    () -> containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(platform), 1, consoleLogAppender));

            assertThat(exception.getMessage()).isEqualTo(MessageFormat.format("The number of {0} EC2 On-Demand Instances running is currently at the maximum permissible limit(1). Not creating any more On-Demand EC2 instances.", platform));

            verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
            verify(ecsClient, never()).deregisterContainerInstance(any(DeregisterContainerInstanceRequest.class));
            verify(ec2Client, never()).runInstances(any(RunInstancesRequest.class));
            verify(ec2Client, never()).startInstances(any(StartInstancesRequest.class));
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldTerminateStoppedInstanceWhen_AllInstancesAreInStoppedState_And_DoesNotHaveCapacityToCreateNewInstance(Platform platform) throws LimitExceededException {
            final Instance stoppedInstance = instance("i-abcd1", InstanceStateName.STOPPED, platform.name());
            final ContainerInstance containerInstance = containerInstance("i-abcd1", "arn-1");

            when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(1);
            when(pluginSettings.getMaxWindowsInstancesAllowed()).thenReturn(1);

            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class)))
                    .thenReturn(listContainerInstancesResponse("i-abcd1"));

            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(containerInstance))
                    .thenReturn(describeContainerInstancesResponse(containerInstance))
                    .thenReturn(describeContainerInstancesResponse(containerInstance("i-newone")));

            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(stoppedInstance));
            when(ec2Client.runInstances(any(RunInstancesRequest.class))).thenReturn(runInstanceResult(instance("i-newone")));

            containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(platform), 1, consoleLogAppender);

            verify(ec2Client, never()).startInstances(any(StartInstancesRequest.class));
            verify(ecsClient).deregisterContainerInstance(DeregisterContainerInstanceRequest.builder().force(true).cluster("GoCD").containerInstance("arn-1").build());
            verify(ec2Client).terminateInstances(TerminateInstancesRequest.builder().instanceIds("i-abcd1").build());
            verify(ec2Client).runInstances(any(RunInstancesRequest.class));

            InOrder inOrder = inOrder(consoleLogAppender);

            inOrder.verify(consoleLogAppender, times(1)).accept("Creating a new container instance to schedule ECS Task.");
            inOrder.verify(consoleLogAppender, times(1)).accept("Waiting for instance(s) ([i-newone]) to register with cluster.");

            verifyNoMoreInteractions(consoleLogAppender);
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldCreateANewInstance(Platform platform) throws LimitExceededException {
            final Instance instance = instance("i-foobar", InstanceStateName.RUNNING, platform.name());
            final ContainerInstance containerInstance = containerInstance("i-foobar", "arn-1");

            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-foobar"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(containerInstance))
                    .thenReturn(describeContainerInstancesResponse(containerInstance("i-newone", "arn-newinstance")));
            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(instance));
            when(ec2Client.runInstances(any(RunInstancesRequest.class))).thenReturn(runInstanceResult(instance("i-newone")));

            final Optional<List<ContainerInstance>> optionalContainerInstanceList = containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(platform), 1, consoleLogAppender);

            assertThat(optionalContainerInstanceList.isPresent()).isTrue();
            assertThat(optionalContainerInstanceList.get())
                    .hasSize(1)
                    .contains(containerInstance("i-newone", "arn-newinstance"));

            verify(ec2Client, never()).startInstances(any(StartInstancesRequest.class));
            verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
            verify(ecsClient, never()).deregisterContainerInstance(any(DeregisterContainerInstanceRequest.class));
            verify(ec2Client, times(1)).runInstances(any(RunInstancesRequest.class));
        }

        @Test
        void shouldNotConsiderSpotInstancesForCalculatingCapacityForTheCluster() throws LimitExceededException {
            final int MAX_LINUX_INSTANCE_ALLOWED = 1;
            final Instance spotInstance = spotInstance("i-foobar", InstanceStateName.RUNNING, LINUX.name());
            final ContainerInstance containerInstance = containerInstance("i-foobar", "arn-1");

            when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(MAX_LINUX_INSTANCE_ALLOWED);
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-foobar"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(containerInstance))
                    .thenReturn(describeContainerInstancesResponse(containerInstance("i-newone", "arn-newinstance")));
            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(spotInstance));
            when(ec2Client.runInstances(any(RunInstancesRequest.class))).thenReturn(runInstanceResult(instance("i-newone")));

            final Optional<List<ContainerInstance>> optionalContainerInstanceList = containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender);

            assertThat(optionalContainerInstanceList.isPresent()).isTrue();
            assertThat(optionalContainerInstanceList.get())
                    .hasSize(1)
                    .contains(containerInstance("i-newone", "arn-newinstance"));

            verify(ec2Client, never()).startInstances(any(StartInstancesRequest.class));
            verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
            verify(ecsClient, never()).deregisterContainerInstance(any(DeregisterContainerInstanceRequest.class));
            verify(ec2Client, times(1)).runInstances(any(RunInstancesRequest.class));
        }


        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldConsiderBothOnDemandAndSpotInstancesInAClusterToSelectASubnet(Platform platform) throws LimitExceededException {
            final int MAX_LINUX_INSTANCE_ALLOWED = 2;
            final Instance onDemandInstance = instance("i-onDemand", InstanceStateName.RUNNING, platform.name());
            final Instance spotInstance = spotInstance("i-spot", InstanceStateName.RUNNING, platform.name());
            final ContainerInstance onDemandContainerInstance = containerInstance("i-onDemand", "arn-1");
            final ContainerInstance spotContainerInstance = containerInstance("i-spot", "arn-2");

            when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(MAX_LINUX_INSTANCE_ALLOWED);
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-onDemand", "i-spot"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(spotContainerInstance, onDemandContainerInstance))
                    .thenReturn(describeContainerInstancesResponse(containerInstance("i-newone", "arn-newinstance")));
            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(spotInstance, onDemandInstance));
            when(ec2Client.runInstances(any(RunInstancesRequest.class))).thenReturn(runInstanceResult(instance("i-newone")));

            containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(platform), 1, consoleLogAppender);

            verify(subnetSelector).selectSubnetWithMinimumEC2Instances(any(), any(), argumentCaptor.capture());
            List<Instance> instances = argumentCaptor.getValue();
            assertThat(instances.size()).isEqualTo(2);
        }

        @Test
        @Timeout(value = 10, unit = SECONDS)
        void shouldTerminateInstancesWhichFailsToRegisterInTime() throws LimitExceededException {
            final Instance instance1 = instance("i-new-abcd1");
            final Instance instance2 = instance("i-new-abcd2");
            final Instance instance3 = instance("i-new-abcd3");
            final ContainerInstance registeredInstance = containerInstance("i-new-abcd1", "arn-newinstance1");

            // With a 1 second register timeout and the poller's 5 second retry interval, only a single poll
            // happens before the timeout, so the very first poll must already see the registered instance.
            when(pluginSettings.getLinuxRegisterTimeout()).thenReturn(Duration.ofSeconds(1));
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class)))
                    .thenReturn(listContainerInstancesResponse("i-new-abcd1"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(registeredInstance));
            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse());
            when(ec2Client.runInstances(any(RunInstancesRequest.class))).thenReturn(runInstanceResult(instance1, instance2, instance3));

            final Optional<List<ContainerInstance>> optionalContainerInstances = containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender);

            assertThat(optionalContainerInstances).contains(List.of(registeredInstance));

            verify(ec2Client, never()).startInstances(any(StartInstancesRequest.class));
            verify(ecsClient, never()).deregisterContainerInstance(any(DeregisterContainerInstanceRequest.class));
            verify(ec2Client, times(1)).runInstances(any(RunInstancesRequest.class));
            verify(ec2Client).terminateInstances(TerminateInstancesRequest.builder().instanceIds("i-new-abcd2", "i-new-abcd3").build());
        }
    }

    @Nested
    class StartOrCreateInstance {
        @Test
        void shouldStartRequiredInstancesFromStoppedInstancesAndAvoidToLaunchNewInstances() throws LimitExceededException {
            final ContainerInstance.Builder stoppedContainerInstance1 = containerInstanceBuilder("i-stopped1").agentConnected(false);
            final Instance stoppedInstance1 = instance("i-stopped1", InstanceStateName.STOPPED, LINUX.name());

            final ContainerInstance.Builder stoppedContainerInstance2 = containerInstanceBuilder("i-stopped2").agentConnected(false);
            final Instance stoppedInstance2 = instance("i-stopped2", InstanceStateName.STOPPED, LINUX.name());

            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(stoppedInstance1, stoppedInstance2));
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-stopped1", "i-stopped2"));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(stoppedContainerInstance1.build(), stoppedContainerInstance2.build()))
                    .thenReturn(describeContainerInstancesResponse(
                            stoppedContainerInstance1.agentConnected(true).build(),
                            stoppedContainerInstance2.agentConnected(true).build()
                    ));

            final List<ContainerInstance> containerInstances = containerInstanceHelper.startOrCreateInstance(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 2, consoleLogAppender);

            assertThat(containerInstances)
                    .hasSize(2)
                    .contains(stoppedContainerInstance1.agentConnected(true).build(), stoppedContainerInstance2.agentConnected(true).build());

            verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
            verify(ecsClient, never()).deregisterContainerInstance(any(DeregisterContainerInstanceRequest.class));
            verify(ec2Client, never()).runInstances(any(RunInstancesRequest.class));
            verify(ec2Client).startInstances(StartInstancesRequest.builder().instanceIds("i-stopped1", "i-stopped2").build());
        }

        @Test
        void shouldStartInstancesWhichMatchesTheConfigurationAndCreateRemaining() throws LimitExceededException {
            final ContainerInstance.Builder stoppedContainerInstance = containerInstanceBuilder("i-stopped").agentConnected(false);
            final Instance stoppedInstance = instance("i-stopped", InstanceStateName.STOPPED, LINUX.name());

            final Instance newlyLaunchedInstance = runningLinuxInstance("i-new-instance");
            final ContainerInstance newlyLaunchedContainerInstance = containerInstance("i-new-instance", true);

            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ec2Client.describeInstances(any(DescribeInstancesRequest.class))).thenReturn(describeInstancesResponse(stoppedInstance));
            when(ecsClient.listContainerInstances(any(ListContainerInstancesRequest.class))).thenReturn(listContainerInstancesResponse("i-stopped"));
            when(ec2Client.runInstances(any(RunInstancesRequest.class))).thenReturn(runInstanceResult(newlyLaunchedInstance));
            when(ecsClient.describeContainerInstances(any(DescribeContainerInstancesRequest.class)))
                    .thenReturn(describeContainerInstancesResponse(stoppedContainerInstance.build()))
                    .thenReturn(describeContainerInstancesResponse(newlyLaunchedContainerInstance, stoppedContainerInstance.agentConnected(true).build()));

            final List<ContainerInstance> containerInstances = containerInstanceHelper.startOrCreateInstance(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 2, consoleLogAppender);

            assertThat(containerInstances)
                    .hasSize(2)
                    .contains(stoppedContainerInstance.build(), newlyLaunchedContainerInstance);

            verify(ec2Client, never()).terminateInstances(any(TerminateInstancesRequest.class));
            verify(ecsClient, never()).deregisterContainerInstance(any(DeregisterContainerInstanceRequest.class));
            verify(ec2Client).startInstances(StartInstancesRequest.builder().instanceIds("i-stopped").build());
            verify(ec2Client).runInstances(any(RunInstancesRequest.class));
        }
    }

    private RunInstancesResponse runInstanceResult(Instance... instances) {
        return RunInstancesResponse.builder().instances(instances).build();
    }


    private ListContainerInstancesResponse listContainerInstancesResponse(String... arns) {
        return ListContainerInstancesResponse.builder().containerInstanceArns(arns).build();
    }

    private DescribeContainerInstancesResponse describeContainerInstancesResponse(ContainerInstance... containerInstances) {
        return DescribeContainerInstancesResponse.builder().containerInstances(containerInstances).build();
    }

    private DescribeInstancesResponse describeInstancesResponse(Instance... instances) {
        return DescribeInstancesResponse.builder().reservations(Reservation.builder().instances(instances).build()).build();
    }
}
