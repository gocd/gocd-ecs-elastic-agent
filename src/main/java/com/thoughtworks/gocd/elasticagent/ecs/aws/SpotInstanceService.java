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

import java.util.*;
import java.util.function.Predicate;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.SpotRequestState.ACTIVE;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.SpotRequestState.OPEN;
import static java.lang.String.format;
import static java.util.stream.Collectors.*;
import static org.apache.commons.collections4.CollectionUtils.union;
import static org.apache.commons.lang3.StringUtils.containsAny;


public class SpotInstanceService {
    private final SpotInstanceHelper spotInstanceHelper;
    private final EC2Config.Builder ec2ConfigBuilder;
    private final ContainerInstanceHelper containerInstanceHelper;
    private final TerminateOperation terminateOperation;
    private final SpotRequestMatcher spotRequestMatcher;
    private final Set<SpotInstanceRequest> untaggedSpotRequests = Collections.synchronizedSet(new HashSet<>());
    private static final SpotInstanceService spotInstanceService = new SpotInstanceService();

    private SpotInstanceService() {
        this(new SpotInstanceHelper(), new EC2Config.Builder(), new ContainerInstanceHelper(), new TerminateOperation(), new SpotRequestMatcher());
    }

    protected SpotInstanceService(SpotInstanceHelper spotInstanceHelper, EC2Config.Builder ec2ConfigBuilder,
                                  ContainerInstanceHelper containerInstanceHelper, TerminateOperation terminateOperation, SpotRequestMatcher spotRequestMatcher) {
        this.spotInstanceHelper = spotInstanceHelper;
        this.ec2ConfigBuilder = ec2ConfigBuilder;
        this.containerInstanceHelper = containerInstanceHelper;
        this.terminateOperation = terminateOperation;
        this.spotRequestMatcher = spotRequestMatcher;
    }

    public static SpotInstanceService instance() {
        return spotInstanceService;
    }

    public Optional<ContainerInstance> create(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, ConsoleLogAppender consoleLogAppender) throws LimitExceededException {
        Platform platform = elasticAgentProfileProperties.platform();
        synchronized (platform) {
            consoleLogAppender.accept("The elastic agent profile is configured to run on a spot instance. Initiating steps to request for a spot instance.");
            EC2Config ec2Config = ec2ConfigBuilder.withSettings(pluginSettings).withProfile(elasticAgentProfileProperties).build();
            List<Instance> allRegisteredSpotInstancesForPlatform = spotInstanceHelper.allRegisteredSpotInstancesForPlatform(pluginSettings, platform);

            synchronized (untaggedSpotRequests) {
                refreshUnTaggedSpotRequests(pluginSettings);

                List<SpotInstanceRequest> spotRequestsWithoutRegisteredInstances = spotRequestsWithoutRegisteredInstances(pluginSettings, platform, allRegisteredSpotInstancesForPlatform);

                Collection<SpotInstanceRequest> allSpotRequestsWithoutRunningInstance = union(spotRequestsWithoutRegisteredInstances, untaggedSpotRequests);

                LOG.info("[create-agent] For Platform: '{}',All-Registered-Spot-Instances count: '{}', Spot-Requests-Without-Registered-Instances count: '{}'," +
                                "UnTagged-Spot-Requests count: '{}'", platform.name(), allRegisteredSpotInstancesForPlatform.size(),
                        spotRequestsWithoutRegisteredInstances.size(), untaggedSpotRequests.size());

                boolean openSpotRequestAvailable = isThereAOpenSpotRequestMatchingProfile(allSpotRequestsWithoutRunningInstance, ec2Config);

                if (openSpotRequestAvailable) {
                    consoleLogAppender.accept("There is an open spot request matching the profile, not requesting for a new spot instance.");
                    LOG.debug("[create-agent] There is an open spot request matching the profile, not requesting for a new spot instance.");

                    return Optional.empty();
                }

                boolean clusterMaxedOut = (allRegisteredSpotInstancesForPlatform.size() + allSpotRequestsWithoutRunningInstance.size()) >= pluginSettings.getMaxLinuxSpotInstanceAllowed();

                if (clusterMaxedOut) {
                    consoleLogAppender.accept(format("The number of %s EC2 (spot instances running + open spot requests) is currently at the maximum permissible limit(%s). Not requesting for a spot instance."
                            , platform, pluginSettings.getMaxLinuxSpotInstanceAllowed()));

                    throw new LimitExceededException(format("The number of %s EC2 Spot Instances running is currently at the maximum permissible limit(%s). Not requesting for any more EC2 Spot Instances.",
                            platform.name(), pluginSettings.getMaxLinuxSpotInstanceAllowed()));
                }
            }

            LOG.debug("[create-agent] Initiating a new spot instance request.");
            RequestSpotInstancesResult requestSpotInstancesResult = spotInstanceHelper.requestSpotInstanceRequest(pluginSettings, ec2Config, consoleLogAppender);

            SpotInstanceRequest spotInstanceRequest = requestSpotInstancesResult.getSpotInstanceRequests().get(0);

            /*
               All valid spot SpotRequests are tagged after making a request for a spot instance.
               There are times when tagging a spot request fails with 404 since aws does not find a spot request for the
               given SpotInstanceRequestId. Ensuring that spot requests can be lookedup by id before tagging them.
            */
            spotInstanceHelper.waitTillSpotRequestCanBeLookedUpById(pluginSettings, spotInstanceRequest.getSpotInstanceRequestId());

            SpotInstanceStatus status = spotInstanceRequest.getStatus();
            LOG.debug("[create-agent] Created spot instance request with request Id: {}, state: {}, status-code:{}, status-message:{}",
                    spotInstanceRequest.getSpotInstanceRequestId(), spotInstanceRequest.getState(), status.getCode(), status.getMessage());

            tagSpotRequest(pluginSettings, elasticAgentProfileProperties, spotInstanceRequest);

            /*
              A spot instance request is tagged post creation. AWS takes time to sync up the the tags on the spot request, hence
              querying aws for the spot requests with tag filters does not yield results. Hence the SpotInstance service maintains
              a list of new spot instance requests and removes from the list in future calls or as part of server ping.
            */
            if (isSpotRequestValid(spotInstanceRequest)) {
                untaggedSpotRequests.add(spotInstanceRequest);
            }
            return Optional.empty();
        }
    }

