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

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin;
import com.thoughtworks.gocd.elasticagent.ecs.aws.predicate.SpotInstanceEligibleForTerminationPredicate;
import com.thoughtworks.gocd.elasticagent.ecs.aws.wait.Poller;
import com.thoughtworks.gocd.elasticagent.ecs.aws.wait.Result;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.*;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper.*;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.InstanceSelectionStrategy.ACCEPTABLE_STATES;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.SpotRequestStatus.REQUEST_CANCELLED_INSTANCE_RUNNING;
import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.toMap;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.amazon.awssdk.services.ec2.model.SpotInstanceState.*;

public class SpotInstanceHelper {
    public static final String SPOT_INSTANCE_NAME_FORMAT = "%s_%s_SPOT_INSTANCE";
    private static final Logger LOG = Logger.getLoggerFor(SpotInstanceHelper.class);

    private final SpotInstanceRequestBuilder spotInstanceRequestBuilder;
    private final SubnetSelector subnetSelector;
    private final Supplier<String> serverIdSupplier;
    private final ContainerInstanceHelper containerInstanceHelper;
    private final Duration spotRequestVisibilityTimeout;

    public SpotInstanceHelper() {
        this(new ContainerInstanceHelper(), new SpotInstanceRequestBuilder(), new SubnetSelector(), ECSElasticPlugin::getServerId, Duration.ofSeconds(25));
    }

    protected SpotInstanceHelper(ContainerInstanceHelper spotInstanceRequestBuilder, SpotInstanceRequestBuilder containerInstanceHelper,
                                 SubnetSelector subnetSelector, Supplier<String> serverIdSupplier, Duration spotRequestVisibilityTimeout) {
        this.containerInstanceHelper = spotInstanceRequestBuilder;
        this.spotInstanceRequestBuilder = containerInstanceHelper;
        this.subnetSelector = subnetSelector;
        this.serverIdSupplier = serverIdSupplier;
        this.spotRequestVisibilityTimeout = spotRequestVisibilityTimeout;
    }

    public RequestSpotInstancesResponse requestSpotInstanceRequest(PluginSettings pluginSettings, EC2Config ec2Config) {
        List<Instance> allInstances = containerInstanceHelper.getAllInstances(pluginSettings);

        Subnet subnet = subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, ec2Config.getSubnetIds(), allInstances);

        RequestSpotInstancesRequest requestSpotInstancesRequest = spotInstanceRequestBuilder.eC2Config(ec2Config).subnet(subnet).build();

