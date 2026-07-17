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
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import com.thoughtworks.gocd.elasticagent.ecs.Constants;
import com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin;
import com.thoughtworks.gocd.elasticagent.ecs.aws.comparator.MostIdleInstanceComparator;
import com.thoughtworks.gocd.elasticagent.ecs.aws.matcher.InstanceMatcher;
import com.thoughtworks.gocd.elasticagent.ecs.aws.strategy.TerminateOperation;
import com.thoughtworks.gocd.elasticagent.ecs.aws.wait.Poller;
import com.thoughtworks.gocd.elasticagent.ecs.aws.wait.Result;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ConsoleLogAppender;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.ContainerInstanceFailedToRegisterException;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.LimitExceededException;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import lombok.NonNull;
import software.amazon.awssdk.services.ec2.model.*;
import software.amazon.awssdk.services.ec2.model.Tag;
import software.amazon.awssdk.services.ecs.model.*;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LABEL_SERVER_ID;
import static com.thoughtworks.gocd.elasticagent.ecs.Constants.LAST_SEEN_IDLE;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.getServerId;
import static com.thoughtworks.gocd.elasticagent.ecs.aws.SpotInstanceHelper.SPOT_INSTANCE_NAME_FORMAT;

import static java.lang.String.valueOf;
import static java.text.MessageFormat.format;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.*;
import static org.apache.commons.lang3.StringUtils.isBlank;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static software.amazon.awssdk.services.ec2.model.InstanceStateName.*;

public class ContainerInstanceHelper {
    private static final Logger LOG = Logger.getLoggerFor(ContainerInstanceHelper.class);
    private static final Function<ContainerInstance, Boolean> CONTAINER_INSTANCE_IDLE_FUNCTION = containerInstance -> containerInstance.pendingTasksCount() == 0 && containerInstance.runningTasksCount() == 0;

    private final Supplier<String> serverIdSupplier;
    private final InstanceMatcher instanceMatcher;
    private final SubnetSelector subnetSelector;

    public ContainerInstanceHelper() {
        this(ECSElasticPlugin::getServerId, new InstanceMatcher(), new SubnetSelector());
    }

    ContainerInstanceHelper(Supplier<String> serverIdSupplier, InstanceMatcher instanceMatcher, SubnetSelector subnetSelector) {
        this.serverIdSupplier = serverIdSupplier;
        this.instanceMatcher = instanceMatcher;
        this.subnetSelector = subnetSelector;
    }

    public List<ContainerInstance> getContainerInstances(PluginSettings settings) {
        final List<String> runningContainerArnList = containerInstanceArnList(settings);

        if (runningContainerArnList.isEmpty()) {
            return emptyList();
        }

        final DescribeContainerInstancesRequest describeContainerInstancesRequest = DescribeContainerInstancesRequest.builder()
                .containerInstances(runningContainerArnList)
                .cluster(settings.getClusterName())
                .build();

        DescribeContainerInstancesResponse describeContainerInstancesResponse = settings.ecsClient()
                .describeContainerInstances(describeContainerInstancesRequest);

        return describeContainerInstancesResponse.containerInstances();
    }

    public List<ContainerInstance> onDemandContainerInstances(PluginSettings pluginSettings) {
        List<ContainerInstance> containerInstances = getContainerInstances(pluginSettings);
        List<Instance> onDemandInstances = getOnDemandInstances(pluginSettings, containerInstances);

        List<String> ids = onDemandInstances.stream().map(Instance::instanceId).toList();

        return containerInstances.stream()
                .filter(containerInstance -> ids.contains(containerInstance.ec2InstanceId()))
                .collect(toList());
    }

    public List<ContainerInstance> spotContainerInstances(PluginSettings pluginSettings) {
        List<ContainerInstance> containerInstances = getContainerInstances(pluginSettings);
        List<Instance> spotInstances = getSpotInstances(pluginSettings, containerInstances);

        List<String> ids = spotInstances.stream().map(Instance::instanceId).toList();

        return containerInstances.stream()
                .filter(containerInstance -> ids.contains(containerInstance.ec2InstanceId()))
                .collect(toList());
    }

    private List<Instance> getOnDemandInstances(PluginSettings pluginSettings, List<ContainerInstance> containerInstances) {
        List<Instance> allEc2Instances = ec2InstancesFromContainerInstances(pluginSettings, containerInstances);

        return allEc2Instances.stream()
                .filter(ec2Instance -> isBlank(ec2Instance.spotInstanceRequestId()))
                .collect(toList());
    }