    public void tagSpotInstances(PluginSettings pluginSettings) {
        List<SpotInstanceRequest> spotRequests = spotInstanceHelper.getSpotRequestsWithARunningSpotInstance(pluginSettings, pluginSettings.getClusterName());

        LOG.debug("[server-ping] There are total of: '{}' Spot Requests with Spot-Request-Ids: '{}' which have a running Spot Instance. Starting tagging of Spot Instance.",
                spotRequests.size(), spotRequestIds(spotRequests));

        Map<String, List<SpotInstanceRequest>> requestsByPlatform = groupSpotRequestsByPlatform(spotRequests);

        requestsByPlatform.forEach((platform, spotInstanceRequests) -> {
            List<String> spotInstanceIds = spotInstanceRequests.stream().map(SpotInstanceRequest::getInstanceId).collect(toList());
            spotInstanceHelper.tagSpotResources(pluginSettings, spotInstanceIds, Platform.from(platform));
        });
    }

    public void refreshUnTaggedSpotRequests(PluginSettings pluginSettings) {
        synchronized (untaggedSpotRequests) {
            List<SpotInstanceRequest> allSpotRequestsForCluster = spotInstanceHelper.getAllSpotRequestsForCluster(pluginSettings);
            List<String> spotRequestIds = allSpotRequestsForCluster.stream().map(SpotInstanceRequest::getSpotInstanceRequestId).collect(toList());

            LOG.debug("[refresh-spot-requests] All SpotRequests for Cluster: '{}'", String.join("", spotRequestIds));
            LOG.debug("[refresh-spot-requests] All Untagged spot requests: '{}'", untaggedSpotRequestIds(untaggedSpotRequests));

            untaggedSpotRequests.removeIf(request -> spotRequestIds.contains(request.getSpotInstanceRequestId()));
        }
    }

    private String untaggedSpotRequestIds(Set<SpotInstanceRequest> untaggedSpotRequests) {
        return untaggedSpotRequests.stream().map(SpotInstanceRequest::getSpotInstanceRequestId).collect(joining(","));
    }


    private List<SpotInstanceRequest> spotRequestsWithoutRegisteredInstances(PluginSettings pluginSettings, Platform platform,
                                                                             List<Instance> allRegisteredSpotInstancesForPlatform) {
        List<String> registeredInstanceIds = allRegisteredSpotInstancesForPlatform.stream()
                .map(Instance::getInstanceId).collect(toList());

        List<SpotInstanceRequest> spotRequests = spotInstanceHelper
                .getAllOpenOrSpotRequestsWithRunningInstances(pluginSettings, pluginSettings.getClusterName(), platform);

        return spotRequests.stream()
                .filter(spotInstanceRequest -> !registeredInstanceIds.contains(spotInstanceRequest.getInstanceId()))
                .collect(toList());
    }

