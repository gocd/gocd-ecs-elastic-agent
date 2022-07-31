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

package com.thoughtworks.gocd.elasticagent.ecs.executors;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.*;
import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper;
import com.thoughtworks.gocd.elasticagent.ecs.aws.EC2Config;
import com.thoughtworks.gocd.elasticagent.ecs.aws.SpotInstanceService;
import com.thoughtworks.gocd.elasticagent.ecs.aws.StopPolicy;
import com.thoughtworks.gocd.elasticagent.ecs.aws.comparator.MostIdleInstanceComparator;
import com.thoughtworks.gocd.elasticagent.ecs.aws.predicate.EligibleForTerminationPredicate;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.InstanceSelectionStrategyFactory;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.StopOperation;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.TerminateOperation;
import com.thoughtworks.gocd.elasticagent.ecs.domain.*;
import com.thoughtworks.gocd.elasticagent.ecs.events.Event;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventFingerprint;
import com.thoughtworks.gocd.elasticagent.ecs.events.EventStream;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.LimitExceededException;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ServerRequestFailedException;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ServerPingRequest;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceHelper.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.values;
import static java.text.MessageFormat.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

public class ServerPingRequestExecutor implements RequestExecutor {
    private ServerPingRequest serverPingRequest;
    private Map<String, ECSTasks> allAgentInstances;
    private final PluginRequest pluginRequest;
    private final ContainerInstanceHelper containerInstanceHelper;
    private final InstanceSelectionStrategyFactory instanceSelectionStrategyFactory;
    private final StopOperation stopOperation;
    private final TerminateOperation terminateOperation;
    private final SpotInstanceService spotInstanceService;

    public ServerPingRequestExecutor(ServerPingRequest serverPingRequest, Map<String, ECSTasks> allAgentInstances, PluginRequest pluginRequest) {
        this(serverPingRequest,
                allAgentInstances,
                pluginRequest,
                new ContainerInstanceHelper(),
                new InstanceSelectionStrategyFactory(),
                new StopOperation(),
                new TerminateOperation(), SpotInstanceService.instance());
    }

    ServerPingRequestExecutor(ServerPingRequest serverPingRequest, Map<String, ECSTasks> allAgentInstances, PluginRequest pluginRequest,
                              ContainerInstanceHelper containerInstanceHelper, InstanceSelectionStrategyFactory instanceSelectionStrategyFactory,
                              StopOperation stopOperation, TerminateOperation terminateOperation, SpotInstanceService spotInstanceService) {
        this.serverPingRequest = serverPingRequest;
        this.allAgentInstances = allAgentInstances;
        this.pluginRequest = pluginRequest;
        this.containerInstanceHelper = containerInstanceHelper;
        this.instanceSelectionStrategyFactory = instanceSelectionStrategyFactory;
        this.stopOperation = stopOperation;
        this.terminateOperation = terminateOperation;
        this.spotInstanceService = spotInstanceService;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        ConsoleLogAppender doNothingConsoleLogAppender = text -> {
            //do nothing console log appender in case of ping request..
        };

        LOG.info("[server-ping] Starting execute server ping request.");
        List<ClusterProfileProperties> allClusterProfileProperties = serverPingRequest.allClusterProfileProperties();

        for (ClusterProfileProperties clusterProfileProperties : allClusterProfileProperties) {
            performCleanupForCluster(clusterProfileProperties, allAgentInstances.get(clusterProfileProperties.uuid()), doNothingConsoleLogAppender);
        }

        CheckForPossiblyMissingAgents();
        return DefaultGoPluginApiResponse.success("");
    }

    private void CheckForPossiblyMissingAgents() {
        Collection<Agent> allAgents = pluginRequest.listAgents().agents();

        List<Agent> missingAgents = allAgents.stream().filter(agent -> allAgentInstances.values().stream()
                .noneMatch(instances -> instances.hasInstance(agent.elasticAgentId()))).collect(Collectors.toList());

        if (!missingAgents.isEmpty()) {
            List<String> missingAgentIds = missingAgents.stream().map(Agent::elasticAgentId).collect(Collectors.toList());
            LOG.warn("[Server Ping] Was expecting a containers with IDs " + missingAgentIds + ", but it was missing! Removing missing agents from config.");
            pluginRequest.disableAgents(missingAgents);
            pluginRequest.deleteAgents(missingAgents);
        }
    }

