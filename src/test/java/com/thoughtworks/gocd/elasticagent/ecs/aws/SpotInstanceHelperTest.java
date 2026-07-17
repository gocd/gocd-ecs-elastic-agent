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

import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.Constants;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.function.Supplier;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.spotInstance;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.spotInstanceBuilder;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static java.lang.String.format;
import static java.time.temporal.ChronoUnit.MINUTES;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class SpotInstanceHelperTest {
    private ContainerInstanceHelper containerInstanceHelper;
    private PluginSettings pluginSettings;
    private Ec2Client ec2Client;
    private SpotInstanceHelper spotInstanceHelper;
    private EC2Config ec2Config;
    private SpotInstanceRequestBuilder spotInstanceRequestBuilder;
    private SubnetSelector subnetSelector;
    @Mock
    private Supplier<String> serverIdSupplier;

    @BeforeEach
    void setUp() {
        openMocks(this);
        containerInstanceHelper = mock(ContainerInstanceHelper.class);
        spotInstanceRequestBuilder = mock(SpotInstanceRequestBuilder.class);
        subnetSelector = mock(SubnetSelector.class);
        pluginSettings = mock(PluginSettings.class);
        ec2Client = mock(Ec2Client.class);
        ec2Config = mock(EC2Config.class);

        spotInstanceHelper = new SpotInstanceHelper(containerInstanceHelper, spotInstanceRequestBuilder, subnetSelector, serverIdSupplier, Duration.ofSeconds(1));
    }

    @Nested
    class getAllSpotInstances {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldListAllSpotInstancesForAGivenClusterAndPlatform(Platform platform) {
            String instanceIdentifier = format("test_cluster_%s_SPOT_INSTANCE", platform);
            Instance spotInstance = Instance.builder().spotInstanceRequestId("req_id").platform(platform.name()).tags(Tag.builder().key("Name").value(instanceIdentifier).build()).build();
            Instance onDemandInstance = Instance.builder().platform(platform.name()).tags(Tag.builder().key("Name").value(instanceIdentifier).build()).build();

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(asList(spotInstance, onDemandInstance));

            List<Instance> spotInstances = spotInstanceHelper.getAllSpotInstances(pluginSettings, "test_cluster", platform);

            assertThat(spotInstances.size()).isEqualTo(1);
            assertThat(spotInstances).contains(spotInstance);
        }
    }

    @Nested
    class getAllSpotRequestsForCluster {
        @Test
        void shouldReturnAllSpotRequestsForTheClusterIrrespectiveOfTheState() {
            final String clusterName = "test_cluster";
            final String SERVER_ID = "asdsr3ewdsacsa";
            ArgumentCaptor<DescribeSpotInstanceRequestsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSpotInstanceRequestsRequest.class);
            SpotInstanceRequest spotInstanceRequest = SpotInstanceRequest.builder().spotInstanceRequestId("req_id").build();

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(pluginSettings.getClusterName()).thenReturn(clusterName);
            when(ec2Client.describeSpotInstanceRequests(argumentCaptor.capture())).thenReturn(DescribeSpotInstanceRequestsResponse.builder().spotInstanceRequests(spotInstanceRequest).build());
            when(serverIdSupplier.get()).thenReturn(SERVER_ID);

            List<SpotInstanceRequest> allSpotRequestsForCluster = spotInstanceHelper.getAllSpotRequestsForCluster(pluginSettings);

            assertThat(allSpotRequestsForCluster.size()).isEqualTo(1);
            assertThat(allSpotRequestsForCluster).contains(spotInstanceRequest);

            DescribeSpotInstanceRequestsRequest argument = argumentCaptor.getValue();
            assertThat(argument.filters().size()).isEqualTo(3);
            assertThat(argument.filters()).contains(Filter.builder().name("tag:Creator").values("com.thoughtworks.gocd.elastic-agent.ecs").build());
            assertThat(argument.filters()).contains(Filter.builder().name("tag:cluster-name").values(clusterName).build());
            assertThat(argument.filters()).contains(Filter.builder().name("tag:server-id").values(SERVER_ID).build());
        }
    }

    @Nested
    class getAllOpenOrSpotRequestsWithRunningInstances {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldReturnAllSpotRequestsWithRequestStateOpen(Platform platform) {
            final String clusterName = "test_cluster";
            final String SERVER_ID = "asdsr3ewdsacsa";
            ArgumentCaptor<DescribeSpotInstanceRequestsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSpotInstanceRequestsRequest.class);
            SpotInstanceRequest spotInstanceRequest = SpotInstanceRequest.builder().spotInstanceRequestId("req_id").build();

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(argumentCaptor.capture())).thenReturn(DescribeSpotInstanceRequestsResponse.builder().spotInstanceRequests(spotInstanceRequest).build());
            when(serverIdSupplier.get()).thenReturn(SERVER_ID);

            assertThat(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, clusterName, platform))
                    .containsExactly(spotInstanceRequest);

            DescribeSpotInstanceRequestsRequest argument = argumentCaptor.getValue();
            assertThat(argument.filters().size()).isEqualTo(5);
            assertThat(argument.filters()).contains(Filter.builder().name("tag:Creator").values("com.thoughtworks.gocd.elastic-agent.ecs").build());
            assertThat(argument.filters()).contains(Filter.builder().name("state").values("open", "active", "cancelled").build());
            assertThat(argument.filters()).contains(Filter.builder().name("tag:cluster-name").values(clusterName).build());
            assertThat(argument.filters()).contains(Filter.builder().name("tag:platform").values(platform.name()).build());
            assertThat(argument.filters()).contains(Filter.builder().name("tag:server-id").values(SERVER_ID).build());
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldReturnCancelledRequestsWithAnInstanceRunning(Platform platform) {
            final String SERVER_ID = "asdsr3ewdsacsa";
            SpotInstanceRequest cancelledSpotRequest = SpotInstanceRequest.builder().spotInstanceRequestId("req_id")
                    .state("cancelled").status(SpotInstanceStatus.builder().code("request-canceled-and-instance-running").build()).build();
            SpotInstanceRequest cancelledSpotRequestWithInstanceRunning = SpotInstanceRequest.builder().spotInstanceRequestId("req_id")
                    .state("cancelled").status(SpotInstanceStatus.builder().code("instance-terminated-by-service").build()).build();

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any(DescribeSpotInstanceRequestsRequest.class)))
                    .thenReturn(DescribeSpotInstanceRequestsResponse.builder().spotInstanceRequests(cancelledSpotRequest, cancelledSpotRequestWithInstanceRunning).build());
            when(serverIdSupplier.get()).thenReturn(SERVER_ID);

            assertThat(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, "test_cluster", platform))
                .containsExactly(cancelledSpotRequest);
        }
    }

    @Nested
    class requestSpotInstanceRequest {
        @Test
        void shouldRequestForASpotInstance() {
            RequestSpotInstancesRequest spotInstancesRequest = mock(RequestSpotInstancesRequest.class);
            RequestSpotInstancesResponse spotRequestResult = mock(RequestSpotInstancesResponse.class);

            when(spotInstanceRequestBuilder.eC2Config(ec2Config)).thenReturn(spotInstanceRequestBuilder);
            when(spotInstanceRequestBuilder.build()).thenReturn(spotInstancesRequest);
            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(spotInstanceRequestBuilder.subnet(any())).thenReturn(spotInstanceRequestBuilder);
            when(ec2Client.requestSpotInstances(spotInstancesRequest)).thenReturn(spotRequestResult);

            RequestSpotInstancesResponse requestSpotInstancesResponse = spotInstanceHelper.requestSpotInstanceRequest(pluginSettings, ec2Config);

            assertThat(requestSpotInstancesResponse).isEqualTo(spotRequestResult);
        }

        @Test
        void shouldRequestForASpotInstanceWithSubnet() {
            RequestSpotInstancesRequest spotInstancesRequest = mock(RequestSpotInstancesRequest.class);
            RequestSpotInstancesResponse spotRequestResult = mock(RequestSpotInstancesResponse.class);
            List<Instance> allInstances = Collections.singletonList(Instance.builder().spotInstanceRequestId("req_id").build());
            List<String> subnetIds = List.of("sub_net_id");
            Subnet subnet = mock(Subnet.class);

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(allInstances);
            when(ec2Config.getSubnetIds()).thenReturn(subnetIds);
            when(subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, subnetIds, allInstances)).thenReturn(subnet);
            when(spotInstanceRequestBuilder.eC2Config(ec2Config)).thenReturn(spotInstanceRequestBuilder);
            when(spotInstanceRequestBuilder.subnet(subnet)).thenReturn(spotInstanceRequestBuilder);
            when(spotInstanceRequestBuilder.build()).thenReturn(spotInstancesRequest);
            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.requestSpotInstances(spotInstancesRequest)).thenReturn(spotRequestResult);

            RequestSpotInstancesResponse requestSpotInstancesResponse = spotInstanceHelper.requestSpotInstanceRequest(pluginSettings, ec2Config);

            assertThat(requestSpotInstancesResponse).isEqualTo(spotRequestResult);
        }
    }

    @Nested
    class waitForSpotRequestToShowUp {
        @Test
        void shouldWaitForSpotRequestToBeQueryAbleBasedOnTheSpotInstanceRequestId() {
            SpotInstanceRequest spotInstanceRequest = SpotInstanceRequest.builder().spotInstanceRequestId("id1").build();

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any(DescribeSpotInstanceRequestsRequest.class))).thenReturn(DescribeSpotInstanceRequestsResponse.builder().spotInstanceRequests(spotInstanceRequest).build());

            assertThat(spotInstanceHelper.waitTillSpotRequestCanBeLookedUpById(pluginSettings, spotInstanceRequest.spotInstanceRequestId()))
                    .isTrue();
        }

        @Test
        void shouldErrorOutAndCancelASpotInstanceRequestIfItsNotQueryAble() {
            SpotInstanceRequest spotInstanceRequest = SpotInstanceRequest.builder().spotInstanceRequestId("id1").build();

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any(DescribeSpotInstanceRequestsRequest.class))).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class,
                    () -> spotInstanceHelper.waitTillSpotRequestCanBeLookedUpById(pluginSettings, spotInstanceRequest.spotInstanceRequestId()));

            verify(ec2Client).cancelSpotInstanceRequests(CancelSpotInstanceRequestsRequest.builder().spotInstanceRequestIds("id1").build());
        }
    }

    @Nested
    class cancelSpotInstanceRequest {
        @Test
        void shouldMakeRequestToCancelASpotInstanceRequest() {
            when(pluginSettings.ec2Client()).thenReturn(ec2Client);

            spotInstanceHelper.cancelSpotInstanceRequest(pluginSettings, "spot_id");

            verify(ec2Client).cancelSpotInstanceRequests(CancelSpotInstanceRequestsRequest.builder().spotInstanceRequestIds("spot_id").build());
        }
    }

    @Nested
    class tagSpotResources {
        @Test
        void shouldTagSpotResourcesWithServerId() {
            final String SERVER_ID = "jsdfsjkf23rjncas";
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(serverIdSupplier.get()).thenReturn(SERVER_ID);

            spotInstanceHelper.tagSpotResources(pluginSettings, List.of("spot_req_id"), LINUX);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.resources()).hasSize(1).contains("spot_req_id");
            assertThat(createTagsRequest.tags()).contains(Tag.builder().key("server-id").value(SERVER_ID).build());
        }

        @Test
        void shouldTagSpotResourcesWithCreator() {
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);

            spotInstanceHelper.tagSpotResources(pluginSettings, List.of("spot_req_id"), LINUX);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.tags()).contains(Tag.builder().key("Creator").value("com.thoughtworks.gocd.elastic-agent.ecs").build());
        }

        @Test
        void shouldTagSpotResourcesWithClusterName() {
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(pluginSettings.getClusterName()).thenReturn("gocd");

            spotInstanceHelper.tagSpotResources(pluginSettings, List.of("spot_req_id"), LINUX);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.tags()).contains(Tag.builder().key("cluster-name").value("gocd").build());
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldTagSpotResourcesWithPlatformName(Platform platform) {
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);

            spotInstanceHelper.tagSpotResources(pluginSettings, List.of("spot_req_id"), platform);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.tags()).contains(Tag.builder().key("platform").value(platform.name()).build());
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldTagSpotResourcesWithName(Platform platform) {
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(pluginSettings.getClusterName()).thenReturn("gocd");

            spotInstanceHelper.tagSpotResources(pluginSettings, List.of("spot_req_id"), platform);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.tags()).contains(Tag.builder().key("Name").value(format("gocd_%s_SPOT_INSTANCE", platform.name())).build());
        }
    }

    @Nested
    class getSpotRequestsWithARunningSpotInstance {
        @Test
        void shouldFetchAllSpotRequestsInAClusterForAGoCDServer() {
            final String SERVER_ID = "dhdsfodvjs3wefs";
            final ArgumentCaptor<DescribeSpotInstanceRequestsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSpotInstanceRequestsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(argumentCaptor.capture())).thenReturn(DescribeSpotInstanceRequestsResponse.builder().build());
            when(serverIdSupplier.get()).thenReturn(SERVER_ID);

            spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd");

            DescribeSpotInstanceRequestsRequest request = argumentCaptor.getValue();
            assertThat(request.filters()).contains(
                    filter("tag:Creator", Constants.PLUGIN_ID),
                    filter("tag:cluster-name", "gocd"),
                    filter("tag:server-id", SERVER_ID)
            );
        }

        @Test
        void shouldFetchAllActiveOrCancelledSpotRequests() {
            final ArgumentCaptor<DescribeSpotInstanceRequestsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSpotInstanceRequestsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(argumentCaptor.capture())).thenReturn(DescribeSpotInstanceRequestsResponse.builder().build());

            spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd");

            DescribeSpotInstanceRequestsRequest request = argumentCaptor.getValue();
            assertThat(request.filters()).contains(
                    filter("state", "active", "cancelled")
            );
        }

        @Test
        void shouldListActiveSpotRequests() {
            SpotInstanceRequest activeSpotRequest = SpotInstanceRequest.builder().state("active").build();
            SpotInstanceRequest openSpotRequest = SpotInstanceRequest.builder().state("open").build();

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any(DescribeSpotInstanceRequestsRequest.class))).thenReturn(DescribeSpotInstanceRequestsResponse.builder().spotInstanceRequests(activeSpotRequest, openSpotRequest).build());

            List<SpotInstanceRequest> requests = spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd");

            assertThat(requests.size()).isEqualTo(1);
            assertThat(requests).contains(activeSpotRequest);
        }

        @Test
        void shouldListCancelledSpotRequestsWithAnInstanceRunning() {
            SpotInstanceRequest cancelledInstanceRunning = SpotInstanceRequest.builder().state("cancelled").status(SpotInstanceStatus.builder().code("request-canceled-and-instance-running").build()).build();
            SpotInstanceRequest cancelledInstanceStopped = SpotInstanceRequest.builder().state("cancelled").status(SpotInstanceStatus.builder().code("instance-terminated-by-service").build()).build();

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any(DescribeSpotInstanceRequestsRequest.class))).thenReturn(DescribeSpotInstanceRequestsResponse.builder().spotInstanceRequests(cancelledInstanceRunning, cancelledInstanceStopped).build());

            List<SpotInstanceRequest> requests = spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd");

            assertThat(requests.size()).isEqualTo(1);
            assertThat(requests).contains(cancelledInstanceRunning);
        }
    }

    @Nested
    class getAllIdleSpotInstances {
        @Test
        void shouldListAllIdleSpotInstancesInACluster() {
            Instance spotInstance = Instance.builder().instanceId("spot_1").spotInstanceRequestId("req_id1").tags(Tag.builder().key("cluster-name").value("gocd").build()).build();
            Instance idleSpotInstance = Instance.builder().instanceId("spot_2").spotInstanceRequestId("req_id2").tags(Tag.builder().key("cluster-name").value("gocd").build()).build();
            Instance onDemandInstance = Instance.builder().instanceId("on_demand").platform("LINUX").tags(Tag.builder().key("cluster-name").value("gocd").build()).build();

            ContainerInstance spotContainerInstance = ContainerInstance.builder().ec2InstanceId("spot_1").runningTasksCount(1).pendingTasksCount(0).build();
            ContainerInstance idleSpotContainerInstance = ContainerInstance.builder().ec2InstanceId("spot_2").runningTasksCount(0).pendingTasksCount(0).build();

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(asList(spotInstance, idleSpotInstance, onDemandInstance));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(spotContainerInstance, idleSpotContainerInstance));

            List<Instance> idleSpotInstances = spotInstanceHelper.getAllIdleSpotInstances(pluginSettings, "gocd");

            assertThat(idleSpotInstances.size()).isEqualTo(1);
            assertThat(idleSpotInstances.getFirst()).isEqualTo(idleSpotInstance);
        }

        @Test
        void shouldNotListUnregisteredSpotInstances() {
            Instance spotInstance = Instance.builder().instanceId("spot_1").spotInstanceRequestId("req_id1").tags(Tag.builder().key("cluster-name").value("gocd").build()).build();
            Instance unregisterdIdleSpotInstance = Instance.builder().instanceId("spot_2").spotInstanceRequestId("req_id2").tags(Tag.builder().key("cluster-name").value("gocd").build()).build();
            ContainerInstance spotContainerInstance = ContainerInstance.builder().ec2InstanceId("spot_1").runningTasksCount(1).pendingTasksCount(0).build();

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(asList(spotInstance, unregisterdIdleSpotInstance));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(Collections.singletonList(spotContainerInstance));

            List<Instance> idleSpotInstances = spotInstanceHelper.getAllIdleSpotInstances(pluginSettings, "gocd");

            assertThat(idleSpotInstances.size()).isEqualTo(0);
        }
    }

    @Nested
    class tagSpotInstancesAsIdle {
        @Test
        void shouldTagSpotInstancesWithLastSeenIdleTag() {
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);

            spotInstanceHelper.tagSpotInstancesAsIdle(pluginSettings, asList("spot_id1", "spot_id2"));

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.resources()).hasSize(2).contains("spot_id1").contains("spot_id2");
            assertThat(createTagsRequest.tags()).hasSize(1);
            assertThat(createTagsRequest.tags().getFirst().key()).isEqualTo(LAST_SEEN_IDLE);
        }
    }

    @Nested
    class getIdleInstancesEligibleForTermination {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldGetIdleSpotInstancesWhichAreIdleForMoreThanConfiguredMaxAllowedTime(Platform platform) {
            long twentyTwoMinutesAgo = Clock.DEFAULT.now().minus(22, MINUTES).toEpochMilli();
            long tenMinutesAgo = Clock.DEFAULT.now().minus(10, MINUTES).toEpochMilli();

            final Instance idleForLong = spotInstanceBuilder("spot_1", InstanceStateName.RUNNING, platform.name())
                    .tags(Tag.builder().key(LAST_SEEN_IDLE).value(String.valueOf(twentyTwoMinutesAgo)).build(), Tag.builder().key("cluster-name").value("gocd").build())
                    .build();

            final Instance idleRecently = spotInstanceBuilder("spot_2", InstanceStateName.RUNNING, platform.name())
                    .tags(Tag.builder().key(LAST_SEEN_IDLE).value(String.valueOf(tenMinutesAgo)).build(), Tag.builder().key("cluster-name").value("gocd").build())
                    .build();

            Instance onDemandInstance = Instance.builder().instanceId("on_demand").platform(platform.name()).tags(Tag.builder().key("cluster-name").value("gocd").build()).build();

            ContainerInstance spotContainerInstance1 = ContainerInstance.builder().ec2InstanceId("spot_1").runningTasksCount(0).pendingTasksCount(0).build();
            ContainerInstance spotContainerInstance2 = ContainerInstance.builder().ec2InstanceId("spot_2").runningTasksCount(0).pendingTasksCount(0).build();

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(asList(idleForLong, idleRecently, onDemandInstance));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(spotContainerInstance1, spotContainerInstance2));
            when(pluginSettings.terminateIdleLinuxSpotInstanceAfter()).thenReturn(Duration.ofMinutes(20));
            when(pluginSettings.terminateIdleWindowsSpotInstanceAfter()).thenReturn(Duration.ofMinutes(20));

            List<Instance> idleSpotInstances = spotInstanceHelper.getIdleInstancesEligibleForTermination(pluginSettings, "gocd");

            assertThat(idleSpotInstances.size()).isEqualTo(1);
            assertThat(idleSpotInstances.getFirst()).isEqualTo(idleForLong);
        }

        @Test
        void shouldNotListSpotInstancesWhichAreNotInCluster() {
            long twentyTwoMinutesAgo = Clock.DEFAULT.now().minus(20, MINUTES).toEpochMilli();

            final Instance idleForLong = spotInstanceBuilder("spot_1", InstanceStateName.RUNNING, LINUX.name())
                    .tags(Tag.builder().key(LAST_SEEN_IDLE).value(String.valueOf(twentyTwoMinutesAgo)).build(), Tag.builder().key("cluster-name").value("gocd_prod").build())
                    .build();

            ContainerInstance spotContainerInstance1 = ContainerInstance.builder().ec2InstanceId("spot_1").runningTasksCount(0).pendingTasksCount(0).build();

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(Collections.singletonList(idleForLong));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(Collections.singletonList(spotContainerInstance1));
            when(pluginSettings.terminateIdleLinuxSpotInstanceAfter()).thenReturn(Duration.ofMinutes(20));
            when(pluginSettings.terminateIdleWindowsSpotInstanceAfter()).thenReturn(Duration.ofMinutes(20));

            List<Instance> idleSpotInstances = spotInstanceHelper.getIdleInstancesEligibleForTermination(pluginSettings, "gocd");

            assertThat(idleSpotInstances.size()).isEqualTo(0);
        }
    }

    @Nested
    class allRegisteredSpotInstancesForPlatform {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldListAllSpotInstancesWhichAreRegisteredWithACluster(Platform platform) {
            ContainerInstance spotContainerInstance = ContainerInstance.builder().ec2InstanceId("spot").build();
            ContainerInstance onDemandContainerInstance = ContainerInstance.builder().ec2InstanceId("on-demand").build();
            List<ContainerInstance> containerInstances = asList(spotContainerInstance, onDemandContainerInstance);
            Instance spotInstance = Instance.builder().instanceId("spot_1").spotInstanceRequestId("req_id1")
                    .state(InstanceState.builder().name(InstanceStateName.RUNNING).build()).platform(platform.name()).build();
            Instance onDemandInstance = Instance.builder().instanceId("on_demand")
                    .state(InstanceState.builder().name(InstanceStateName.RUNNING).build()).platform(platform.name()).build();

            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(asList(spotInstance, onDemandInstance));

            List<Instance> instances = spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, platform);

            assertThat(instances.size()).isEqualTo(1);
            assertThat(instances).contains(spotInstance);
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldListSpotInstancesWhichAreRunningOrPending(Platform platform) {
            ContainerInstance runningSpotContainerInstance = ContainerInstance.builder().ec2InstanceId("spot_running").build();
            ContainerInstance pendingSpotContainerInstance = ContainerInstance.builder().ec2InstanceId("spot_pending").build();
            ContainerInstance terminatedSpotContainerInstance = ContainerInstance.builder().ec2InstanceId("spot_terminated").build();

            List<ContainerInstance> containerInstances = asList(runningSpotContainerInstance, pendingSpotContainerInstance, terminatedSpotContainerInstance);
            Instance runningInstance = Instance.builder().instanceId("spot_running").spotInstanceRequestId("req_id1")
                    .state(InstanceState.builder().name(InstanceStateName.RUNNING).build()).platform(platform.name()).build();
            Instance pendingInstance = Instance.builder().instanceId("spot_pending").spotInstanceRequestId("req_id2")
                    .state(InstanceState.builder().name(InstanceStateName.PENDING).build()).platform(platform.name()).build();
            Instance terminatedInstance = Instance.builder().instanceId("spot_terminated").spotInstanceRequestId("req_id3")
                    .state(InstanceState.builder().name(InstanceStateName.TERMINATED).build()).platform(platform.name()).build();


            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances))
                    .thenReturn(asList(runningInstance, pendingInstance, terminatedInstance));

            List<Instance> instances = spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, platform);

            assertThat(instances.size()).isEqualTo(2);
            assertThat(instances).contains(runningInstance);
            assertThat(instances).contains(pendingInstance);

        }
    }

    private Filter filter(String name, String... values) {
        return Filter.builder().name(name).values(values).build();
    }
}
