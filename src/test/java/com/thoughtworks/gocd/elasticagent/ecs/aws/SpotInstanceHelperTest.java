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

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.Constants;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.joda.time.Period;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.List;
import java.util.function.Supplier;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.spotInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.RUNNING;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class SpotInstanceHelperTest {
    private ContainerInstanceHelper containerInstanceHelper;
    private PluginSettings pluginSettings;
    private AmazonEC2 ec2Client;
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
        ec2Client = mock(AmazonEC2.class);
        ec2Config = mock(EC2Config.class);

        spotInstanceHelper = new SpotInstanceHelper(containerInstanceHelper, spotInstanceRequestBuilder, subnetSelector, serverIdSupplier, Period.seconds(1));
    }

    @Nested
    class getAllSpotInstances {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldListAllSpotInstancesForAGivenClusterAndPlatform(Platform platform) {
            String instanceIdentifier = format("test_cluster_%s_SPOT_INSTANCE", platform);
            Instance spotInstance = new Instance().withSpotInstanceRequestId("req_id").withPlatform(platform.name()).withTags(new Tag().withKey("Name").withValue(instanceIdentifier));
            Instance onDemandInstance = new Instance().withPlatform(platform.name()).withTags(new Tag().withKey("Name").withValue(instanceIdentifier));

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
            SpotInstanceRequest spotInstanceRequest = new SpotInstanceRequest().withSpotInstanceRequestId("req_id");

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(pluginSettings.getClusterName()).thenReturn(clusterName);
            when(ec2Client.describeSpotInstanceRequests(argumentCaptor.capture())).thenReturn(new DescribeSpotInstanceRequestsResult().withSpotInstanceRequests(spotInstanceRequest));
            when(serverIdSupplier.get()).thenReturn(SERVER_ID);

            List<SpotInstanceRequest> allSpotRequestsForCluster = spotInstanceHelper.getAllSpotRequestsForCluster(pluginSettings);

            assertThat(allSpotRequestsForCluster.size()).isEqualTo(1);
            assertThat(allSpotRequestsForCluster).contains(spotInstanceRequest);

            DescribeSpotInstanceRequestsRequest argument = argumentCaptor.getValue();
            assertThat(argument.getFilters().size()).isEqualTo(3);
            assertThat(argument.getFilters()).contains(new Filter().withName("tag:Creator").withValues("com.thoughtworks.gocd.elastic-agent.ecs"));
            assertThat(argument.getFilters()).contains(new Filter().withName("tag:cluster-name").withValues(clusterName));
            assertThat(argument.getFilters()).contains(new Filter().withName("tag:server-id").withValues(SERVER_ID));
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
            SpotInstanceRequest spotInstanceRequest = new SpotInstanceRequest().withSpotInstanceRequestId("req_id");

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(argumentCaptor.capture())).thenReturn(new DescribeSpotInstanceRequestsResult().withSpotInstanceRequests(spotInstanceRequest));
            when(serverIdSupplier.get()).thenReturn(SERVER_ID);

            List<SpotInstanceRequest> openSpotRequests = spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, clusterName, platform);

            assertThat(openSpotRequests.size()).isEqualTo(1);
            assertThat(openSpotRequests).contains(spotInstanceRequest);

            DescribeSpotInstanceRequestsRequest argument = argumentCaptor.getValue();
            assertThat(argument.getFilters().size()).isEqualTo(5);
            assertThat(argument.getFilters()).contains(new Filter().withName("tag:Creator").withValues("com.thoughtworks.gocd.elastic-agent.ecs"));
            assertThat(argument.getFilters()).contains(new Filter().withName("state").withValues("open", "active", "cancelled"));
            assertThat(argument.getFilters()).contains(new Filter().withName("tag:cluster-name").withValues(clusterName));
            assertThat(argument.getFilters()).contains(new Filter().withName("tag:platform").withValues(platform.name()));
            assertThat(argument.getFilters()).contains(new Filter().withName("tag:server-id").withValues(SERVER_ID));
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldReturnCancelledRequestsWithAnInstanceRunning(Platform platform) {
            final String SERVER_ID = "asdsr3ewdsacsa";
            SpotInstanceRequest cancelledSpotRequest = new SpotInstanceRequest().withSpotInstanceRequestId("req_id")
                    .withState("cancelled").withStatus(new SpotInstanceStatus().withCode("request-canceled-and-instance-running"));
            SpotInstanceRequest cancelledSpotRequestWithInstanceRunning = new SpotInstanceRequest().withSpotInstanceRequestId("req_id")
                    .withState("cancelled").withStatus(new SpotInstanceStatus().withCode("instance-terminated-by-service"));

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any()))
                    .thenReturn(new DescribeSpotInstanceRequestsResult().withSpotInstanceRequests(cancelledSpotRequest, cancelledSpotRequestWithInstanceRunning));
            when(serverIdSupplier.get()).thenReturn(SERVER_ID);

            List<SpotInstanceRequest> openSpotRequests = spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, "test_cluster", platform);

            assertThat(openSpotRequests.size()).isEqualTo(1);
            assertThat(openSpotRequests).contains(cancelledSpotRequest);
        }
    }

    @Nested
    class requestSpotInstanceRequest {
        @Test
        void shouldRequestForASpotInstance() {
            RequestSpotInstancesRequest spotInstancesRequest = mock(RequestSpotInstancesRequest.class);
            RequestSpotInstancesResult spotRequestResult = mock(RequestSpotInstancesResult.class);

            when(spotInstanceRequestBuilder.withEC2Config(ec2Config)).thenReturn(spotInstanceRequestBuilder);
            when(spotInstanceRequestBuilder.build()).thenReturn(spotInstancesRequest);
            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(spotInstanceRequestBuilder.withSubnet(any())).thenReturn(spotInstanceRequestBuilder);
            when(ec2Client.requestSpotInstances(spotInstancesRequest)).thenReturn(spotRequestResult);

            RequestSpotInstancesResult requestSpotInstancesResult = spotInstanceHelper.requestSpotInstanceRequest(pluginSettings, ec2Config, null);

            assertThat(requestSpotInstancesResult).isEqualTo(spotRequestResult);
        }

        @Test
        void shouldRequestForASpotInstanceWithSubnet() {
            RequestSpotInstancesRequest spotInstancesRequest = mock(RequestSpotInstancesRequest.class);
            RequestSpotInstancesResult spotRequestResult = mock(RequestSpotInstancesResult.class);
            List<Instance> allInstances = asList(new Instance().withSpotInstanceRequestId("req_id"));
            List<String> subnetIds = asList("sub_net_id");
            Subnet subnet = mock(Subnet.class);

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(allInstances);
            when(ec2Config.getSubnetIds()).thenReturn(subnetIds);
            when(subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, subnetIds, allInstances)).thenReturn(subnet);
            when(spotInstanceRequestBuilder.withEC2Config(ec2Config)).thenReturn(spotInstanceRequestBuilder);
            when(spotInstanceRequestBuilder.withSubnet(subnet)).thenReturn(spotInstanceRequestBuilder);
            when(spotInstanceRequestBuilder.build()).thenReturn(spotInstancesRequest);
            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.requestSpotInstances(spotInstancesRequest)).thenReturn(spotRequestResult);

            RequestSpotInstancesResult requestSpotInstancesResult = spotInstanceHelper.requestSpotInstanceRequest(pluginSettings, ec2Config, null);

            assertThat(requestSpotInstancesResult).isEqualTo(spotRequestResult);
        }
    }

    @Nested
    class waitForSpotRequestToShowUp {
        @Test
        void shouldWaitForSpotRequestToBeQueryAbleBasedOnTheSpotInstanceRequestId() {
            SpotInstanceRequest spotInstanceRequest = new SpotInstanceRequest().withSpotInstanceRequestId("id1");

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any())).thenReturn(mock(DescribeSpotInstanceRequestsResult.class));

            assertThat(spotInstanceHelper.waitTillSpotRequestCanBeLookedUpById(pluginSettings, spotInstanceRequest.getSpotInstanceRequestId()))
                    .isTrue();
        }

        @Test
        void shouldErrorOutAndCancelASpotInstanceRequestIfItsNotQueryAble() {
            SpotInstanceRequest spotInstanceRequest = new SpotInstanceRequest().withSpotInstanceRequestId("id1");

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any())).thenThrow(new RuntimeException());

            assertThrows(RuntimeException.class,
                    () -> spotInstanceHelper.waitTillSpotRequestCanBeLookedUpById(pluginSettings, spotInstanceRequest.getSpotInstanceRequestId()));

            verify(ec2Client).cancelSpotInstanceRequests(new CancelSpotInstanceRequestsRequest().withSpotInstanceRequestIds("id1"));
        }
    }

    @Nested
    class cancelSpotInstanceRequest {
        @Test
        void shouldMakeRequestToCancelASpotInstanceRequest() {
            when(pluginSettings.ec2Client()).thenReturn(ec2Client);

            spotInstanceHelper.cancelSpotInstanceRequest(pluginSettings, "spot_id");

            verify(ec2Client).cancelSpotInstanceRequests(new CancelSpotInstanceRequestsRequest().withSpotInstanceRequestIds("spot_id"));
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

            spotInstanceHelper.tagSpotResources(pluginSettings, asList("spot_req_id"), LINUX);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.getResources()).hasSize(1).contains("spot_req_id");
            assertThat(createTagsRequest.getTags()).contains(new Tag().withKey("server-id").withValue(SERVER_ID));
        }

        @Test
        void shouldTagSpotResourcesWithCreator() {
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);

            spotInstanceHelper.tagSpotResources(pluginSettings, asList("spot_req_id"), LINUX);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.getTags()).contains(new Tag().withKey("Creator").withValue("com.thoughtworks.gocd.elastic-agent.ecs"));
        }

        @Test
        void shouldTagSpotResourcesWithClusterName() {
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(pluginSettings.getClusterName()).thenReturn("gocd");

            spotInstanceHelper.tagSpotResources(pluginSettings, asList("spot_req_id"), LINUX);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.getTags()).contains(new Tag().withKey("cluster-name").withValue("gocd"));
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldTagSpotResourcesWithPlatformName(Platform platform) {
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);

            spotInstanceHelper.tagSpotResources(pluginSettings, asList("spot_req_id"), platform);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.getTags()).contains(new Tag().withKey("platform").withValue(platform.name()));
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldTagSpotResourcesWithName(Platform platform) {
            final ArgumentCaptor<CreateTagsRequest> argumentCaptor = ArgumentCaptor.forClass(CreateTagsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(pluginSettings.getClusterName()).thenReturn("gocd");

            spotInstanceHelper.tagSpotResources(pluginSettings, asList("spot_req_id"), platform);

            verify(ec2Client).createTags(argumentCaptor.capture());

            final CreateTagsRequest createTagsRequest = argumentCaptor.getValue();
            assertThat(createTagsRequest.getTags()).contains(new Tag().withKey("Name").withValue(format("gocd_%s_SPOT_INSTANCE", platform.name())));
        }
    }

    @Nested
    class getSpotRequestsWithARunningSpotInstance {
        @Test
        void shouldFetchAllSpotRequestsInAClusterForAGoCDServer() {
            final String SERVER_ID = "dhdsfodvjs3wefs";
            final ArgumentCaptor<DescribeSpotInstanceRequestsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSpotInstanceRequestsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(argumentCaptor.capture())).thenReturn(new DescribeSpotInstanceRequestsResult());
            when(serverIdSupplier.get()).thenReturn(SERVER_ID);

            spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd");

            DescribeSpotInstanceRequestsRequest request = argumentCaptor.getValue();
            assertThat(request.getFilters()).contains(
                    filter("tag:Creator", Constants.PLUGIN_ID),
                    filter("tag:cluster-name", "gocd"),
                    filter("tag:server-id", SERVER_ID)
            );
        }

        @Test
        void shouldFetchAllActiveOrCancelledSpotRequests() {
            final ArgumentCaptor<DescribeSpotInstanceRequestsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSpotInstanceRequestsRequest.class);

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(argumentCaptor.capture())).thenReturn(new DescribeSpotInstanceRequestsResult());

            spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd");

            DescribeSpotInstanceRequestsRequest request = argumentCaptor.getValue();
            assertThat(request.getFilters()).contains(
                    filter("state", "active", "cancelled")
            );
        }

        @Test
        void shouldListActiveSpotRequests() {
            SpotInstanceRequest activeSpotRequest = new SpotInstanceRequest().withState("active");
            SpotInstanceRequest openSpotRequest = new SpotInstanceRequest().withState("open");

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any())).thenReturn(new DescribeSpotInstanceRequestsResult().withSpotInstanceRequests(activeSpotRequest, openSpotRequest));

            List<SpotInstanceRequest> requests = spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd");

            assertThat(requests.size()).isEqualTo(1);
            assertThat(requests).contains(activeSpotRequest);
        }

        @Test
        void shouldListCancelledSpotRequestsWithAnInstanceRunning() {
            SpotInstanceRequest cancelledInstanceRunning = new SpotInstanceRequest().withState("cancelled").withStatus(new SpotInstanceStatus().withCode("request-canceled-and-instance-running"));
            SpotInstanceRequest cancelledInstanceStopped = new SpotInstanceRequest().withState("cancelled").withStatus(new SpotInstanceStatus().withCode("instance-terminated-by-service"));

            when(pluginSettings.ec2Client()).thenReturn(ec2Client);
            when(ec2Client.describeSpotInstanceRequests(any())).thenReturn(new DescribeSpotInstanceRequestsResult().withSpotInstanceRequests(cancelledInstanceRunning, cancelledInstanceStopped));

            List<SpotInstanceRequest> requests = spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd");

            assertThat(requests.size()).isEqualTo(1);
            assertThat(requests).contains(cancelledInstanceRunning);
        }
    }

    @Nested
    class getAllIdleSpotInstances {
        @Test
        void shouldListAllIdleSpotInstancesInACluster() {
            Instance spotInstance = new Instance().withInstanceId("spot_1").withSpotInstanceRequestId("req_id1").withTags(new Tag().withKey("cluster-name").withValue("gocd"));
            Instance idleSpotInstance = new Instance().withInstanceId("spot_2").withSpotInstanceRequestId("req_id2").withTags(new Tag().withKey("cluster-name").withValue("gocd"));
            Instance onDemandInstance = new Instance().withInstanceId("on_demand").withPlatform("LINUX").withTags(new Tag().withKey("cluster-name").withValue("gocd"));

            ContainerInstance spotContainerInstance = new ContainerInstance().withEc2InstanceId("spot_1").withRunningTasksCount(1).withPendingTasksCount(0);
            ContainerInstance idleSpotContainerInstance = new ContainerInstance().withEc2InstanceId("spot_2").withRunningTasksCount(0).withPendingTasksCount(0);

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(asList(spotInstance, idleSpotInstance, onDemandInstance));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(spotContainerInstance, idleSpotContainerInstance));

            List<Instance> idleSpotInstances = spotInstanceHelper.getAllIdleSpotInstances(pluginSettings, "gocd");

            assertThat(idleSpotInstances.size()).isEqualTo(1);
            assertThat(idleSpotInstances.get(0)).isEqualTo(idleSpotInstance);
        }

        @Test
        void shouldNotListUnregisteredSpotInstances() {
            Instance spotInstance = new Instance().withInstanceId("spot_1").withSpotInstanceRequestId("req_id1").withTags(new Tag().withKey("cluster-name").withValue("gocd"));
            Instance unregisterdIdleSpotInstance = new Instance().withInstanceId("spot_2").withSpotInstanceRequestId("req_id2").withTags(new Tag().withKey("cluster-name").withValue("gocd"));
            ContainerInstance spotContainerInstance = new ContainerInstance().withEc2InstanceId("spot_1").withRunningTasksCount(1).withPendingTasksCount(0);

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(asList(spotInstance, unregisterdIdleSpotInstance));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(spotContainerInstance));

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
            assertThat(createTagsRequest.getResources()).hasSize(2).contains("spot_id1").contains("spot_id2");
            assertThat(createTagsRequest.getTags()).hasSize(1);
            assertThat(createTagsRequest.getTags().get(0).getKey()).isEqualTo(LAST_SEEN_IDLE);
        }
    }

    @Nested
    class getIdleInstancesEligibleForTermination {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldGetIdleSpotInstancesWhichAreIdleForMoreThanConfiguredMaxAllowedTime(Platform platform) {
            long twentyTwoMinutesAgo = Clock.DEFAULT.now().minusMinutes(22).getMillis();
            long tenMinutesAgo = Clock.DEFAULT.now().minusMinutes(10).getMillis();

            final Instance idleForLong = spotInstance("spot_1", RUNNING, platform.name())
                    .withTags(new Tag(LAST_SEEN_IDLE, String.valueOf(twentyTwoMinutesAgo)), new Tag().withKey("cluster-name").withValue("gocd"));

            final Instance idleRecently = spotInstance("spot_2", RUNNING, platform.name())
                    .withTags(new Tag(LAST_SEEN_IDLE, String.valueOf(tenMinutesAgo)), new Tag().withKey("cluster-name").withValue("gocd"));

            Instance onDemandInstance = new Instance().withInstanceId("on_demand").withPlatform(platform.name()).withTags(new Tag().withKey("cluster-name").withValue("gocd"));

            ContainerInstance spotContainerInstance1 = new ContainerInstance().withEc2InstanceId("spot_1").withRunningTasksCount(0).withPendingTasksCount(0);
            ContainerInstance spotContainerInstance2 = new ContainerInstance().withEc2InstanceId("spot_2").withRunningTasksCount(0).withPendingTasksCount(0);

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(asList(idleForLong, idleRecently, onDemandInstance));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(spotContainerInstance1, spotContainerInstance2));
            when(pluginSettings.terminateIdleLinuxSpotInstanceAfter()).thenReturn(Period.minutes(20));
            when(pluginSettings.terminateIdleWindowsSpotInstanceAfter()).thenReturn(Period.minutes(20));

            List<Instance> idleSpotInstances = spotInstanceHelper.getIdleInstancesEligibleForTermination(pluginSettings, "gocd");

            assertThat(idleSpotInstances.size()).isEqualTo(1);
            assertThat(idleSpotInstances.get(0)).isEqualTo(idleForLong);
        }

        @Test
        void shouldNotListSpotInstancesWhichAreNotInCluster() {
            long twentyTwoMinutesAgo = Clock.DEFAULT.now().minusMinutes(20).getMillis();

            final Instance idleForLong = spotInstance("spot_1", RUNNING, LINUX.name())
                    .withTags(new Tag(LAST_SEEN_IDLE, String.valueOf(twentyTwoMinutesAgo)), new Tag().withKey("cluster-name").withValue("gocd_prod"));

            ContainerInstance spotContainerInstance1 = new ContainerInstance().withEc2InstanceId("spot_1").withRunningTasksCount(0).withPendingTasksCount(0);

            when(containerInstanceHelper.getAllInstances(pluginSettings)).thenReturn(asList(idleForLong));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(spotContainerInstance1));
            when(pluginSettings.terminateIdleLinuxSpotInstanceAfter()).thenReturn(Period.minutes(20));
            when(pluginSettings.terminateIdleWindowsSpotInstanceAfter()).thenReturn(Period.minutes(20));

            List<Instance> idleSpotInstances = spotInstanceHelper.getIdleInstancesEligibleForTermination(pluginSettings, "gocd");

            assertThat(idleSpotInstances.size()).isEqualTo(0);
        }
    }

    @Nested
    class allRegisteredSpotInstancesForPlatform {
        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldListAllSpotInstancesWhichAreRegisteredWithACluster(Platform platform) {
            ContainerInstance spotContainerInstance = new ContainerInstance().withEc2InstanceId("spot");
            ContainerInstance onDemandContainerInstance = new ContainerInstance().withEc2InstanceId("on-demand");
            List<ContainerInstance> containerInstances = asList(spotContainerInstance, onDemandContainerInstance);
            Instance spotInstance = new Instance().withInstanceId("spot_1").withSpotInstanceRequestId("req_id1")
                    .withState(new InstanceState().withName(InstanceStateName.Running)).withPlatform(platform.name());
            Instance onDemandInstance = new Instance().withInstanceId("on_demand")
                    .withState(new InstanceState().withName(InstanceStateName.Running)).withPlatform(platform.name());

            when(containerInstanceHelper.getContainerInstances(pluginSettings)).thenReturn(containerInstances);
            when(containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstances)).thenReturn(asList(spotInstance, onDemandInstance));

            List<Instance> instances = spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, platform);

            assertThat(instances.size()).isEqualTo(1);
            assertThat(instances).contains(spotInstance);
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldListSpotInstancesWhichAreRunningOrPending(Platform platform) {
            ContainerInstance runningSpotContainerInstance = new ContainerInstance().withEc2InstanceId("spot_running");
            ContainerInstance pendingSpotContainerInstance = new ContainerInstance().withEc2InstanceId("spot_pending");
            ContainerInstance terminatedSpotContainerInstance = new ContainerInstance().withEc2InstanceId("spot_terminated");

            List<ContainerInstance> containerInstances = asList(runningSpotContainerInstance, pendingSpotContainerInstance, terminatedSpotContainerInstance);
            Instance runningInstance = new Instance().withInstanceId("spot_running").withSpotInstanceRequestId("req_id1")
                    .withState(new InstanceState().withName(InstanceStateName.Running)).withPlatform(platform.name());
            Instance pendingInstance = new Instance().withInstanceId("spot_pending").withSpotInstanceRequestId("req_id2")
                    .withState(new InstanceState().withName(InstanceStateName.Pending)).withPlatform(platform.name());
            Instance terminatedInstance = new Instance().withInstanceId("spot_terminated").withSpotInstanceRequestId("req_id3")
                    .withState(new InstanceState().withName(InstanceStateName.Terminated)).withPlatform(platform.name());


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
        return new Filter().withName(name).withValues(values);
    }
}