    private List<Instance> getSpotInstances(PluginSettings pluginSettings, List<ContainerInstance> containerInstances) {
        List<Instance> allEc2Instances = ec2InstancesFromContainerInstances(pluginSettings, containerInstances);

        return allEc2Instances.stream()
                .filter(ec2Instance -> isNotBlank(ec2Instance.spotInstanceRequestId()))
                .collect(toList());
    }

    public Cluster getCluster(PluginSettings settings) {
        final DescribeClustersRequest describeClustersRequest = DescribeClustersRequest.builder().clusters(settings.getClusterName()).build();
        final List<Cluster> clusters = settings.ecsClient().describeClusters(describeClustersRequest).clusters();

        if (clusters.isEmpty()) {
            throw ClusterNotFoundException.builder().message(format("Cluster {0} not found.", settings.getClusterName())).build();
        }

        return clusters.getFirst();
    }

    public List<Instance> ec2InstancesFromContainerInstances(PluginSettings settings, List<ContainerInstance> containerInstanceList) {
        if (containerInstanceList.isEmpty()) {
            return emptyList();
        }

        final List<String> instanceIds = containerInstanceList.stream().map(ContainerInstance::ec2InstanceId).collect(toList());
        DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder().instanceIds(instanceIds).build();
        final DescribeInstancesResponse describeInstancesResponse = settings.ec2Client().describeInstances(describeInstancesRequest);

        List<Reservation> reservations = describeInstancesResponse.reservations();
        List<Instance> instances = new ArrayList<>();
        for (Reservation reservation : reservations) {
            instances.addAll(reservation.instances());
        }

        return instances;
    }

    public List<Instance> getAllInstances(PluginSettings pluginSettings) {
        final DescribeInstancesRequest describeInstancesRequest = DescribeInstancesRequest.builder()
                .filters(
                        Filter.builder().name("tag:Creator").values(Constants.PLUGIN_ID).build(),
                        Filter.builder().name("instance-state-name").values(PENDING.toString(), RUNNING.toString(), STOPPING.toString(), STOPPED.toString()).build()
                )
                .build();

        final Set<String> registeredInstanceIds = getContainerInstances(pluginSettings).stream()
                .map(ContainerInstance::ec2InstanceId)
                .collect(toSet());

        return pluginSettings.ec2Client().describeInstances(describeInstancesRequest)
                .reservations().stream()
                .flatMap(filterInstances(registeredInstanceIds))
                .collect(toList());
    }

    public List<Instance> getAllOnDemandInstances(PluginSettings pluginSettings) {
        List<Instance> allInstances = getAllInstances(pluginSettings);

        return allInstances.stream()
                .filter(instance -> isBlank(instance.spotInstanceRequestId()))
                .collect(toList());
    }

    public void checkAndMarkEC2InstanceIdle(PluginSettings pluginSettings, String ec2InstanceId) {
        final List<ContainerInstance> containerInstances = getContainerInstances(pluginSettings);

        if (containerInstances.isEmpty()) {
            return;
        }

        final Boolean isIdle = containerInstances.stream()
                .filter(containerInstance -> containerInstance.ec2InstanceId().equals(ec2InstanceId))
                .map(CONTAINER_INSTANCE_IDLE_FUNCTION)
                .findFirst().orElse(false);

        if (isIdle) {
            final CreateTagsRequest tag = CreateTagsRequest.builder()
                    .tags(Tag.builder().key(LAST_SEEN_IDLE).value(valueOf(System.currentTimeMillis())).build())
                    .resources(ec2InstanceId)
                    .build();

            pluginSettings.ec2Client().createTags(tag);
        }
    }

    public void removeLastSeenIdleTag(PluginSettings pluginSettings, Collection<String> instanceIds) {
        LOG.info("Removing LAST_SEEN_IDLE tag from instances " + instanceIds);

        final DeleteTagsRequest deleteTagsRequest = DeleteTagsRequest.builder()
                .tags(Tag.builder().key(LAST_SEEN_IDLE).build())
                .resources(instanceIds)
                .build();

        pluginSettings.ec2Client().deleteTags(deleteTagsRequest);
    }

