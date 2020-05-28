/*
 * Copyright 2020 ThoughtWorks, Inc.
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

import com.amazonaws.services.ec2.model.*;
import com.amazonaws.services.ecs.model.ContainerInstance;
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

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.InstanceMother.spotInstance;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Java6Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;
import static org.mockito.MockitoAnnotations.initMocks;

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
        initMocks(this);

        service = new SpotInstanceService(spotInstanceHelper, configBuilder, containerInstanceHelper, terminateOperation, spotRequestMatcher);
    }

    @Nested
    class create {
        @Test
        void shouldRequestForASpotInstance() throws LimitExceededException {

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any(), any()))
                    .thenReturn(new RequestSpotInstancesResult().withSpotInstanceRequests(new SpotInstanceRequest().withSpotInstanceRequestId("spot_req_id").withState("open").withStatus(new SpotInstanceStatus())));

            Optional<ContainerInstance> containerInstance = service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper).requestSpotInstanceRequest(pluginSettings, ec2Config, consoleLogAppender);
            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldNotRequestForASpotInstanceIfAOpenSpotRequestMatchingTheProfileExists() throws LimitExceededException {
            SpotInstanceRequest openSpotInstanceRequest = new SpotInstanceRequest().withSpotInstanceRequestId("spot_req_id").withState("open");

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(any(), any(), any())).thenReturn(asList(openSpotInstanceRequest));
            when(spotRequestMatcher.matches(ec2Config, openSpotInstanceRequest)).thenReturn(true);

            Optional<ContainerInstance> containerInstance = service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper, never()).requestSpotInstanceRequest(any(), any(), any());
            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldNotRequestForASpotInstanceIfAUnTaggedSpotRequestMatchingTheProfileExists() throws LimitExceededException {
            SpotInstanceRequest unTaggedSpotRequest = new SpotInstanceRequest().withSpotInstanceRequestId("spot_req_id")
                    .withState("open").withStatus(new SpotInstanceStatus());

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(any(), any(), any())).thenReturn(emptyList());
            when(spotRequestMatcher.matches(ec2Config, unTaggedSpotRequest)).thenReturn(true);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any(), any()))
                    .thenReturn(new RequestSpotInstancesResult().withSpotInstanceRequests(unTaggedSpotRequest));

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);
            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper, times(1)).requestSpotInstanceRequest(any(), any(), any());
        }

        @Test
        void shouldWaitForSpotRequestToBeAbleToLookupByIdBeforeTagging() throws LimitExceededException {
            SpotInstanceRequest spotInstanceRequest = new SpotInstanceRequest().withSpotInstanceRequestId("spot_req_id").withState("open");

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any(), any()))
                    .thenReturn(new RequestSpotInstancesResult().withSpotInstanceRequests(spotInstanceRequest.withStatus(new SpotInstanceStatus())));

            Optional<ContainerInstance> containerInstance = service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            InOrder inOrder = inOrder(spotInstanceHelper);
            inOrder.verify(spotInstanceHelper).waitTillSpotRequestCanBeLookedUpById(pluginSettings, spotInstanceRequest.getSpotInstanceRequestId());
            inOrder.verify(spotInstanceHelper).tagSpotResources(pluginSettings, asList("spot_req_id"), LINUX);
            assertThat(containerInstance.isPresent()).isFalse();
        }

        @Test
        void shouldCancelTheSpotRequestIfTaggingItFails() throws LimitExceededException {
            SpotInstanceRequest spotInstanceRequest = new SpotInstanceRequest().withSpotInstanceRequestId("spot_req_id").withState("open");

            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.getAllSpotInstances(any(), any(), any())).thenReturn(emptyList());
            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any(), any()))
                    .thenReturn(new RequestSpotInstancesResult().withSpotInstanceRequests(spotInstanceRequest.withStatus(new SpotInstanceStatus())));
            doThrow(new RuntimeException()).when(spotInstanceHelper).tagSpotResources(pluginSettings, asList("spot_req_id"), LINUX);

            assertThrows(RuntimeException.class,
                    () -> service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender));

            verify(spotInstanceHelper).cancelSpotInstanceRequest(pluginSettings, "spot_req_id");
        }

        @ParameterizedTest
        @EnumSource(Platform.class)
        void shouldTagSpotRequestsPostSpotRequestCreation(Platform platform) throws LimitExceededException {
            SpotInstanceRequest spotInstanceRequest = new SpotInstanceRequest().withSpotInstanceRequestId("spot_req_id").withState("open").withStatus(new SpotInstanceStatus());

            when(elasticAgentProfileProperties.platform()).thenReturn(platform);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any(), any()))
                    .thenReturn(new RequestSpotInstancesResult().withSpotInstanceRequests(spotInstanceRequest));

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper).tagSpotResources(pluginSettings, asList("spot_req_id"), platform);
        }

        @Test
        void shouldErrorOutIfClusterIsMaxedOut() {

            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(2);
            when(spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, LINUX)).thenReturn(asList(new Instance(), new Instance()));

            assertThrows(LimitExceededException.class,
                    () -> service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender));

            verify(spotInstanceHelper, never()).requestSpotInstanceRequest(any(), any(), any());
        }

        @Test
        void shouldConsiderAllOpenOrSpotRequestsWithRunningUnRegisteredInstancesToComputeClusterSize() {
            int maxLinuxSpotInstanceAllowed = 4;

            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(maxLinuxSpotInstanceAllowed);
            when(spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, LINUX))
                    .thenReturn(asList(new Instance().withInstanceId("id1"), new Instance().withInstanceId("id2")));
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, pluginSettings.getClusterName(), LINUX))
                    .thenReturn(asList(new SpotInstanceRequest().withInstanceId("new_1"), new SpotInstanceRequest().withInstanceId("new_2")));

            assertThrows(LimitExceededException.class,
                    () -> service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender));

            verify(spotInstanceHelper, never()).requestSpotInstanceRequest(any(), any(), any());
        }

        @Test
        void shouldNotConsiderActiveOrCancelledSpotRequestsWithRegisteredInstancesToComputeClusterSize() throws LimitExceededException {
            int maxLinuxSpotInstanceAllowed = 3;

            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(maxLinuxSpotInstanceAllowed);
            when(spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, LINUX))
                    .thenReturn(asList(new Instance().withInstanceId("id1"), new Instance().withInstanceId("id2")));
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, pluginSettings.getClusterName(), LINUX))
                    .thenReturn(asList(new SpotInstanceRequest().withInstanceId("id1"), new SpotInstanceRequest().withInstanceId("id1")));
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any(), any()))
                    .thenReturn(new RequestSpotInstancesResult().withSpotInstanceRequests(new SpotInstanceRequest().withSpotInstanceRequestId("spot_req_id").withState("open").withStatus(new SpotInstanceStatus())));
            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper).requestSpotInstanceRequest(any(), any(), any());
        }

        @Test
        void shouldConsiderUnTaggedSpotRequestsToComputeClusterSize() throws LimitExceededException {
            int maxLinuxSpotInstanceAllowed = 1;

            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(maxLinuxSpotInstanceAllowed);
            when(spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, LINUX)).thenReturn(emptyList());
            when(spotInstanceHelper.getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, pluginSettings.getClusterName(), LINUX)).thenReturn(emptyList());
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any(), any()))
                    .thenReturn(new RequestSpotInstancesResult().withSpotInstanceRequests(new SpotInstanceRequest().withSpotInstanceRequestId("spot_req_id").withState("open").withStatus(new SpotInstanceStatus())));

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            assertThrows(LimitExceededException.class,
                    () -> service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender));
        }

        @Test
        void shouldRefreshUnTaggedSpotRequestsList() throws LimitExceededException {
            int maxLinuxSpotInstanceAllowed = 1;
            SpotInstanceRequest spotInstanceRequest = new SpotInstanceRequest().withSpotInstanceRequestId("spot_req_id").withState("open").withStatus(new SpotInstanceStatus());

            when(configBuilder.withSettings(pluginSettings)).thenReturn(configBuilder);
            when(configBuilder.withProfile(elasticAgentProfileProperties)).thenReturn(configBuilder);
            when(configBuilder.build()).thenReturn(ec2Config);
            when(elasticAgentProfileProperties.platform()).thenReturn(LINUX);
            when(pluginSettings.getMaxLinuxSpotInstanceAllowed()).thenReturn(maxLinuxSpotInstanceAllowed);
            when(spotInstanceHelper.getAllSpotRequestsForCluster(pluginSettings))
                    .thenReturn(emptyList())
                    .thenReturn(singletonList(spotInstanceRequest));
            when(spotInstanceHelper.requestSpotInstanceRequest(any(), any(), any()))
                    .thenReturn(new RequestSpotInstancesResult().withSpotInstanceRequests(spotInstanceRequest));

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            service.create(pluginSettings, elasticAgentProfileProperties, consoleLogAppender);

            verify(spotInstanceHelper, times(2)).requestSpotInstanceRequest(any(), any(), any());
        }
    }

    @Nested
    class tagSpotInstances {
        @Test
        void shouldTagSpotInstances() {
            SpotInstanceRequest req1 = new SpotInstanceRequest().withTags(tag("Creator", "com.tw.ecs"), tag("platform", "LINUX")).withInstanceId("1");
            SpotInstanceRequest req2 = new SpotInstanceRequest().withTags(tag("Creator", "com.tw.ecs"), tag("platform", "WINDOWS")).withInstanceId("2");
            SpotInstanceRequest req3 = new SpotInstanceRequest().withTags(tag("Creator", "com.tw.ecs"), tag("platform", "LINUX")).withInstanceId("3");

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, "gocd")).thenReturn(asList(req1, req2, req3));

            service.tagSpotInstances(pluginSettings);

            verify(spotInstanceHelper).tagSpotResources(pluginSettings, asList("1", "3"), LINUX);
            verify(spotInstanceHelper).tagSpotResources(pluginSettings, asList("2"), WINDOWS);
        }
    }

    @Nested
    class tagIdleSpotInstances {
        @Test
        void shouldTagAIdleSpotInstance() {
            Instance idleSpotInstance1 = new Instance().withInstanceId("spot_1");
            Instance idleSpotInstance2 = new Instance().withInstanceId("spot_2");

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getAllIdleSpotInstances(pluginSettings, "gocd")).thenReturn(asList(idleSpotInstance1, idleSpotInstance2));

            service.tagIdleSpotInstances(pluginSettings);

            verify(spotInstanceHelper).tagSpotInstancesAsIdle(pluginSettings, asList("spot_1", "spot_2"));
        }

        @Test
        void shouldNotTagIdleInstancesWithExistingIdleTag() {
            Instance idleSpotInstance1 = new Instance().withInstanceId("spot_1");
            Instance idleSpotInstance2 = new Instance().withInstanceId("spot_2").withTags(new Tag().withKey("LAST_SEEN_IDLE").withValue("timestamp"));

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getAllIdleSpotInstances(pluginSettings, "gocd")).thenReturn(asList(idleSpotInstance1, idleSpotInstance2));

            service.tagIdleSpotInstances(pluginSettings);

            verify(spotInstanceHelper).tagSpotInstancesAsIdle(pluginSettings, asList("spot_1"));
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
            Instance idleSpotInstance1 = spotInstance("spot_1", "running", "LINUX");
            Instance idleSpotInstance2 = spotInstance("spot_2", "running", "LINUX");

            ContainerInstance containerInstance1 = new ContainerInstance().withEc2InstanceId("spot_1");
            ContainerInstance containerInstance2 = new ContainerInstance().withEc2InstanceId("spot_2");

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getIdleInstancesEligibleForTermination(pluginSettings, "gocd")).thenReturn(asList(idleSpotInstance1, idleSpotInstance2));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(containerInstance1, containerInstance2));

            service.terminateIdleSpotInstances(pluginSettings);

            verify(terminateOperation).execute(pluginSettings, asList(containerInstance1, containerInstance2));
        }

        @Test
        void shouldNotTerminateUnRegisteredIdleSpotInstances() {
            Instance idleSpotInstance1 = spotInstance("spot_1", "running", "LINUX");
            Instance idleSpotInstance2 = spotInstance("spot_2", "running", "LINUX");

            ContainerInstance containerInstance1 = new ContainerInstance().withEc2InstanceId("spot_1");
            ContainerInstance runningSpotInstance = new ContainerInstance().withEc2InstanceId("spot_3");

            when(pluginSettings.getClusterName()).thenReturn("gocd");
            when(spotInstanceHelper.getIdleInstancesEligibleForTermination(pluginSettings, "gocd")).thenReturn(asList(idleSpotInstance1, idleSpotInstance2));
            when(containerInstanceHelper.spotContainerInstances(pluginSettings)).thenReturn(asList(containerInstance1, runningSpotInstance));

            service.terminateIdleSpotInstances(pluginSettings);

            verify(terminateOperation).execute(pluginSettings, asList(containerInstance1));
        }
    }

    private Tag tag(String key, String value) {
        return new Tag().withKey(key).withValue(value);
    }
}
