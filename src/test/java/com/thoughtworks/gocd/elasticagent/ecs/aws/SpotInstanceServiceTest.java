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

import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.SpotRequestMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.TerminateOperation;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ConsoleLogAppender;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.LimitExceededException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.InOrder;
import org.mockito.Mock;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.spotInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.openMocks;

public class SpotInstanceServiceTest {

    private SpotInstanceService service;
    @Mock
    private PluginSettings pluginSettings;
    @Mock
    private SpotInstanceHelper spotInstanceHelper;
    @Mock
    private ElasticAgentProfileProperties elasticAgentProfileProperties;
    @Mock
    private ConsoleLogAppender consoleLogAppender;
    @Mock
    private EC2Config ec2Config;
    @Mock
    private EC2Config.Builder configBuilder;
    @Mock
    private ContainerInstanceHelper containerInstanceHelper;
    @Mock
    private TerminateOperation terminateOperation;
    @Mock
    private SpotRequestMatcher spotRequestMatcher;

    @BeforeEach
    void setUp() {
        openMocks(this);

        service = new SpotInstanceService(spotInstanceHelper, configBuilder, containerInstanceHelper, terminateOperation, spotRequestMatcher);
    }

    @Nested
    class create {
        @Test
        void shouldRequestForASpotInstance() throws LimitExceededException {

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any()))
                    .thenReturn(RequestSpotInstancesResponse.builder().spotInstanceRequests(SpotInstanceRequest.builder().spotInstanceRequestId("spot_req_id").state("open").status(SpotInstanceStatus.builder().build()).build()).build());

