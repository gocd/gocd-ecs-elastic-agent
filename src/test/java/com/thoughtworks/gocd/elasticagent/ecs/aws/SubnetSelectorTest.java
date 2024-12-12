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

import com.amazonaws.services.ec2.AmazonEC2Client;
import com.amazonaws.services.ec2.model.DescribeSubnetsRequest;
import com.amazonaws.services.ec2.model.DescribeSubnetsResult;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.Subnet;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.SubnetNotAvailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toSet;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SubnetSelectorTest {
    private PluginSettings pluginSettings;
    private SubnetSelector subnetSelector;
    private AmazonEC2Client ec2Client;

    @BeforeEach
    void setUp() {
        pluginSettings = mock(PluginSettings.class);
        ec2Client = mock(AmazonEC2Client.class);

        when(pluginSettings.ec2Client()).thenReturn(ec2Client);

        subnetSelector = new SubnetSelector();
    }

    @Test
    void shouldReturnNullIfSubnetIdsNotConfiguredInPluginSettings() {
        when(pluginSettings.getSubnetIds()).thenReturn(new ArrayList<>());

        final Subnet subnetId = subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), null);

        assertThat(subnetId).isNull();
    }

    @Test
    void shouldErrorOutIfNoSubnetAvailable() {
        when(pluginSettings.getSubnetIds()).thenReturn(List.of("subnet-1"));
        when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(new DescribeSubnetsResult().withSubnets(
                new Subnet().withSubnetId("subnet-1").withState("pending")
        ));

        final SubnetNotAvailableException exception = assertThrows(SubnetNotAvailableException.class, () -> subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), null));

        assertThat(exception.getMessage()).isEqualTo("None of the subnet available to launch ec2 instance from list subnet-1");
    }

    @Test
    void shouldReturnRandomSubnetIdIfNoContainerInstanceRunningAndMultipleSubnetOptions() {
        final ArgumentCaptor<DescribeSubnetsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSubnetsRequest.class);

        when(pluginSettings.getSubnetIds()).thenReturn(List.of("subnet-1", "subnet-2"));
        when(ec2Client.describeSubnets(argumentCaptor.capture())).thenReturn(new DescribeSubnetsResult().withSubnets(
                new Subnet().withSubnetId("subnet-1").withState("available"),
                new Subnet().withSubnetId("subnet-2").withState("available")
        ));

        assertThat(IntStream.range(0, 10)
                .mapToObj(i -> subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), emptyList()).getSubnetId())
                .collect(toSet()))
                .containsExactly("subnet-1", "subnet-2");
    }

    @Test
    void shouldReturnRandomSubnetIdIfContainerInstanceRunningInUnrelatedSubnetAndMultipleSubnetOptions() {
        final ArgumentCaptor<DescribeSubnetsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSubnetsRequest.class);

        when(pluginSettings.getSubnetIds()).thenReturn(List.of("subnet-1", "subnet-2"));
        when(ec2Client.describeSubnets(argumentCaptor.capture())).thenReturn(new DescribeSubnetsResult().withSubnets(
                new Subnet().withSubnetId("subnet-1").withState("available"),
                new Subnet().withSubnetId("subnet-2").withState("available")
        ));

        Instance instanceInOtherSubnet = new Instance().withSubnetId("some-other-subnet");

        assertThat(IntStream.range(0, 10)
                .mapToObj(i -> subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), List.of(instanceInOtherSubnet)).getSubnetId())
                .collect(toSet()))
                .containsExactly("subnet-1", "subnet-2");
    }

    @Test
    void shouldReturnSubnetIdWhichIsHavingMinimumEC2InstanceRunning() {
        final ArgumentCaptor<DescribeSubnetsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSubnetsRequest.class);

        when(pluginSettings.getSubnetIds()).thenReturn(List.of("subnet-1", "subnet-2", "subnet-3", "subnet-4", "subnet-5"));
        when(ec2Client.describeSubnets(argumentCaptor.capture())).thenReturn(new DescribeSubnetsResult().withSubnets(
                new Subnet().withSubnetId("subnet-1").withState("pending"),
                new Subnet().withSubnetId("subnet-2").withState("available"),
                new Subnet().withSubnetId("subnet-3").withState("available"),
                new Subnet().withSubnetId("subnet-4").withState("available"),
                new Subnet().withSubnetId("subnet-5").withState("available")
        ));

        final List<Instance> instances = List.of(
                new Instance().withInstanceId("instance-1.1").withSubnetId("subnet-1"),
                new Instance().withInstanceId("instance-2.1").withSubnetId("subnet-2"),
                new Instance().withInstanceId("instance-2.2").withSubnetId("subnet-2"),
                new Instance().withInstanceId("instance-3.1").withSubnetId("subnet-3"),
                new Instance().withInstanceId("instance-4.1").withSubnetId("subnet-4"),
                new Instance().withInstanceId("instance-4.2").withSubnetId("subnet-4"),
                new Instance().withInstanceId("instance-4.3").withSubnetId("subnet-4"),
                new Instance().withInstanceId("instance-5.1").withSubnetId("subnet-5")
        );

        assertThat(IntStream.range(0, 10)
                .mapToObj(i -> subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), instances).getSubnetId())
                .collect(toSet()))
                .containsExactly("subnet-3", "subnet-5");
    }
}
