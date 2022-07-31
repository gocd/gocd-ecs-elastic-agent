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

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.Tag;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ecs.AmazonECSClient;
import com.amazonaws.services.ecs.model.*;
import com.thoughtworks.gocd.elasticagent.ecs.Constants;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.InstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ConsoleLogAppender;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ContainerInstanceFailedToRegisterException;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.LimitExceededException;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InOrder;

import java.text.MessageFormat;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LABEL_SERVER_ID;
import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

class ContainerInstanceHelperTest {
    private AmazonECSClient ecsClient;
    private AmazonEC2Client ec2Client;
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
        ecsClient = mock(AmazonECSClient.class);
        ec2Client = mock(AmazonEC2Client.class);
        instanceMatcher = mock(InstanceMatcher.class);
        subnetSelector = mock(SubnetSelector.class);
        consoleLogAppender = mock(ConsoleLogAppender.class);

        when(pluginSettings.ecsClient()).thenReturn(ecsClient);
        when(pluginSettings.ec2Client()).thenReturn(ec2Client);
        when(pluginSettings.getClusterName()).thenReturn("GoCD");

        when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(3);
        when(pluginSettings.getMaxWindowsInstancesAllowed()).thenReturn(3);
        when(pluginSettings.getLinuxRegisterTimeout()).thenReturn(Period.seconds(10));
        when(pluginSettings.getWindowsRegisterTimeout()).thenReturn(Period.seconds(10));