    public @NonNull ContainerInstance startOrCreateOneInstance(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, ConsoleLogAppender consoleLogAppender) throws LimitExceededException {
        return startOrCreateInstance(pluginSettings, elasticAgentProfileProperties, 1, consoleLogAppender)
                .getFirst();
    }

    public List<ContainerInstance> startOrCreateInstance(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, int numberOfInstanceToStartOrCreate, ConsoleLogAppender consoleLogAppender) throws LimitExceededException {
        final Optional<List<ContainerInstance>> optionalStartedInstances = startInstances(pluginSettings, elasticAgentProfileProperties, numberOfInstanceToStartOrCreate, consoleLogAppender);
        final List<ContainerInstance> startedContainerInstances = optionalStartedInstances.orElse(new ArrayList<>());

        final int instancesToCreate = numberOfInstanceToStartOrCreate - startedContainerInstances.size();
        if (instancesToCreate > 0) {
            final Optional<List<ContainerInstance>> createdInstances = createInstances(pluginSettings, elasticAgentProfileProperties, instancesToCreate, consoleLogAppender);
            createdInstances.ifPresent(startedContainerInstances::addAll);
        }

        return startedContainerInstances;
    }

    public Optional<List<ContainerInstance>> startInstances(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, int numberOfInstanceToStartOrCreate, ConsoleLogAppender consoleLogAppender) {
        synchronized (elasticAgentProfileProperties.platform()) {

            String instanceName = String.format("%s_%s_INSTANCE", pluginSettings.getClusterName(), elasticAgentProfileProperties.platform());
            final List<Instance> allStoppedInstances = filterBy(filterByState(getAllOnDemandInstances(pluginSettings), STOPPED), hasTag("Name", instanceName))
                    .stream()
                    .filter(platformPredicate(elasticAgentProfileProperties.platform()))
                    .toList();

            if (allStoppedInstances.isEmpty()) {
                LOG.info("No stopped instances found.");
                return Optional.empty();
            }

            LOG.info(format("Found {0} stopped instances.", allStoppedInstances.size()));

            final EC2Config ec2Config = new EC2Config.Builder()
                    .settings(pluginSettings).profile(elasticAgentProfileProperties)
                    .build();

            final List<String> instancesToStart = allStoppedInstances.stream()
                    .filter(instance -> instanceMatcher.matches(ec2Config, instance))
                    .map(Instance::instanceId)
                    .limit(numberOfInstanceToStartOrCreate)
                    .collect(toList());

            if (instancesToStart.isEmpty()) {
                LOG.info(format("None of the stopped instances match the configuration {0}.", ec2Config));
                return Optional.empty();
            }

            removeLastSeenIdleTag(pluginSettings, instancesToStart);

            LOG.info(format("Starting {0} instances.", instancesToStart.size()));
            consoleLogAppender.accept(String.format("Found existing stopped instance(s) matching platform configurations. Starting (%s) instances to schedule ECS Task.", instancesToStart));

            pluginSettings.ec2Client().startInstances(StartInstancesRequest.builder().instanceIds(instancesToStart).build());

            return waitInstanceToStart(pluginSettings, ec2Config, instancesToStart, consoleLogAppender);
        }
    }