    private boolean isThereAOpenSpotRequestMatchingProfile(Collection<SpotInstanceRequest> openSpotRequests, EC2Config ec2Config) {
        return openSpotRequests.stream().anyMatch(spotInstanceRequest -> spotRequestMatcher.matches(ec2Config, spotInstanceRequest));
    }

    private String spotRequestIds(List<SpotInstanceRequest> spotRequests) {
        return spotRequests.stream().map(SpotInstanceRequest::getSpotInstanceRequestId).collect(joining(", "));
    }

    public void tagIdleSpotInstances(PluginSettings pluginSettings) {
        List<Instance> idleSpotInstances = spotInstanceHelper.getAllIdleSpotInstances(pluginSettings, pluginSettings.getClusterName());

        List<Instance> idleInstancesWithoutTag = idleSpotInstances.stream().filter(getIdleInstancePredicate()).collect(toList());
        LOG.debug("[server-ping] There are total of: '{}' idle Spot Instances with Ids: '{}' without the 'LAST_SEEN_IDLE' tag. Starting tagging of spot instance.",
                idleSpotInstances.size(), instanceIds(idleInstancesWithoutTag));

        List<String> spotInstanceIds = idleInstancesWithoutTag.stream().map(Instance::getInstanceId).collect(toList());

        if (!spotInstanceIds.isEmpty()) {
            spotInstanceHelper.tagSpotInstancesAsIdle(pluginSettings, spotInstanceIds);
        }
    }

    public void terminateIdleSpotInstances(PluginSettings pluginSettings) {
        List<Instance> instancesToTerminate = spotInstanceHelper.getIdleInstancesEligibleForTermination(pluginSettings, pluginSettings.getClusterName());

        LOG.debug("[server-ping] Terminating total of: '{}' idle Spot Instances with Ids: '{}'.", instancesToTerminate.size(), instanceIds(instancesToTerminate));

        if (instancesToTerminate.isEmpty()) {
            return;
        }

        List<String> instancesToTerminateIds = instancesToTerminate.stream().map(Instance::getInstanceId).collect(toList());

        final List<ContainerInstance> containerInstances = containerInstanceHelper.spotContainerInstances(pluginSettings);
        final List<ContainerInstance> containerInstanceList = containerInstances.stream()
                .filter(containerInstance -> instancesToTerminateIds.contains(containerInstance.getEc2InstanceId()))
                .collect(toList());

        terminateOperation.execute(pluginSettings, containerInstanceList);
    }

    private String instanceIds(List<Instance> idleInstancesWithoutTag) {
        return idleInstancesWithoutTag.stream().map(Instance::getInstanceId).collect(joining(", "));
    }

    private Predicate<Instance> getIdleInstancePredicate() {
        return instance -> instance.getTags().stream().noneMatch(tag -> tag.getKey().equals(LAST_SEEN_IDLE));
    }

    private Map<String, List<SpotInstanceRequest>> groupSpotRequestsByPlatform(List<SpotInstanceRequest> spotInstanceRequests) {
        return spotInstanceRequests.stream()
                .collect(groupingBy(spotInstanceRequest -> platformTag(spotInstanceRequest.getTags()).getValue()));
    }

    private Tag platformTag(List<Tag> tags) {
        return tags.stream()
                .filter(tag -> "platform".equals(tag.getKey()))
                .findFirst()
                .get();
    }

    private void tagSpotRequest(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, SpotInstanceRequest spotInstanceRequest) {
        try {
            LOG.debug("[create-agent] Tagging open spot request.");

            spotInstanceRequest.withTags(new Tag().withKey("platform").withValue(elasticAgentProfileProperties.platform().name()));
            spotInstanceHelper.tagSpotResources(pluginSettings, List.of(spotInstanceRequest.getSpotInstanceRequestId()), elasticAgentProfileProperties.platform());
        } catch (Exception e) {
            LOG.error("[create-agent] There were errors while tagging spot instance request with id: '{}' cancelling the request.",
                    spotInstanceRequest.getSpotInstanceRequestId(), e);

            spotInstanceHelper.cancelSpotInstanceRequest(pluginSettings, spotInstanceRequest.getSpotInstanceRequestId());
            throw e;
        }
    }

    private boolean isSpotRequestValid(SpotInstanceRequest spotInstanceRequest) {
        return containsAny(spotInstanceRequest.getState(), ACTIVE, OPEN);
    }
}