    private void performCleanupForCluster(ClusterProfileProperties clusterProfileProperties, ECSTasks agentInstances, ConsoleLogAppender doNothingConsoleLogAppender) throws Exception {
        Agents allAgents = pluginRequest.listAgents();

        Agents agentsToDisable = agentInstances.instancesCreatedAfterTimeout(clusterProfileProperties, allAgents);

        LOG.debug("[server-ping] Disabling agents '{0}' as those are in idle state or created after container register timeout.", agentsToDisable.agentIds());
        disableIdleAgents(agentsToDisable);

        allAgents = pluginRequest.listAgents();
        terminateDisabledAgents(allAgents, clusterProfileProperties, agentInstances);

        agentInstances.terminateUnregisteredInstances(clusterProfileProperties, allAgents);

        synchronized (agentInstances) {
            tagSpotInstances(clusterProfileProperties);
            terminateIdleSpotInstances(clusterProfileProperties);
            ensureClusterSize(clusterProfileProperties, agentInstances.getEventStream(), doNothingConsoleLogAppender);
            stopIdleEC2Instance(clusterProfileProperties, agentInstances.getEventStream());
            terminateStoppedInstances(clusterProfileProperties);
        }
    }

    private void terminateIdleSpotInstances(ClusterProfileProperties clusterProfileProperties) {
        try {
            spotInstanceService.terminateIdleSpotInstances(clusterProfileProperties);
        } catch (Exception e) {
            LOG.error("[server-ping] There were errors while terminating idle spot instances.", e);
        }
    }

    private void tagSpotInstances(ClusterProfileProperties clusterProfileProperties) {
        try {
            spotInstanceService.tagSpotInstances(clusterProfileProperties);
            spotInstanceService.tagIdleSpotInstances(clusterProfileProperties);
            spotInstanceService.refreshUnTaggedSpotRequests(clusterProfileProperties);
        } catch (Exception e) {
            LOG.error("[server-ping] There were errors while tagging a spot instance.", e);
        }
    }

    private void terminateStoppedInstances(PluginSettings pluginSettings) {
        final List<Instance> allInstances = containerInstanceHelper.getAllOnDemandInstances(pluginSettings);

        final EligibleForTerminationPredicate predicate = new EligibleForTerminationPredicate(pluginSettings);
        final Set<String> instancesToTerminate = allInstances.stream()
                .filter(predicate)
                .map(Instance::getInstanceId)
                .collect(Collectors.toSet());

        if (instancesToTerminate.isEmpty()) {
            LOG.debug("[server-ping] None of the instance is eligible for termination.");
        }

        final List<ContainerInstance> containerInstances = containerInstanceHelper.onDemandContainerInstances(pluginSettings);
        final List<ContainerInstance> containerInstanceList = containerInstances.stream()
                .filter(containerInstance -> instancesToTerminate.contains(containerInstance.getEc2InstanceId()))
                .collect(toList());

        terminateOperation.execute(pluginSettings, containerInstanceList);
    }

    private void stopIdleEC2Instance(PluginSettings pluginSettings, EventStream eventStream) {
        Arrays.stream(values()).forEach(platform -> stopInstances(pluginSettings, platform, eventStream));
        eventStream.remove(EventFingerprint.forTerminatingIdleEC2Instances());
    }

    private void stopInstances(PluginSettings pluginSettings, Platform platform, EventStream eventStream) {
        try {
            final StopPolicy stopPolicy = platform == LINUX ? pluginSettings.getLinuxStopPolicy() : pluginSettings.getWindowsStopPolicy();
            final Optional<List<ContainerInstance>> instanceToStop = instanceSelectionStrategyFactory
                    .strategyFor(stopPolicy)
                    .instancesToStop(pluginSettings, platform);

            instanceToStop.ifPresent(instancesToStop -> stopOperation.execute(pluginSettings, instancesToStop));

        } catch (Exception e) {
            LOG.error(format("[server-ping] Error while stopping idle {1} instance.", platform.name()), e);
            eventStream.update(Event.errorEvent(EventFingerprint.forTerminatingIdleEC2Instances(), format("Error while stopping idle {1} instance.", platform.name()), e.getMessage()));
        }
    }