            Optional<ContainerInstance> containerInstance = service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper).requestSpotInstanceRequest(pluginSettings, ec2Config);
            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldNotRequestForASpotInstanceIfAOpenSpotRequestMatchingTheProfileExists() throws LimitExceededException {
            SpotInstanceRequest openSpotInstanceRequest = SpotInstanceRequest.builder().spotInstanceRequestId("spot_req_id").state("open").build();

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(any(), any(), any())).thenReturn(Stream.of(openSpotInstanceRequest));
            when(spotRequestMatcher.matches(ec2Config, openSpotInstanceRequest)).thenReturn(true);

            Optional<ContainerInstance> containerInstance = service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper, never()).requestSpotInstanceRequest(any(), any());
            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldNotRequestForASpotInstanceIfAUnTaggedSpotRequestMatchingTheProfileExists() throws LimitExceededException {
            SpotInstanceRequest unTaggedSpotRequest = SpotInstanceRequest.builder().spotInstanceRequestId("spot_req_id")
                    .state("open").status(SpotInstanceStatus.builder().build()).build();

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(any(), any(), any())).thenReturn(Stream.empty(), Stream.empty());
            // the service stores a rebuilt copy of the request (with the platform tag added), so match any request
            when(spotRequestMatcher.matches(eq(ec2Config), any(SpotInstanceRequest.class))).thenReturn(true);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any()))
                    .thenReturn(RequestSpotInstancesResponse.builder().spotInstanceRequests(unTaggedSpotRequest).build());

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);
            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper, times(1)).requestSpotInstanceRequest(any(), any());
        }

        @Test
        void shouldWaitForSpotRequestToBeAbleToLookupByIdBeforeTagging() throws LimitExceededException {
            SpotInstanceRequest spotInstanceRequest = SpotInstanceRequest.builder().spotInstanceRequestId("spot_req_id").state("open").build();

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any()))
                    .thenReturn(RequestSpotInstancesResponse.builder().spotInstanceRequests(spotInstanceRequest).build());

            Optional<ContainerInstance> containerInstance = service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            InOrder inOrder = inOrder(spotInstanceHelper);
            inOrder.verify(spotInstanceHelper).waitTillSpotRequestCanBeLookedUpById(pluginSettings, spotInstanceRequest.spotInstanceRequestId());
            inOrder.verify(spotInstanceHelper).tagSpotResources(pluginSettings, List.of("spot_req_id"), LINUX);
            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldCancelTheSpotRequestIfTaggingItFails() {
            SpotInstanceRequest spotInstanceRequest = SpotInstanceRequest.builder().spotInstanceRequestId("spot_req_id").state("open").build();

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any()))
                    .thenReturn(RequestSpotInstancesResponse.builder().spotInstanceRequests(spotInstanceRequest).build());
            doThrow(new RuntimeException()).when(spotInstanceHelper).tagSpotResources(pluginSettings, List.of("spot_req_id"), LINUX);

            assertThrows(RuntimeException.class,
                    () -> service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender));

            verify(spotInstanceHelper).cancelSpotInstanceRequest(pluginSettings, "spot_req_id");
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldTagSpotRequestsPostSpotRequestCreation(Platform platform) throws LimitExceededException {
            SpotInstanceRequest spotInstanceRequest = SpotInstanceRequest.builder().spotInstanceRequestId("spot_req_id").state("open").status(SpotInstanceStatus.builder().build()).build();

            when(elasticAgentProfileProperties.platform()).thenReturn(platform);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any()))
                    .thenReturn(RequestSpotInstancesResponse.builder().spotInstanceRequests(spotInstanceRequest).build());

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper).tagSpotResources(pluginSettings, List.of("spot_req_id"), platform);
        }

        @Test
        void shouldErrorOutIfClusterIsMaxedOut() {

            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, LINUX)).thenReturn(asList(Instance.builder().build(), Instance.builder().build()));

            assertThrows(LimitExceededException.class,
                    () -> service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender));

            verify(spotInstanceHelper, never()).requestSpotInstanceRequest(any(), any());
        }

        @Test
        void shouldConsiderAllOpenOrSpotRequestsWithRunningUnRegisteredInstancesToComputeClusterSize() {
            int maxLinuxSpotInstanceAllowed = 4;

            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(maxLinuxSpotInstanceAllowed);
            when(spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, LINUX))
                    .thenReturn(asList(Instance.builder().instanceId("id1").build(), Instance.builder().instanceId("id2").build()));
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, pluginSettings.getClusterName(), LINUX))
                    .thenReturn(Stream.of(SpotInstanceRequest.builder().instanceId("new_1").build(), SpotInstanceRequest.builder().instanceId("new_2").build()));

            assertThrows(LimitExceededException.class,
                    () -> service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender));

            verify(spotInstanceHelper, never()).requestSpotInstanceRequest(any(), any());
        }

        @Test
        void shouldNotConsiderActiveOrCancelledSpotRequestsWithRegisteredInstancesToComputeClusterSize() throws LimitExceededException {
            int maxLinuxSpotInstanceAllowed = 3;

            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(maxLinuxSpotInstanceAllowed);
            when(spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, LINUX))
                    .thenReturn(asList(Instance.builder().instanceId("id1").build(), Instance.builder().instanceId("id2").build()));
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, pluginSettings.getClusterName(), LINUX))
                    .thenReturn(Stream.of(SpotInstanceRequest.builder().instanceId("id1").build(), SpotInstanceRequest.builder().instanceId("id1").build()));
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any()))
                    .thenReturn(RequestSpotInstancesResponse.builder().spotInstanceRequests(SpotInstanceRequest.builder().spotInstanceRequestId("spot_req_id").state("open").status(SpotInstanceStatus.builder().build()).build()).build());
            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper).requestSpotInstanceRequest(any(), any());
        }

        @Test
        void shouldConsiderUnTaggedSpotRequestsToComputeClusterSize() throws LimitExceededException {
            int maxLinuxSpotInstanceAllowed = 1;

            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(maxLinuxSpotInstanceAllowed);
            when(spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, LINUX)).thenReturn(emptyList());
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, pluginSettings.getClusterName(), LINUX)).thenReturn(Stream.empty(), Stream.empty());
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any()))
                    .thenReturn(RequestSpotInstancesResponse.builder().spotInstanceRequests(SpotInstanceRequest.builder().spotInstanceRequestId("spot_req_id").state("open").status(SpotInstanceStatus.builder().build()).build()).build());

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            assertThrows(LimitExceededException.class,
                    () -> service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender));
        }

        @Test
        void shouldRefreshUnTaggedSpotRequestsList() throws LimitExceededException {
            int maxLinuxSpotInstanceAllowed = 1;
            SpotInstanceRequest spotInstanceRequest = SpotInstanceRequest.builder().spotInstanceRequestId("spot_req_id").state("open").status(SpotInstanceStatus.builder().build()).build();

            when(configBuilder.settings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.profile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(maxLinuxSpotInstanceAllowed);
            when(spotInstanceHelper.getAllSpotRequestsForCluster(pluginSettings))
                    .thenReturn(emptyList())
                    .thenReturn(singletonList(spotInstanceRequest));
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any()))
                    .thenReturn(RequestSpotInstancesResponse.builder().spotInstanceRequests(spotInstanceRequest).build());

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper, times(2)).requestSpotInstanceRequest(any(), any());
        }
    }

    @Nested
    class tagSpotInstances {
        @Test
        void shouldTagSpotInstances() {
            SpotInstanceRequest req1 = SpotInstanceRequest.builder().tags(tag("Creator", "com.tw.ecs"), tag("platform", "LINUX")).instanceId("1").build();
            SpotInstanceRequest req2 = SpotInstanceRequest.builder().tags(tag("Creator", "com.tw.ecs"), tag("platform", "WINDOWS")).instanceId("2").build();
            SpotInstanceRequest req3 = SpotInstanceRequest.builder().tags(tag("Creator", "com.tw.ecs"), tag("platform", "LINUX")).instanceId("3").build();

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd")).thenReturn(asList(req1, req2, req3));

            service.tagSpotInstances(pluginSettings);

            verify(spotInstanceHelper).tagSpotResources(pluginSettings, asList("1", "3"), LINUX);
            verify(spotInstanceHelper).tagSpotResources(pluginSettings, List.of("2"), WINDOWS);
        }
    }

    @Nested
    class tagIdleSpotInstances {
        @Test
        void shouldTagAIdleSpotInstance() {
            Instance idleSpotInstance1 = Instance.builder().instanceId("spot_1").build();
            Instance idleSpotInstance2 = Instance.builder().instanceId("spot_2").build();

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getAllIdleSpotInstances(pluginSettings, "gocd")).thenReturn(asList(idleSpotInstance1, idleSpotInstance2));

            service.tagIdleSpotInstances(pluginSettings);

            verify(spotInstanceHelper).tagSpotInstancesAsIdle(pluginSettings, asList("spot_1", "spot_2"));
        }

        @Test
        void shouldNotTagIdleInstancesWithExistingIdleTag() {
            Instance idleSpotInstance1 = Instance.builder().instanceId("spot_1").build();
            Instance idleSpotInstance2 = Instance.builder().instanceId("spot_2").tags(Tag.builder().key("LAST_SEEN_IDLE").value("timestamp").build()).build();

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getAllIdleSpotInstances(pluginSettings, "gocd")).thenReturn(asList(idleSpotInstance1, idleSpotInstance2));

            service.tagIdleSpotInstances(pluginSettings);

            verify(spotInstanceHelper).tagSpotInstancesAsIdle(pluginSettings, List.of("spot_1"));
        }

        @Test
        void shouldDoNothingInAbsenceOfSpotInstances() {
            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getAllIdleSpotInstances(pluginSettings, "gocd")).thenReturn(emptyList());

            service.tagIdleSpotInstances(pluginSettings);

            verify(spotInstanceHelper, never()).tagSpotInstancesAsIdle(any(), any());
        }
    }

    @Nested
    class terminateIdleSpotInstances {
        @Test
        void shouldTerminateIdleSpotInstances() {
            Instance idleSpotInstance1 = spotInstance("spot_1", InstanceStateName.RUNNING, "LINUX");
            Instance idleSpotInstance2 = spotInstance("spot_2", InstanceStateName.RUNNING, "LINUX");

            ContainerInstance containerInstance1 = ContainerInstance.builder().ec2InstanceId("spot_1").build();
            ContainerInstance containerInstance2 = ContainerInstance.builder().ec2InstanceId("spot_2").build();

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getIdleInstancesEligibleForTermination(pluginSettings, "gocd")).thenReturn(asList(idleSpotInstance1, idleSpotInstance2));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(containerInstance1, containerInstance2));

            service.terminateIdleSpotInstances(pluginSettings);

            verify(terminateOperation).execute(pluginSettings, asList(containerInstance1, containerInstance2));
        }

        @Test
        void shouldNotTerminateUnRegisteredIdleSpotInstances() {
            Instance idleSpotInstance1 = spotInstance("spot_1", InstanceStateName.RUNNING, "LINUX");
            Instance idleSpotInstance2 = spotInstance("spot_2", InstanceStateName.RUNNING, "LINUX");

            ContainerInstance containerInstance1 = ContainerInstance.builder().ec2InstanceId("spot_1").build();
            ContainerInstance runningSpotInstance = ContainerInstance.builder().ec2InstanceId("spot_3").build();

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getIdleInstancesEligibleForTermination(pluginSettings, "gocd")).thenReturn(asList(idleSpotInstance1, idleSpotInstance2));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(containerInstance1, runningSpotInstance));

            service.terminateIdleSpotInstances(pluginSettings);

            verify(terminateOperation).execute(pluginSettings, singletonList(containerInstance1));
        }
    }

    private Tag tag(String key, String value) {
        return Tag.builder().key(key).value(value).build();
    }
}
