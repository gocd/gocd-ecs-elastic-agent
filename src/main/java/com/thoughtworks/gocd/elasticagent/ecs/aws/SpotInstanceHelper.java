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
import com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin;
import com.thoughtworks.gocd.elasticagent.ecs.aws.predicate.SpotInstanceEligibleForTerminationPredicate;
import com.thoughtworks.gocd.elasticagent.ecs.aws.wait.Poller;
import com.thoughtworks.gocd.elasticagent.ecs.aws.wait.Result;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ConsoleLogAppender;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.joda.time.Period;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.*;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.PENDING;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.RUNNING;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.SpotRequestState.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.SpotRequestStatus.REQUEST_CANCELLED_INSTANCE_RUNNING;
import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.toMap;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.lang.System.currentTimeMillis;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class SpotInstanceHelper {
    private final SpotInstanceRequestBuilder spotInstanceRequestBuilder;
    private final SubnetSelector subnetSelector;
    private final Supplier<String> serverIdSupplier;
    private final ContainerInstanceHelper containerInstanceHelper;
    public static final String SPOT_INSTANCE_NAME_FORMAT = "%s_%s_SPOT_INSTANCE";
    private static final Set<String> ACCEPTABLE_STATES = Stream.of(PENDING, RUNNING).collect(toSet());
    private Period spotRequestVisibilityTimeout;

    public SpotInstanceHelper() {
        this(new ContainerInstanceHelper(), new SpotInstanceRequestBuilder(), new SubnetSelector(), ECSElasticPlugin::getServerId, Period.seconds(25));
    }

    protected SpotInstanceHelper(ContainerInstanceHelper spotInstanceRequestBuilder, SpotInstanceRequestBuilder containerInstanceHelper,
                                 SubnetSelector subnetSelector, Supplier<String> serverIdSupplier, Period spotRequestVisibilityTimeout) {
        this.containerInstanceHelper = spotInstanceRequestBuilder;
        this.spotInstanceRequestBuilder = containerInstanceHelper;
        this.subnetSelector = subnetSelector;
        this.serverIdSupplier = serverIdSupplier;
        this.spotRequestVisibilityTimeout = spotRequestVisibilityTimeout;
    }

    public RequestSpotInstancesResult requestSpotInstanceRequest(PluginSettings pluginSettings, EC2Config ec2Config, ConsoleLogAppender consoleLogAppender) {
        List<Instance> allInstances = containerInstanceHelper.getAllInstances(pluginSettings);

        Subnet subnet = subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, ec2Config.getSubnetIds(), allInstances);

        RequestSpotInstancesRequest requestSpotInstancesRequest = spotInstanceRequestBuilder.withEC2Config(ec2Config).withSubnet(subnet).build();

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

        Map<String, ContainerInstance> containerInstanceMap = toMap(containerInstances, ContainerInstance::getEc2InstanceId, containerInstance -> containerInstance);

        if (containerInstances.isEmpty()) {
            return emptyList();
        }

        return spotInstancesInCluster.stream()
                .filter(instance -> containerInstanceMap.containsKey(instance.getInstanceId()))
                .filter(instance -> isIdle(containerInstanceMap.get(instance.getInstanceId())))
                .collect(toList());
    }

    public List<Instance> getIdleInstancesEligibleForTermination(PluginSettings pluginSettings, String clusterName) {
        List<Instance> idleSpotInstances = getAllIdleSpotInstances(pluginSettings, clusterName);

        return idleSpotInstances.stream()
                .filter(new SpotInstanceEligibleForTerminationPredicate(pluginSettings))
                .collect(toList());
    }

    public List<SpotInstanceRequest> getAllOpenOrSpotRequestsWithRunningInstances(PluginSettings pluginSettings, String clusterName, Platform platform) {
        DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest = new DescribeSpotInstanceRequestsRequest()
                .withFilters(
                        new Filter().withName("state").withValues(OPEN, ACTIVE, CANCELLED),
                        new Filter().withName("tag:Creator").withValues(PLUGIN_ID),
                        new Filter().withName("tag:cluster-name").withValues(clusterName),
                        new Filter().withName("tag:platform").withValues(platform.name()),
                        new Filter().withName("tag:server-id").withValues(serverIdSupplier.get())
                );

        DescribeSpotInstanceRequestsResult describeSpotInstanceRequestsResult = pluginSettings.ec2Client().describeSpotInstanceRequests(describeSpotInstanceRequestsRequest);

        List<SpotInstanceRequest> allOpenSpotRequests = describeSpotInstanceRequestsResult.getSpotInstanceRequests();

        allOpenSpotRequests.removeIf(spotInstanceRequest -> {
            if (CANCELLED.equals(spotInstanceRequest.getState())) {
                return !REQUEST_CANCELLED_INSTANCE_RUNNING.equals(spotInstanceRequest.getStatus().getCode());
            }
            return false;
        });

        return allOpenSpotRequests;
    }

    public List<SpotInstanceRequest> getAllSpotRequestsForCluster(PluginSettings pluginSettings) {
        DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest = new DescribeSpotInstanceRequestsRequest()
                .withFilters(
                        new Filter().withName("tag:Creator").withValues(PLUGIN_ID),
                        new Filter().withName("tag:cluster-name").withValues(pluginSettings.getClusterName()),
                        new Filter().withName("tag:server-id").withValues(serverIdSupplier.get())
                );

        DescribeSpotInstanceRequestsResult describeSpotInstanceRequestsResult = pluginSettings.ec2Client().describeSpotInstanceRequests(describeSpotInstanceRequestsRequest);

        return describeSpotInstanceRequestsResult.getSpotInstanceRequests();
    }

    public List<Instance> allRegisteredSpotInstancesForPlatform(PluginSettings pluginSettings, Platform platform) {
        List<ContainerInstance> containerInstanceList = containerInstanceHelper.getContainerInstances(pluginSettings);

        final List<Instance> registeredSpotInstances = containerInstanceHelper.ec2InstancesFromContainerInstances(pluginSettings, containerInstanceList).stream()
                .filter(instance -> isNotBlank(instance.getSpotInstanceRequestId()))
                .filter(instance -> ACCEPTABLE_STATES.contains(instance.getState().getName().toLowerCase()))
                .collect(toList());

        return filterByPlatform(registeredSpotInstances, platform);
    }

    public void tagSpotResources(PluginSettings pluginSettings, List<String> resources, Platform platform) {
        CreateTagsRequest createTagsRequest = new CreateTagsRequest()
                .withResources(resources)
                .withTags(
                        new Tag().withKey(LABEL_SERVER_ID).withValue(serverIdSupplier.get()),
                        new Tag().withKey("Creator").withValue(PLUGIN_ID),
                        new Tag().withKey("cluster-name").withValue(pluginSettings.getClusterName()),
                        new Tag().withKey("platform").withValue(platform.name()),
                        new Tag().withKey("Name").withValue(format(SPOT_INSTANCE_NAME_FORMAT, pluginSettings.getClusterName(), platform.name()))
                );

        pluginSettings.ec2Client().createTags(createTagsRequest);
    }

    public boolean waitTillSpotRequestCanBeLookedUpById(PluginSettings pluginSettings, String spotInstanceRequestId) {
        final Result<DescribeSpotInstanceRequestsResult> result = new Poller<DescribeSpotInstanceRequestsResult>()
                .timeout(spotRequestVisibilityTimeout)
                .stopWhen(describeSpotInstanceRequestsResult -> describeSpotInstanceRequestsResult != null)
                .poll(getSpotRequest(pluginSettings, spotInstanceRequestId))
                .start();

        if (result.isFailed()) {
            LOG.debug("[create-agent] Unable to lookup for Spot Request with id: '{}', hence cancelling the spot request.", spotInstanceRequestId);
            cancelSpotInstanceRequest(pluginSettings, spotInstanceRequestId);

            throw new RuntimeException(format("Unable to lookup for SpotRequest with id: '%s'", spotInstanceRequestId));
        }

        return true;
    }

    public void cancelSpotInstanceRequest(PluginSettings pluginSettings, String spotInstanceRequestId) {
        CancelSpotInstanceRequestsRequest cancelSpotInstanceRequestsRequest = new CancelSpotInstanceRequestsRequest()
                .withSpotInstanceRequestIds(spotInstanceRequestId);
        pluginSettings.ec2Client().cancelSpotInstanceRequests(cancelSpotInstanceRequestsRequest);
    }

    private Supplier<DescribeSpotInstanceRequestsResult> getSpotRequest(PluginSettings pluginSettings, String spotInstanceRequestId) {
        return () -> {
            DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest = new DescribeSpotInstanceRequestsRequest()
                    .withSpotInstanceRequestIds(spotInstanceRequestId);
            DescribeSpotInstanceRequestsResult describeSpotInstanceRequestsResult = null;

            try {
                describeSpotInstanceRequestsResult = pluginSettings.ec2Client().describeSpotInstanceRequests(describeSpotInstanceRequestsRequest);
            } catch (Exception e) {
                LOG.error("[create-agent] Error fetching spot request with id: '{}', reason: '{}'", spotInstanceRequestId, e.getMessage());
            }

            return describeSpotInstanceRequestsResult;
        };
    }

    public List<SpotInstanceRequest> getSpotRequestsWithARunningSpotInstance(PluginSettings pluginSettings, String clusterName) {
        DescribeSpotInstanceRequestsRequest describeSpotInstanceRequestsRequest = new DescribeSpotInstanceRequestsRequest()
                .withFilters(
                        new Filter().withName("state").withValues(ACTIVE, CANCELLED),
                        new Filter().withName("tag:Creator").withValues(PLUGIN_ID),
                        new Filter().withName("tag:cluster-name").withValues(clusterName),
                        new Filter().withName("tag:server-id").withValues(serverIdSupplier.get())
                );

        DescribeSpotInstanceRequestsResult describeSpotInstanceRequestsResult = pluginSettings.ec2Client().describeSpotInstanceRequests(describeSpotInstanceRequestsRequest);

        return describeSpotInstanceRequestsResult.getSpotInstanceRequests().stream()
                .filter(sr -> isActive(sr) || isCancelledWithRunningInstance(sr))
                .collect(toList());
    }

    public void tagSpotInstancesAsIdle(PluginSettings pluginSettings, List<String> instanceIds) {
        CreateTagsRequest createTagsRequest = new CreateTagsRequest()
                .withResources(instanceIds)
                .withTags(
                        new Tag().withKey(LAST_SEEN_IDLE).withValue(valueOf(currentTimeMillis()))
                );

        pluginSettings.ec2Client().createTags(createTagsRequest);
    }

    private boolean isIdle(ContainerInstance containerInstance) {
        return containerInstance.getPendingTasksCount() == 0 && containerInstance.getRunningTasksCount() == 0;
    }

    private List<Instance> filterSpotInstances(List<Instance> allInstances) {
        return allInstances.stream()
                .filter(instance -> isNotBlank(instance.getSpotInstanceRequestId()))
                .collect(toList());
    }

    private String spotInstanceName(String clusterName, String platform) {
        return format(SPOT_INSTANCE_NAME_FORMAT, clusterName, platform);
    }

    private boolean isCancelledWithRunningInstance(SpotInstanceRequest sr) {
        return sr.getState().equals(CANCELLED) && sr.getStatus().getCode().equals("request-canceled-and-instance-running");
    }

    private boolean isActive(SpotInstanceRequest spotInstanceRequest) {
        return spotInstanceRequest.getState().equals(ACTIVE);
    }
}