    private void disableIdleAgents(Agents agents) throws ServerRequestFailedException {
        pluginRequest.disableAgents(agents.findInstancesToDisable());
    }

    private void terminateDisabledAgents(Agents agents, PluginSettings pluginSettings, ECSTasks agentInstances) throws Exception {
        Collection<Agent> toBeDeleted = agents.findInstancesToTerminate();
        final Set<String> elasticAgentIds = toBeDeleted.stream().map(Agent::elasticAgentId).collect(toSet());

        LOG.debug("[server-ping] Terminating '{}' disabled agents from the cluster '{}'.", elasticAgentIds, pluginSettings.getClusterName());
        for (Agent agent : toBeDeleted) {
            agentInstances.terminate(agent.elasticAgentId(), pluginSettings);
        }

        LOG.debug("[server-ping] Deleting disabled agents from server '{}'.", elasticAgentIds);
        pluginRequest.deleteAgents(toBeDeleted);
    }

    private void ensureClusterSize(PluginSettings settings, EventStream eventStream, ConsoleLogAppender consoleLogAppender) {
        try {
            for (Platform platform : values()) {
                ensureClusterSizeBasedOnPlatform(settings, ElasticAgentProfileProperties.empty(platform), eventStream, consoleLogAppender);
            }
        } catch (Exception e) {
            eventStream.update(Event.errorEvent(EventFingerprint.forEnsureClusterMinSize(), "Error creating EC2 Instance(s).", e.getMessage()));
            LOG.error("[server-ping] Error while creating EC2 Instance to ensure cluster min size", e);
        }
    }

    private void ensureClusterSizeBasedOnPlatform(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, EventStream eventStream, ConsoleLogAppender consoleLogAppender) throws LimitExceededException {
        LOG.info(format("[server-ping] Checking running {0} instances in the cluster.", elasticAgentProfileProperties.platform()));

        final EC2Config ec2Config = new EC2Config.Builder().withProfile(elasticAgentProfileProperties).withSettings(pluginSettings).build();

        String instanceName = String.format("%s_%s_INSTANCE", pluginSettings.getClusterName(), elasticAgentProfileProperties.platform());
        final List<Instance> allInstances = containerInstanceHelper.getAllOnDemandInstances(pluginSettings);
        final List<Instance> instancesForPlatform = filterBy(filterByPlatform(allInstances, ec2Config.getPlatform()), hasTag("Name", instanceName));

        int currentClusterSize = instancesForPlatform.size();
        if (currentClusterSize < ec2Config.getMinInstanceCount()) {
            int instancesToCreate = ec2Config.getMinInstanceCount() - currentClusterSize;
            LOG.info(format("[server-ping] Ensuring cluster min size, cluster {0} requires {1} more ec2 instances.", pluginSettings.getClusterName(), instancesToCreate));
            containerInstanceHelper.startOrCreateInstance(pluginSettings, elasticAgentProfileProperties, instancesToCreate, consoleLogAppender);
        } else if (currentClusterSize > ec2Config.getMaxInstancesAllowed()) {
            LOG.info(format("[server-ping] Cluster has total {0} {1} instances which is beyond permissible limit({2}). Terminating idle instances.", currentClusterSize, ec2Config.getPlatform(), ec2Config.getMaxInstancesAllowed()));
            terminateIdleContainerInstance(pluginSettings, instancesForPlatform);
        }

        eventStream.remove(EventFingerprint.forEnsureClusterMinSize());
    }

    private void terminateIdleContainerInstance(PluginSettings pluginSettings, List<Instance> instancesForPlatform) {
        final Instance instance = instancesForPlatform.stream().sorted(new MostIdleInstanceComparator(Clock.DEFAULT.now())).collect(toList()).get(0);
        final Optional<ContainerInstance> containerInstance = containerInstanceHelper.onDemandContainerInstances(pluginSettings).stream()
                .filter(ci -> ci.getEc2InstanceId().equals(instance.getInstanceId()))
                .findFirst();
        containerInstance.ifPresent(self -> terminateOperation.execute(pluginSettings, self));
    }
}