        return pluginSettings.ec2Client().requestSpotInstances(requestSpotInstancesRequest);
    }

    public List<Instance> getAllSpotInstances(PluginSettings pluginSettings, String clusterName, Platform platform) {
        List<Instance> allInstances = containerInstanceHelper.getAllInstances(pluginSettings);

        List<Instance> allSpotInstances = filterSpotInstances(allInstances);

        final List<Instance> instances = filterBy(allSpotInstances, hasTag("Name", spotInstanceName(clusterName, platform.name())));

        return filterByPlatform(instances, platform);
    }

    public List<Instance> getAllIdleSpotInstances(PluginSettings pluginSettings, String clusterName) {
        List<Instance> allInstances = containerInstanceHelper.getAllInstances(pluginSettings);

        List<Instance> allSpotInstances = filterSpotInstances(allInstances);

        List<Instance> spotInstancesInCluster = filterBy(allSpotInstances, hasTag("cluster-name", clusterName));

        final List<ContainerInstance> containerInstances = containerInstanceHelper.spotContainerInstances(pluginSettings);

        Map<String, ContainerInstance> containerInstanceMap = toMap(containerInstances, ContainerInstance::ec2InstanceId, containerInstance -> containerInstance);

        if (containerInstances.isEmpty()) {
            return emptyList();
        }

        return spotInstancesInCluster.stream()
                .filter(instance -> containerInstanceMap.containsKey(instance.instanceId()))
                .filter(instance -> isIdle(containerInstanceMap.get(instance.instanceId())))
                .collect(toList());
    }

    public List<Instance> getIdleInstancesEligibleForTermination(PluginSettings pluginSettings, String clusterName) {
        List<Instance> idleSpotInstances = getAllIdleSpotInstances(pluginSettings, clusterName);

        return idleSpotInstances.stream()
                .filter(new SpotInstanceEligibleForTerminationPredicate(pluginSettings))
                .collect(toList());
    }

    public Stream<SpotInstanceRequest> getAllOpenOrSpotRequestsWithRunningInstances(PluginSettings pluginSettings, String clusterName, Platform platform) {
        DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest = DescribeSpotInstanceRequestsRequest.builder()
                .filters(
                        Filter.builder().name("state").values(OPEN.toString(), ACTIVE.toString(), CANCELLED.toString()).build(),
                        Filter.builder().name("tag:Creator").values(PLUGIN_ID).build(),
                        Filter.builder().name("tag:cluster-name").values(clusterName).build(),
                        Filter.builder().name("tag:platform").values(platform.name()).build(),
                        Filter.builder().name("tag:server-id").values(serverIdSupplier.get()).build()
                ).build();

        DescribeSpotInstanceRequestsResponse describeSpotInstanceRequestsResult = pluginSettings.ec2Client().describeSpotInstanceRequests(describeSpotInstanceRequestsRequest);

        return describeSpotInstanceRequestsResult.spotInstanceRequests()
                .stream()
                .filter(spotInstanceRequest -> !CANCELLED.equals(spotInstanceRequest.state()) || REQUEST_CANCELLED_INSTANCE_RUNNING.equals(spotInstanceRequest.status().code()));
    }

    public List<SpotInstanceRequest> getAllSpotRequestsForCluster(PluginSettings pluginSettings) {
        DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest = DescribeSpotInstanceRequestsRequest.builder()
                .filters(
                        Filter.builder().name("tag:Creator").values(PLUGIN_ID).build(),
                        Filter.builder().name("tag:cluster-name").values(pluginSettings.getClusterName()).build(),
                        Filter.builder().name("tag:server-id").values(serverIdSupplier.get()).build()
                ).build();

        return pluginSettings.ec2Client()
                .describeSpotInstanceRequests(describeSpotInstanceRequestsRequest)
                .spotInstanceRequests();
    }

    public List<Instance> allRegisteredSpotInstancesForPlatform(PluginSettings pluginSettings, Platform platform) {
        List<ContainerInstance> containerInstanceList = containerInstanceHelper.getContainerInstances(pluginSettings);

        final List<Instance> registeredSpotInstances = containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstanceList).stream()
                .filter(instance -> isNotBlank(instance.spotInstanceRequestId()))
                .filter(instance -> ACCEPTABLE_STATES.contains(instance.state().name()))
                .collect(toList());

        return filterByPlatform(registeredSpotInstances, platform);
    }

    public void tagSpotResources(PluginSettings pluginSettings, List<String> resources, Platform platform) {
        CreateTagsRequest createTagsRequest = CreateTagsRequest.builder()
                .resources(resources)
                .tags(
                        Tag.builder().key(LABEL_SERVER_ID).value(serverIdSupplier.get()).build(),
                        Tag.builder().key("Creator").value(PLUGIN_ID).build(),
                        Tag.builder().key("cluster-name").value(pluginSettings.getClusterName()).build(),
                        Tag.builder().key("platform").value(platform.name()).build(),
                        Tag.builder().key("Name").value(format(SPOT_INSTANCE_NAME_FORMAT, pluginSettings.getClusterName(), platform.name())).build()
                )
                .build();

        pluginSettings.ec2Client().createTags(createTagsRequest);
    }

    public boolean waitTillSpotRequestCanBeLookedUpById(PluginSettings pluginSettings, String spotInstanceRequestId) {
        final Result<DescribeSpotInstanceRequestsResponse> result = new Poller<DescribeSpotInstanceRequestsResponse>()
                .timeout(spotRequestVisibilityTimeout)
                .stopWhen(Objects::nonNull)
                .poll(getSpotRequest(pluginSettings, spotInstanceRequestId))
                .await();

        if (result.isFailed()) {
            LOG.debug("[create-agent] Unable to lookup for Spot Request with id: '{}', hence cancelling the spot request.", spotInstanceRequestId);
            cancelSpotInstanceRequest(pluginSettings, spotInstanceRequestId);

            throw new RuntimeException(format("Unable to lookup for SpotRequest with id: '%s'", spotInstanceRequestId));
        }

        return true;
    }

    public void cancelSpotInstanceRequest(PluginSettings pluginSettings, String spotInstanceRequestId) {
        CancelSpotInstanceRequestsRequest cancelSpotInstanceRequestsRequest = CancelSpotInstanceRequestsRequest.builder()
                .spotInstanceRequestIds(spotInstanceRequestId)
                .build();
        pluginSettings.ec2Client().cancelSpotInstanceRequests(cancelSpotInstanceRequestsRequest);
    }

    private Supplier<DescribeSpotInstanceRequestsResponse> getSpotRequest(PluginSettings pluginSettings, String spotInstanceRequestId) {
        return () -> {
            DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest = DescribeSpotInstanceRequestsRequest.builder()
                    .spotInstanceRequestIds(spotInstanceRequestId)
                    .build();
            DescribeSpotInstanceRequestsResponse describeSpotInstanceRequestsResult = null;

            try {
                describeSpotInstanceRequestsResult = pluginSettings.ec2Client().describeSpotInstanceRequests(describeSpotInstanceRequestsRequest);
            } catch (Exception e) {
                LOG.error("[create-agent] Error fetching spot request with id: '{}', reason: '{}'", spotInstanceRequestId, e.getMessage());
            }

            return describeSpotInstanceRequestsResult;
        };
    }

    public List<SpotInstanceRequest> getSpotRequestsWithARunningSpotInstance(PluginSettings pluginSettings, String clusterName) {
        DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest = DescribeSpotInstanceRequestsRequest.builder()
                .filters(
                        Filter.builder().name("state").values(ACTIVE.toString(), CANCELLED.toString()).build(),
                        Filter.builder().name("tag:Creator").values(PLUGIN_ID).build(),
                        Filter.builder().name("tag:cluster-name").values(clusterName).build(),
                        Filter.builder().name("tag:server-id").values(serverIdSupplier.get()).build()
                )
                .build();

        DescribeSpotInstanceRequestsResponse describeSpotInstanceRequestsResult = pluginSettings.ec2Client().describeSpotInstanceRequests(describeSpotInstanceRequestsRequest);

        return describeSpotInstanceRequestsResult.spotInstanceRequests().stream()
                .filter(sr -> isActive(sr) || isCancelledWithRunningInstance(sr))
                .collect(toList());
    }

    public void tagSpotInstancesAsIdle(PluginSettings pluginSettings, List<String> instanceIds) {
        CreateTagsRequest createTagsRequest = CreateTagsRequest.builder()
                .resources(instanceIds)
                .tags(
                        Tag.builder().key(LAST_SEEN_IDLE).value(valueOf(currentTimeMillis())).build()
                )
                .build();

        pluginSettings.ec2Client().createTags(createTagsRequest);
    }

    private boolean isIdle(ContainerInstance containerInstance) {
        return containerInstance.pendingTasksCount() == 0 && containerInstance.runningTasksCount() == 0;
    }

    private List<Instance> filterSpotInstances(List<Instance> allInstances) {
        return allInstances.stream()
                .filter(instance -> isNotBlank(instance.spotInstanceRequestId()))
                .collect(toList());
    }

    private String spotInstanceName(String clusterName, String platform) {
        return format(SPOT_INSTANCE_NAME_FORMAT, clusterName, platform);
    }

    private boolean isCancelledWithRunningInstance(SpotInstanceRequest sr) {
        return sr.state().equals(CANCELLED) && sr.status().code().equals(REQUEST_CANCELLED_INSTANCE_RUNNING);
    }

    private boolean isActive(SpotInstanceRequest spotInstanceRequest) {
        return spotInstanceRequest.state().equals(ACTIVE);
    }
}