    public Optional<List<ContainerInstance>> createInstances(PluginSettings pluginSettings, ElasticAgentProfileProperties elasticAgentProfileProperties, int numberOfInstancesToCreate, ConsoleLogAppender consoleLogAppender) throws LimitExceededException {
        synchronized (elasticAgentProfileProperties.platform()) {
            if (numberOfInstancesToCreate == 0) {
                LOG.info("Not creating new instances as number of requested ec2 instances are 0.");
                return Optional.empty();
            }

            final EC2Config ec2Config = new EC2Config.Builder()
                    .settings(pluginSettings)
                    .profile(elasticAgentProfileProperties)
                    .build();

            final List<Instance> allInstances = allInstances(pluginSettings, elasticAgentProfileProperties.platform());
            final List<Instance> allOnDemandInstances = filterBy(allInstances, isOnDemandInstance());
            final List<Instance> instancesForPlatform = filterByPlatform(allOnDemandInstances, ec2Config.getPlatform());
            final List<Instance> stoppedInstances = filterByState(instancesForPlatform, STOPPED);

            LOG.info(format("Found total {0} on-demand instances for platform {1} and from that {2} instances are in stopped state.", instancesForPlatform.size(), ec2Config.getPlatform(), stoppedInstances.size()));

            if (stoppedInstances.size() == ec2Config.getMaxInstancesAllowed()) {
                terminateMostIdleStoppedInstance(pluginSettings, stoppedInstances);
            } else if (ec2Config.getMaxInstancesAllowed() <= instancesForPlatform.size()) {
                throw new LimitExceededException(ec2Config.getPlatform().name(), ec2Config.getMaxInstancesAllowed());
            }

            final Subnet selectedSubnet = subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, ec2Config.getSubnetIds(), allInstances);

            final RunInstancesRequest runInstancesRequest = new RunInstanceRequestBuilder()
                    .eC2Config(ec2Config)
                    .subnet(selectedSubnet)
                    .instanceToCreate(numberOfInstancesToCreate)
                    .serverId(getServerId())
                    .build();

            consoleLogAppender.accept("Creating a new container instance to schedule ECS Task.");
            LOG.info(format("Creating container instance with configuration: {0}", runInstancesRequest.toString()));
            RunInstancesResponse runInstancesResponse = pluginSettings.ec2Client().runInstances(runInstancesRequest);

            List<String> newlyLaunchedInstances = runInstancesResponse.instances().stream()
                    .map(Instance::instanceId).collect(toList());

            return waitInstanceToStart(pluginSettings, ec2Config, newlyLaunchedInstances, consoleLogAppender);
        }
    }

    private List<Instance> allInstances(PluginSettings pluginSettings, Platform platform) {
        String onDemandInstanceName = String.format("%s_%s_INSTANCE", pluginSettings.getClusterName(), platform);
        String spotInstanceName = String.format(SPOT_INSTANCE_NAME_FORMAT, pluginSettings.getClusterName(), platform);

        List<Instance> allInstances = getAllInstances(pluginSettings);
        List<Instance> onDemandInstances = filterBy(allInstances, hasTag("Name", onDemandInstanceName));
        List<Instance> spotInstances = filterBy(allInstances, hasTag("Name", spotInstanceName));

        return Stream.concat(onDemandInstances.stream(), spotInstances.stream()).toList();
    }

    public static Map<Platform, List<Instance>> groupByPlatform(List<Instance> instances) {
        return instances.stream().collect(groupingBy(i -> Platform.from(i.platformAsString())));
    }

    public static List<Instance> filterBy(List<Instance> instances, Predicate<Instance> predicate) {
        return instances.stream().filter(predicate).collect(toList());
    }

    public static List<Instance> filterByPlatform(List<Instance> instances, Platform platform) {
        return filterBy(instances, platformPredicate(platform));
    }

    private static Predicate<Instance> platformPredicate(Platform platform) {
        return instance -> Platform.from(instance.platformAsString()) == platform;
    }

    public static List<Instance> filterByState(List<Instance> instances, InstanceStateName instanceState) {
        return filterBy(instances, instance -> instance.state().name().equals(instanceState));
    }

    public static Predicate<Instance> hasTag(String name, String value) {
        return instance -> instance.tags().stream().anyMatch(getTagPredicate(name, value));
    }

    public static Predicate<Instance> isOnDemandInstance() {
        return instance -> isBlank(instance.spotInstanceRequestId());
    }

    public static Predicate<Instance> isSpotInstance() {
        return instance -> isNotBlank(instance.spotInstanceRequestId());
    }

    private static Predicate<Tag> getTagPredicate(String tagName, String tagValue) {
        return tag -> tagName.equals(tag.key()) && tagValue.equals(tag.value());
    }

    private Function<Reservation, Stream<? extends Instance>> filterInstances(Set<String> registeredInstanceIds) {
        return reservation -> reservation.instances().stream()
                .filter(isRegistered(registeredInstanceIds).or(hasTag(LABEL_SERVER_ID, serverIdSupplier.get())));
    }

    private Predicate<Instance> isRegistered(Set<String> registeredInstanceIds) {
        return instance -> registeredInstanceIds.contains(instance.instanceId());
    }

    private List<String> containerInstanceArnList(PluginSettings settings) {
        final ListContainerInstancesRequest listContainerInstancesRequest = ListContainerInstancesRequest.builder()
                .cluster(settings.getClusterName())
                .build();

        return settings.ecsClient().listContainerInstances(listContainerInstancesRequest).containerInstanceArns();
    }


    private Supplier<List<ContainerInstance>> waitInstanceToStartSupplier(PluginSettings pluginSettings, List<String> instancesToStart) {
        return () -> getContainerInstances(pluginSettings).stream()
                .filter(containerInstance -> instancesToStart.contains(containerInstance.ec2InstanceId()))
                .filter(ContainerInstance::agentConnected)
                .collect(toList());
    }

    private void cleanOnFail(PluginSettings pluginSettings, Collection<String> instancesFailed) throws ContainerInstanceFailedToRegisterException {
        if (instancesFailed.isEmpty()) {
            return;
        }

        final Set<String> containerInstancesToDeregister = getContainerInstances(pluginSettings).stream()
                .filter(containerInstance -> instancesFailed.contains(containerInstance.ec2InstanceId()))
                .map(ContainerInstance::containerInstanceArn)
                .collect(toSet());

        containerInstancesToDeregister.forEach(containerInstanceToDeregister -> {
            try {
                LOG.info(format("Deregistering container instance {0}.", containerInstanceToDeregister));
                final DeregisterContainerInstanceRequest deregisterContainerInstanceRequest = DeregisterContainerInstanceRequest.builder()
                        .containerInstance(containerInstanceToDeregister)
                        .cluster(pluginSettings.getClusterName())
                        .force(true)
                        .build();

                pluginSettings.ecsClient().deregisterContainerInstance(deregisterContainerInstanceRequest);
            } catch (Exception e) {
                LOG.debug("Instance must have been deregistered previously.", e);
            }
        });

        LOG.info(format("EC2 instances {0} failed to start. Terminating created instances.", instancesFailed));
        pluginSettings.ec2Client().terminateInstances(TerminateInstancesRequest.builder().instanceIds(instancesFailed).build());

        LOG.info(format("EC2 instances({0}) successfully terminated.", instancesFailed));
    }

    private Optional<List<ContainerInstance>> waitInstanceToStart(PluginSettings pluginSettings, EC2Config ec2Config, List<String> instanceIds, ConsoleLogAppender consoleLogAppender) {
        consoleLogAppender.accept(String.format("Waiting for instance(s) (%s) to register with cluster.", instanceIds));

        LOG.info(format("Waiting for instances({0}) to register with cluster.", instanceIds));
        final Result<List<ContainerInstance>> result = new Poller<List<ContainerInstance>>()
                .timeout(ec2Config.getRegisterTimeOut())
                .stopWhen(containerInstances -> containerInstances.size() == instanceIds.size())
                .poll(waitInstanceToStartSupplier(pluginSettings, instanceIds))
                .await();

        if (result.isFailed()) {
            final Collection<String> instancesFailedToRegister = instancesFailedToRegister(result, instanceIds);
            cleanOnFail(pluginSettings, instancesFailedToRegister);

            if (result.get() == null || result.get().isEmpty()) {
                throw new ContainerInstanceFailedToRegisterException(format("EC2Instance failed to register with the ECS cluster: {0} within {1}. Terminated un-registered instance(s).", pluginSettings.getClusterName(), Util.formatDurationWords(ec2Config.getRegisterTimeOut().toMillis())));
            }
        }

        return Optional.ofNullable(result.get());
    }

    private void terminateMostIdleStoppedInstance(PluginSettings pluginSettings, List<Instance> stoppedInstances) {
        stoppedInstances.sort(new MostIdleInstanceComparator(Clock.DEFAULT.now()));
        final String instanceId = stoppedInstances.getFirst().instanceId();

        LOG.info(format("Terminating stopped instance as max cluster limit is reached {0}.", instanceId));
        final Optional<ContainerInstance> containerInstance = getContainerInstances(pluginSettings)
                .stream().filter(ci -> ci.ec2InstanceId().equals(instanceId))
                .findFirst();

        containerInstance.ifPresent(self -> new TerminateOperation().execute(pluginSettings, self));
    }

    private Collection<String> instancesFailedToRegister(Result<List<ContainerInstance>> result, Collection<String> allRequestedInstances) {
        if (result.get() == null || result.get().isEmpty()) {
            return allRequestedInstances;
        }

        final Set<String> instancesRegistered = result.get().stream()
                .map(ContainerInstance::ec2InstanceId)
                .collect(toSet());

        LOG.info(format("Started {0} instances.", instancesRegistered));

        return allRequestedInstances.stream()
                .filter(requestInstanceId -> !instancesRegistered.contains(requestInstanceId))
                .collect(toSet());
    }
}