        containerInstanceHelper = new ContainerInstanceHelper(() -> serverId, instanceMatcher, subnetSelector);
    }

    @Test
    void shouldRemoveLastSeenIdleTagFromGivenInstances() {
        containerInstanceHelper.removeLastSeenIdleTag(pluginSettings, Arrays.asList("i-foobar1", "i-foobar2"));

        final ArgumentCaptor<DeleteTagsRequest> deleteTagsRequestArgumentCaptor = ArgumentCaptor.forClass(DeleteTagsRequest.class);
        verify(ec2Client).deleteTags(deleteTagsRequestArgumentCaptor.capture());

        final DeleteTagsRequest request = deleteTagsRequestArgumentCaptor.getValue();

        assertThat(request.getResources()).hasSize(2).contains("i-foobar1", "i-foobar2");
        assertThat(request.getTags()).hasSize(1).contains(new Tag(LAST_SEEN_IDLE));
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
        final Instance linuxInstance1 = linuxInstance("i-linux1", RUNNING);
        final Instance linuxInstance2 = linuxInstance("i-linux2", PENDING);
        final Instance windowsInstance1 = windowsInstance("i-windows1", RUNNING);
        final Instance windowsInstance2 = windowsInstance("i-windows2", PENDING);
        final List<Instance> instances = Arrays.asList(linuxInstance1, linuxInstance2, windowsInstance1, windowsInstance2);

        final List<Instance> pendingInstances = ContainerInstanceHelper.filterBy(instances, instance -> instance.getState().getName().equals(PENDING));

        assertThat(pendingInstances)
                .hasSize(2)
                .contains(linuxInstance2, windowsInstance2);
    }

    @Test
    void shouldFilterInstancesByPlatform() {
        final Instance linuxInstance1 = linuxInstance("i-linux1", RUNNING);
        final Instance linuxInstance2 = linuxInstance("i-linux2", PENDING);
        final Instance windowsInstance1 = windowsInstance("i-windows1", RUNNING);
        final Instance windowsInstance2 = windowsInstance("i-windows2", PENDING);
        final List<Instance> instances = Arrays.asList(linuxInstance1, linuxInstance2, windowsInstance1, windowsInstance2);

        final List<Instance> pendingInstances = ContainerInstanceHelper.filterByPlatform(instances, WINDOWS);

        assertThat(pendingInstances)
                .hasSize(2)
                .contains(windowsInstance1, windowsInstance2);
    }

    @Test
    void shouldFilterInstancesByState() {
        final Instance linuxInstance1 = linuxInstance("i-linux1", RUNNING);
        final Instance linuxInstance2 = linuxInstance("i-linux2", PENDING);
        final Instance windowsInstance1 = windowsInstance("i-windows1", RUNNING);
        final Instance windowsInstance2 = windowsInstance("i-windows2", PENDING);
        final List<Instance> instances = Arrays.asList(linuxInstance1, linuxInstance2, windowsInstance1, windowsInstance2);

        final List<Instance> pendingInstances = ContainerInstanceHelper.filterByState(instances, RUNNING);

        assertThat(pendingInstances)
                .hasSize(2)
                .contains(linuxInstance1, windowsInstance1);
    }

    @Nested
    class GetCluster {
        @Test
        void shouldGetClusterInfo() {
            final ArgumentCaptor<DescribeClustersRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeClustersRequest.class);

            when(ecsClient.describeClusters(argumentCaptor.capture())).thenReturn(
                    new DescribeClustersResult().withClusters(new Cluster().withClusterName("GoCD"))
            );

            final Cluster cluster = containerInstanceHelper.getCluster(pluginSettings);

            assertThat(cluster.getClusterName()).isEqualTo(pluginSettings.getClusterName());

            final DescribeClustersRequest describeClustersRequest = argumentCaptor.getValue();
            assertThat(describeClustersRequest.getClusters()).hasSize(1).contains(pluginSettings.getClusterName());
        }

        @Test
        void shouldErrorOutIfClusterNotExist() {
            when(ecsClient.describeClusters(any())).thenReturn(new DescribeClustersResult());

            final ClusterNotFoundException clusterNotFoundException = assertThrows(ClusterNotFoundException.class, () -> containerInstanceHelper.getCluster(pluginSettings));

            assertThat(clusterNotFoundException.getMessage()).contains("Cluster GoCD not found");
        }
    }

    @Nested
    class GetContainerInstances {
        @Test
        void shouldReturnEmptyListIfNoContainerInstancesAreRunning() {
            when(ecsClient.listContainerInstances(ArgumentCaptor.forClass(ListContainerInstancesRequest.class).capture()))
                    .thenReturn(listContainerInstancesResult());

            when(ecsClient.describeContainerInstances(ArgumentCaptor.forClass(DescribeContainerInstancesRequest.class).capture()))
                    .thenReturn(describeContainerInstancesResult());

            final List<ContainerInstance> containerInstances = containerInstanceHelper.getContainerInstances(pluginSettings);

            assertThat(containerInstances).isEmpty();
        }

        @Test
        void shouldGetAllRegisteredContainerInstancesWithCluster() {
            final ContainerInstance containerInstance1 = containerInstance("i-foobar1");
            final ContainerInstance containerInstance2 = containerInstance("i-foobar2");
            final ContainerInstance containerInstance3 = containerInstance("i-foobar3");

            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("arn1", "arn2", "arn3"));
            when(ecsClient.describeContainerInstances(any())).thenReturn(describeContainerInstancesResult(containerInstance1, containerInstance2, containerInstance3));

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
            final Instance onDemandInstance = new Instance().withInstanceId(onDemandContainerInstance.getEc2InstanceId());
            final Instance spotInstance = new Instance().withInstanceId(spotContainerInstance.getEc2InstanceId()).withSpotInstanceRequestId("spot_id");

            when(ec2Client.describeInstances(any())).thenReturn(
                    new DescribeInstancesResult().withReservations(
                            new Reservation().withInstances(onDemandInstance),
                            new Reservation().withInstances(spotInstance)
                    )
            );
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("arn1", "arn2"));
            when(ecsClient.describeContainerInstances(any())).thenReturn(describeContainerInstancesResult(onDemandContainerInstance, spotContainerInstance));

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
            final Instance onDemandInstance = new Instance().withInstanceId(onDemandContainerInstance.getEc2InstanceId());
            final Instance spotInstance = new Instance().withInstanceId(spotContainerInstance.getEc2InstanceId()).withSpotInstanceRequestId("spot_id");

            when(ec2Client.describeInstances(any())).thenReturn(
                    new DescribeInstancesResult().withReservations(
                            new Reservation().withInstances(onDemandInstance),
                            new Reservation().withInstances(spotInstance)
                    )
            );
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("arn1", "arn2"));
            when(ecsClient.describeContainerInstances(any())).thenReturn(describeContainerInstancesResult(onDemandContainerInstance, spotContainerInstance));

            final List<ContainerInstance> containerInstances = containerInstanceHelper.spotContainerInstances(pluginSettings);

            assertThat(containerInstances)
                    .hasSize(1)
                    .contains(spotContainerInstance);
        }
    }

    private Attribute serverIdAttribute(String serverId) {
        return new Attribute().withName(LABEL_SERVER_ID).withValue(serverId);
    }

    @Test
    void shouldTerminateContainerInstance() {
        final ContainerInstance containerInstance = new ContainerInstance()
                .withContainerInstanceArn("container-instance-arn")
                .withEc2InstanceId("ec2-instance-id");

        pluginSettings.ecsClient().deregisterContainerInstance(new DeregisterContainerInstanceRequest()
                .withContainerInstance(containerInstance.getContainerInstanceArn())
                .withCluster(pluginSettings.getClusterName())
                .withForce(true)
        );

        pluginSettings.ec2Client().terminateInstances(new TerminateInstancesRequest().withInstanceIds(containerInstance.getEc2InstanceId()));

        final ArgumentCaptor<DeregisterContainerInstanceRequest> deregisterContainerInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(DeregisterContainerInstanceRequest.class);
        verify(ecsClient).deregisterContainerInstance(deregisterContainerInstanceRequestArgumentCaptor.capture());

        final DeregisterContainerInstanceRequest deregisterContainerInstanceRequest = deregisterContainerInstanceRequestArgumentCaptor.getValue();

        assertThat(deregisterContainerInstanceRequest).isNotNull();
        assertThat(deregisterContainerInstanceRequest.getCluster()).isEqualTo(pluginSettings.getClusterName());
        assertThat(deregisterContainerInstanceRequest.getContainerInstance()).isEqualTo(containerInstance.getContainerInstanceArn());

        final ArgumentCaptor<TerminateInstancesRequest> terminateInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        verify(ec2Client).terminateInstances(terminateInstancesRequestArgumentCaptor.capture());

        final TerminateInstancesRequest terminateInstancesRequest = terminateInstancesRequestArgumentCaptor.getValue();

        assertThat(terminateInstancesRequest).isNotNull();
        assertThat(terminateInstancesRequest.getInstanceIds()).contains(containerInstance.getEc2InstanceId());
    }

    @Test
    void shouldGetEC2InstancesForContainerInstances() {
        final ContainerInstance containerInstance1 = new ContainerInstance()
                .withContainerInstanceArn("container-instance-arn-1")
                .withEc2InstanceId("ec2-instance-id-1");
        final Instance instance1 = new Instance().withInstanceId(containerInstance1.getEc2InstanceId());

        final ContainerInstance containerInstance2 = new ContainerInstance()
                .withContainerInstanceArn("container-instance-arn-1")
                .withEc2InstanceId("ec2-instance-id-1");
        final Instance instance2 = new Instance().withInstanceId(containerInstance2.getEc2InstanceId());

        final ArgumentCaptor<DescribeInstancesRequest> describeInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(DescribeInstancesRequest.class);
        when(ec2Client.describeInstances(describeInstancesRequestArgumentCaptor.capture())).thenReturn(
                new DescribeInstancesResult().withReservations(
                        new Reservation().withInstances(instance1),
                        new Reservation().withInstances(instance2)
                )
        );

        final List<Instance> instances = containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, Arrays.asList(containerInstance1, containerInstance2));

        assertThat(instances).hasSize(2);
        assertThat(instances).contains(instance1, instance2);
        assertThat(describeInstancesRequestArgumentCaptor.getValue().getInstanceIds()).contains(instance1.getInstanceId(), instance2.getInstanceId());
    }

    @Nested
    class GetAllEC2Instances {
        @Test
        void shouldMakeDescribeInstancesRequest() {
            final ArgumentCaptor<DescribeInstancesRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeInstancesRequest.class);

            when(ec2Client.describeInstances(argumentCaptor.capture())).thenReturn(describeInstancesResult(windowsInstance("i-foobar1", RUNNING)));
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-foobar1"));
            when(ecsClient.describeContainerInstances(any())).thenReturn(describeContainerInstancesResult(containerInstance("i-foobar1")));

            final List<Instance> instances = containerInstanceHelper.getAllInstances(pluginSettings);

            assertThat(argumentCaptor.getValue().getFilters())
                    .hasSize(2)
                    .contains(
                            filter("tag:Creator", Constants.PLUGIN_ID),
                            filter("instance-state-name", PENDING, RUNNING, STOPPING, STOPPED)
                    );
            assertThat(instances).hasSize(1);
        }

        @Test
        void shouldCollectInstancesFromMultipleReservations() {
            final DescribeInstancesResult describeInstancesResult = new DescribeInstancesResult()
                    .withReservations(
                            new Reservation().withInstances(windowsInstance("i-foobar1", RUNNING)),
                            new Reservation().withInstances(linuxInstance("i-foobar2", RUNNING))
                    );

            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult);
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-foobar1", "i-foobar2"));
            when(ecsClient.describeContainerInstances(any())).thenReturn(describeContainerInstancesResult(
                    containerInstance("i-foobar1"),
                    containerInstance("i-foobar2")
            ));

            assertThat(containerInstanceHelper.getAllInstances(pluginSettings)).hasSize(2);
        }

        @Test
        void shouldReturnInstancesRegisteredWithClusterOrCreatedByTheSameServer() {
            final Instance registeredWithCluster = windowsInstance("i-foobar1", RUNNING);
            final Instance createdWithServerIdTag = linuxInstanceWithTag("i-foobar2", new Tag(LABEL_SERVER_ID, serverId));

            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(registeredWithCluster, createdWithServerIdTag));
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-foobar1"));
            when(ecsClient.describeContainerInstances(any())).thenReturn(describeContainerInstancesResult(containerInstance("i-foobar1")));

            assertThat(containerInstanceHelper.getAllInstances(pluginSettings))
                    .hasSize(2)
                    .contains(registeredWithCluster, createdWithServerIdTag);
        }
    }

    private Filter filter(String name, String... values) {
        return new Filter().withName(name).withValues(values);
    }

    @Test
    void shouldUpdateLastSeenIdleTimeOnEC2IfItIsIdle() {
        final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);
        final ContainerInstance containerInstance = containerInstance("i-123abcd", serverIdAttribute(serverId))
                .withPendingTasksCount(0).withRunningTasksCount(0);

        when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("foo"));
        when(ecsClient.describeContainerInstances(any())).thenReturn(describeContainerInstancesResult(containerInstance));

        containerInstanceHelper.checkAndMarkEC2InstanceIdle(pluginSettings, "i-123abcd");

        verify(ec2Client, times(1)).createTags(argumentCaptor.capture());

        final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
        assertThat(createTagsRequest.getResources())
                .hasSize(1)
                .contains("i-123abcd");

        assertThat(createTagsRequest.getTags()).hasSize(1);
        assertThat(createTagsRequest.getTags().get(0).getKey()).isEqualTo(LAST_SEEN_IDLE);
    }

    @Test
    void shouldNotUpdateLastSeenIdleTimeOnEC2IfItIsNotIdle() {
        final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);
        final ContainerInstance containerInstance = new ContainerInstance().withEc2InstanceId("i-123abcd").withPendingTasksCount(1).withRunningTasksCount(0);

        when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("foo"));
        when(ecsClient.describeContainerInstances(any())).thenReturn(describeContainerInstancesResult(containerInstance));

        containerInstanceHelper.checkAndMarkEC2InstanceIdle(pluginSettings, "i-123abcd");

        verify(ec2Client, times(0)).createTags(argumentCaptor.capture());
    }

    @Nested
    class StartInstance {
        @Test
        void shouldStartStoppedInstancesWhenItMatchesTheConfiguration() {
            final ContainerInstance instanceMatchingConfig = containerInstance("i-abcd3", "arn-3");

            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-abcd1", "i-abcd2", "i-abcd3", "i-abcd4"));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(
                            containerInstance("i-abcd1", "arn-1").withAgentConnected(false),
                            containerInstance("i-abcd2", "arn-2").withAgentConnected(false),
                            containerInstance("i-abcd4", "arn-4").withAgentConnected(false),
                            instanceMatchingConfig.withAgentConnected(false))
                    )
                    .thenReturn(describeContainerInstancesResult(instanceMatchingConfig.withAgentConnected(true)));

            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(
                    instance("i-abcd1", RUNNING, LINUX.name()),
                    instance("i-abcd2", STOPPED, WINDOWS.name()),
                    instance("i-abcd3", STOPPED, LINUX.name()),
                    instance("i-abcd4", STOPPING, LINUX.name())
            ));

            final Optional<List<ContainerInstance>> containerInstanceOptional = containerInstanceHelper.startInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender);

            assertThat(containerInstanceOptional.isPresent()).isTrue();
            assertThat(containerInstanceOptional.get())
                    .hasSize(1)
                    .contains(instanceMatchingConfig);

            verify(ec2Client, never()).terminateInstances(any());
            verify(ecsClient, never()).deregisterContainerInstance(any());
            verify(ec2Client, never()).runInstances(any());

            verify(ec2Client).deleteTags(new DeleteTagsRequest().withTags(new Tag(LAST_SEEN_IDLE)).withResources("i-abcd3"));
            verify(ec2Client).startInstances(new StartInstancesRequest().withInstanceIds("i-abcd3"));

            InOrder inOrder = inOrder(consoleLogAppender);

            inOrder.verify(consoleLogAppender, times(1)).accept("Found existing stopped instance(s) matching platform configurations. Starting ([i-abcd3]) instances to schedule ECS Task.");
            inOrder.verify(consoleLogAppender, times(1)).accept("Waiting for instance(s) ([i-abcd3]) to register with cluster.");

            verifyNoMoreInteractions(consoleLogAppender);
        }

        @Test
        void shouldNeverStartASpotInstance() {
            final ContainerInstance spotInstanceMatchingConfig = containerInstance("i-abcd2", "arn-2");

            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-abcd2"));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(spotInstanceMatchingConfig.withAgentConnected(true)));

            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(
                    spotInstance("i-abcd2", STOPPED, LINUX.name())
            ));

            final Optional<List<ContainerInstance>> containerInstanceOptional = containerInstanceHelper.startInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender);

            assertThat(containerInstanceOptional.isPresent()).isFalse();

            verify(ec2Client, never()).terminateInstances(any());
            verify(ecsClient, never()).deregisterContainerInstance(any());
            verify(ec2Client, never()).runInstances(any());

            verify(ec2Client, never()).deleteTags(any());
            verify(ec2Client, never()).startInstances(any());
            verify(consoleLogAppender, never()).accept(any());
        }

        @Test
        void shouldDeleteInstanceIfItFailsToStartInTimeSpecifiedInPluginSettings() {
            final Instance instance = instance("i-abcd3", STOPPED, LINUX.name());
            final ContainerInstance instanceMatchingConfig = containerInstance("i-abcd3", "arn-3")
                    .withAgentConnected(false);

            when(pluginSettings.getLinuxRegisterTimeout()).thenReturn(Period.seconds(1));
            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-abcd1", "i-abcd2", "i-abcd3", "i-abcd4"));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(instanceMatchingConfig))
                    .thenReturn(describeContainerInstancesResult(instanceMatchingConfig.withAgentConnected(false)));

            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(instance));

            final ContainerInstanceFailedToRegisterException exception = assertThrows(ContainerInstanceFailedToRegisterException.class,
                    () -> containerInstanceHelper.startInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender));

            assertThat(exception.getMessage()).isEqualTo("EC2Instance failed to register with the ECS cluster: GoCD within 1 seconds. Terminated un-registered instance(s).");

            verify(ec2Client, never()).runInstances(any());

            verify(ec2Client).startInstances(new StartInstancesRequest().withInstanceIds("i-abcd3"));
            verify(ecsClient).deregisterContainerInstance(new DeregisterContainerInstanceRequest().withCluster("GoCD").withContainerInstance("arn-3").withForce(true));
            verify(ec2Client).terminateInstances(new TerminateInstancesRequest().withInstanceIds("i-abcd3"));
        }
    }

    @Nested
    class CreateInstance {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldNotCreateNewEC2IfClusterIsAlreadyRunningMaxEC2Instance(Platform platform) {
            when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(1);
            when(pluginSettings.getMaxWindowsInstancesAllowed()).thenReturn(1);

            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-abcd1", "i-abcd2"));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(containerInstance("i-abcd1"), containerInstance("i-abcd2")));
            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(
                    runningLinuxInstance("i-abcd1"), runningWindowsInstance("i-abcd2")
            ));

            final LimitExceededException exception = assertThrows(LimitExceededException.class,
                    () -> containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(platform), 1, consoleLogAppender));

            assertThat(exception.getMessage()).isEqualTo(MessageFormat.format("The number of {0} EC2 On-Demand Instances running is currently at the maximum permissible limit(1). Not creating any more On-Demand EC2 instances.", platform));

            verify(ec2Client, never()).terminateInstances(any());
            verify(ecsClient, never()).deregisterContainerInstance(any());
            verify(ec2Client, never()).runInstances(any());
            verify(ec2Client, never()).startInstances(any());
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldTerminateStoppedInstanceWhen_AllInstancesAreInStoppedState_And_DoesNotHaveCapacityToCreateNewInstance(Platform platform) throws LimitExceededException {
            final Instance stoppedInstance = instance("i-abcd1", STOPPED, platform.name());
            final ContainerInstance containerInstance = containerInstance("i-abcd1", "arn-1");

            when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(1);
            when(pluginSettings.getMaxWindowsInstancesAllowed()).thenReturn(1);

            when(ecsClient.listContainerInstances(any()))
                    .thenReturn(listContainerInstancesResult("i-abcd1"));

            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(containerInstance))
                    .thenReturn(describeContainerInstancesResult(containerInstance))
                    .thenReturn(describeContainerInstancesResult(containerInstance("i-newone")));

            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(stoppedInstance));
            when(ec2Client.runInstances(any())).thenReturn(runInstanceResult(instance("i-newone")));

            containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(platform), 1, consoleLogAppender);

            verify(ec2Client, never()).startInstances(any());
            verify(ecsClient).deregisterContainerInstance(new DeregisterContainerInstanceRequest().withForce(true).withCluster("GoCD").withContainerInstance("arn-1"));
            verify(ec2Client).terminateInstances(new TerminateInstancesRequest().withInstanceIds("i-abcd1"));
            verify(ec2Client).runInstances(any());

            InOrder inOrder = inOrder(consoleLogAppender);

            inOrder.verify(consoleLogAppender, times(1)).accept("Creating a new container instance to schedule ECS Task.");
            inOrder.verify(consoleLogAppender, times(1)).accept("Waiting for instance(s) ([i-newone]) to register with cluster.");

            verifyNoMoreInteractions(consoleLogAppender);
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldCreateANewInstance(Platform platform) throws LimitExceededException {
            final Instance instance = instance("i-foobar", RUNNING, platform.name());
            final ContainerInstance containerInstance = containerInstance("i-foobar", "arn-1");

            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-foobar"));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(containerInstance))
                    .thenReturn(describeContainerInstancesResult(containerInstance("i-newone", "arn-newinstance")));
            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(instance));
            when(ec2Client.runInstances(any())).thenReturn(runInstanceResult(instance("i-newone")));

            final Optional<List<ContainerInstance>> optionalContainerInstanceList = containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(platform), 1, consoleLogAppender);

            assertThat(optionalContainerInstanceList.isPresent()).isTrue();
            assertThat(optionalContainerInstanceList.get())
                    .hasSize(1)
                    .contains(containerInstance("i-newone", "arn-newinstance"));

            verify(ec2Client, never()).startInstances(any());
            verify(ec2Client, never()).terminateInstances(any());
            verify(ecsClient, never()).deregisterContainerInstance(any());
            verify(ec2Client, times(1)).runInstances(any());
        }

        @Test
        void shouldNotConsiderSpotInstancesForCalculatingCapacityForTheCluster() throws LimitExceededException {
            final int MAX_LINUX_INSTANCE_ALLOWED = 1;
            final Instance spotInstance = spotInstance("i-foobar", RUNNING, LINUX.name());
            final ContainerInstance containerInstance = containerInstance("i-foobar", "arn-1");

            when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(MAX_LINUX_INSTANCE_ALLOWED);
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-foobar"));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(containerInstance))
                    .thenReturn(describeContainerInstancesResult(containerInstance("i-newone", "arn-newinstance")));
            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(spotInstance));
            when(ec2Client.runInstances(any())).thenReturn(runInstanceResult(instance("i-newone")));

            final Optional<List<ContainerInstance>> optionalContainerInstanceList = containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender);

            assertThat(optionalContainerInstanceList.isPresent()).isTrue();
            assertThat(optionalContainerInstanceList.get())
                    .hasSize(1)
                    .contains(containerInstance("i-newone", "arn-newinstance"));

            verify(ec2Client, never()).startInstances(any());
            verify(ec2Client, never()).terminateInstances(any());
            verify(ecsClient, never()).deregisterContainerInstance(any());
            verify(ec2Client, times(1)).runInstances(any());
        }


        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldConsiderBothOnDemandAndSpotInstancesInAClusterToSelectASubnet(Platform platform) throws LimitExceededException {
            final int MAX_LINUX_INSTANCE_ALLOWED = 2;
            final Instance onDemandInstance = instance("i-onDemand", RUNNING, platform.name());
            final Instance spotInstance = spotInstance("i-spot", RUNNING, platform.name());
            final ContainerInstance onDemandContainerInstance = containerInstance("i-onDemand", "arn-1");
            final ContainerInstance spotContainerInstance = containerInstance("i-spot", "arn-2");

            when(pluginSettings.getMaxLinuxInstancesAllowed()).thenReturn(MAX_LINUX_INSTANCE_ALLOWED);
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-onDemand", "i-spot"));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(spotContainerInstance, onDemandContainerInstance))
                    .thenReturn(describeContainerInstancesResult(containerInstance("i-newone", "arn-newinstance")));
            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(spotInstance, onDemandInstance));
            when(ec2Client.runInstances(any())).thenReturn(runInstanceResult(instance("i-newone")));

            containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(platform), 1, consoleLogAppender);

            verify(subnetSelector).selectSubnetWithMinimumEC2Instances(any(), any(), argumentCaptor.capture());
            List<Instance> instances = argumentCaptor.getValue();
            assertThat(instances.size()).isEqualTo(2);
        }

        @Test
        void shouldTerminateInstancesWhichFailsToRegisterInTime() throws LimitExceededException {
            final Instance instance1 = instance("i-new-abcd1");
            final Instance instance2 = instance("i-new-abcd2");
            final Instance instance3 = instance("i-new-abcd3");
            final ContainerInstance registeredInstance = containerInstance("i-new-abcd1", "arn-newinstance1");

            when(ecsClient.listContainerInstances(any()))
                    .thenReturn(listContainerInstancesResult())
                    .thenReturn(listContainerInstancesResult("i-new-abcd1"));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult())
                    .thenReturn(describeContainerInstancesResult(registeredInstance));
            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult());
            when(ec2Client.runInstances(any())).thenReturn(runInstanceResult(instance1, instance2, instance3));

            final Optional<List<ContainerInstance>> optionalContainerInstances = containerInstanceHelper.createInstances(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 1, consoleLogAppender);

            assertThat(optionalContainerInstances.isPresent()).isTrue();
            assertThat(optionalContainerInstances.get())
                    .hasSize(1)
                    .contains(registeredInstance);

            verify(ec2Client, never()).startInstances(any());
            verify(ecsClient, never()).deregisterContainerInstance(any());
            verify(ec2Client, times(1)).runInstances(any());
            verify(ec2Client).terminateInstances(new TerminateInstancesRequest().withInstanceIds("i-new-abcd2", "i-new-abcd3"));
        }
    }

    @Nested
    class StartOrCreateInstance {
        @Test
        void shouldStartRequiredInstancesFromStoppedInstancesAndAvoidToLaunchNewInstances() throws LimitExceededException {
            final ContainerInstance stoppedContainerInstance1 = containerInstance("i-stopped1", false);
            final Instance stoppedInstance1 = instance("i-stopped1", STOPPED, LINUX.name());

            final ContainerInstance stoppedContainerInstance2 = containerInstance("i-stopped2", false);
            final Instance stoppedInstance2 = instance("i-stopped2", STOPPED, LINUX.name());

            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(stoppedInstance1, stoppedInstance2));
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-stopped1", "i-stopped2"));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(stoppedContainerInstance1, stoppedContainerInstance2))
                    .thenReturn(describeContainerInstancesResult(
                            stoppedContainerInstance1.clone().withAgentConnected(true),
                            stoppedContainerInstance2.clone().withAgentConnected(true)
                    ));

            final Optional<List<ContainerInstance>> containerInstances = containerInstanceHelper.startOrCreateInstance(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 2, consoleLogAppender);

            assertThat(containerInstances.isPresent()).isTrue();
            assertThat(containerInstances.get())
                    .hasSize(2)
                    .contains(stoppedContainerInstance1.clone().withAgentConnected(true), stoppedContainerInstance2.clone().withAgentConnected(true));

            verify(ec2Client, never()).terminateInstances(any());
            verify(ecsClient, never()).deregisterContainerInstance(any());
            verify(ec2Client, never()).runInstances(any());
            verify(ec2Client).startInstances(new StartInstancesRequest().withInstanceIds("i-stopped1", "i-stopped2"));
        }

        @Test
        void shouldStartInstancesWhichMatchesTheConfigurationAndCreateRemaining() throws LimitExceededException {
            final ContainerInstance stoppedContainerInstance = containerInstance("i-stopped", false);
            final Instance stoppedInstance = instance("i-stopped", STOPPED, LINUX.name());

            final Instance newlyLaunchedInstance = runningLinuxInstance("i-new-instance");
            final ContainerInstance newlyLaunchedContainerInstance = containerInstance("i-new-instance", true);

            when(instanceMatcher.matches(any(), any())).thenReturn(true);
            when(ec2Client.describeInstances(any())).thenReturn(describeInstancesResult(stoppedInstance));
            when(ecsClient.listContainerInstances(any())).thenReturn(listContainerInstancesResult("i-stopped"));
            when(ec2Client.runInstances(any())).thenReturn(runInstanceResult(newlyLaunchedInstance));
            when(ecsClient.describeContainerInstances(any()))
                    .thenReturn(describeContainerInstancesResult(stoppedContainerInstance))
                    .thenReturn(describeContainerInstancesResult(newlyLaunchedContainerInstance, stoppedContainerInstance.withAgentConnected(true)));

            final Optional<List<ContainerInstance>> containerInstances = containerInstanceHelper.startOrCreateInstance(pluginSettings, ElasticAgentProfileProperties.empty(LINUX), 2, consoleLogAppender);

            assertThat(containerInstances.isPresent()).isTrue();
            assertThat(containerInstances.get())
                    .hasSize(2)
                    .contains(stoppedContainerInstance, newlyLaunchedContainerInstance);

            verify(ec2Client, never()).terminateInstances(any());
            verify(ecsClient, never()).deregisterContainerInstance(any());
            verify(ec2Client).startInstances(new StartInstancesRequest().withInstanceIds("i-stopped"));
            verify(ec2Client).runInstances(any());
        }
    }

    private RunInstancesResult runInstanceResult(Instance... instances) {
        return new RunInstancesResult().withReservation(new Reservation().withInstances(instances));
    }


    private ListContainerInstancesResult listContainerInstancesResult(String... arns) {
        return new ListContainerInstancesResult().withContainerInstanceArns(arns);
    }

    private DescribeContainerInstancesResult describeContainerInstancesResult(ContainerInstance... containerInstances) {
        return new DescribeContainerInstancesResult().withContainerInstances(containerInstances);
    }

    private DescribeInstancesResult describeInstancesResult(Instance... instances) {
        return new DescribeInstancesResult().withReservations(
                new Reservation().withInstances(instances)
        );
    }
}
