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

import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Subnet;
import com.amazonaws.services.ec2.model.SubnetState;
import com.google.common.base.Joiner;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.SubnetNotAvailableException;

import java.util.*;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static java.text.MessageFormat.format;

public class SubnetSelector {
    public Subnet selectSubnetWithMinimumEC2Instances(PluginSettings pluginSettings, Collection<String> subnetIds, List<Instance> instances) {
        if (subnetIds.isEmpty()) {
            LOG.info("Subnet id is not configured in plugin settings. AWS will assign subnet default available subnet id.");
            return null;
        }

        final List<Subnet> subnets = availableSubnets(pluginSettings, subnetIds);

        if (instances.isEmpty()) {
            return subnets.get(0);
        }

        return findSubnetWithMinimumInstances(subnets, instances);
    }

    private Subnet findSubnetWithMinimumInstances(List<Subnet> subnets, List<Instance> instances) {
        final Map<Subnet, Long> instancePerSubnet = instancePerSubnet(subnets, instances);

        return subnets.stream()
                .filter(subnet -> !instancePerSubnet.containsKey(subnet)).findFirst()
                .orElse(Collections.min(instancePerSubnet.entrySet(), Comparator.comparingDouble(Map.Entry::getValue)).getKey());
    }

    private Map<Subnet, Long> instancePerSubnet(List<Subnet> subnets, List<Instance> instances) {
        final Map<String, Subnet> subnetMap = subnets.stream().collect(Collectors.toMap(Subnet::getSubnetId, subnet -> subnet));

        return instances.stream()
                .filter(instance -> subnetMap.keySet().contains(instance.getSubnetId()))
                .collect(Collectors.groupingBy(subnet -> subnetMap.get(subnet.getSubnetId()), Collectors.counting()));
    }

    private List<Subnet> availableSubnets(PluginSettings pluginSettings, Collection<String> subnetIds) {
        final List<Subnet> subnets = allSubnets(pluginSettings, subnetIds).stream()
                .filter(this::isSubnetAvailable)
                .collect(Collectors.toList());

        if (subnets.isEmpty()) {
            throw new SubnetNotAvailableException(format("None of the subnet available to launch ec2 instance from list {0}", Joiner.on(",").join(subnetIds)));
        }

        return Collections.unmodifiableList(subnets);
    }

    private boolean isSubnetAvailable(Subnet subnet) {
        return SubnetState.Available == SubnetState.fromValue(subnet.getState());
    }

    private List<Subnet> allSubnets(PluginSettings pluginSettings, Collection<String> subnetIds) {
        final DescribeSubnetsRequest describeSubnetsRequest = new DescribeSubnetsRequest().withSubnetIds(subnetIds);
        return pluginSettings.ec2Client().describeSubnets(describeSubnetsRequest).getSubnets();
    }
}
