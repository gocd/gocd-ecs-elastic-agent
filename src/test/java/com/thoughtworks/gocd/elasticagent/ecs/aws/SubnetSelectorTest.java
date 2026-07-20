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

import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.SubnetNotAvailableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsRequest;
import software.amazon.awssdk.services.ec2.model.DescribeSubnetsResponse;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.Subnet;

import java.util.ArrayList;
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
    private Ec2Client ec2Client;

    @BeforeEach
    void setUp() {
        pluginSettings = mock(PluginSettings.class);
        ec2Client = mock(Ec2Client.class);

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
        when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(DescribeSubnetsResponse.builder().subnets(
                Subnet.builder().subnetId("subnet-1").state("pending").build()
        ).build());

        final SubnetNotAvailableException exception = assertThrows(SubnetNotAvailableException.class, () -> subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), null));

        assertThat(exception.getMessage()).isEqualTo("None of the subnet available to launch ec2 instance from list subnet-1");
    }

    @Test
    void shouldReturnRandomSubnetIdIfNoContainerInstanceRunningAndMultipleSubnetOptions() {
        final ArgumentCaptor<DescribeSubnetsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSubnetsRequest.class);

        when(pluginSettings.getSubnetIds()).thenReturn(List.of("subnet-1", "subnet-2"));
        when(ec2Client.describeSubnets(argumentCaptor.capture())).thenReturn(DescribeSubnetsResponse.builder().subnets(
                Subnet.builder().subnetId("subnet-1").state("available").build(),
                Subnet.builder().subnetId("subnet-2").state("available").build()
        ).build());

        assertThat(IntStream.range(0, 10)
                .mapToObj(i -> subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), emptyList()).subnetId())
                .collect(toSet()))
                .containsExactly("subnet-1", "subnet-2");
    }

    @Test
    void shouldReturnRandomSubnetIdIfContainerInstanceRunningInUnrelatedSubnetAndMultipleSubnetOptions() {
        final ArgumentCaptor<DescribeSubnetsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSubnetsRequest.class);

        when(pluginSettings.getSubnetIds()).thenReturn(List.of("subnet-1", "subnet-2"));
        when(ec2Client.describeSubnets(argumentCaptor.capture())).thenReturn(DescribeSubnetsResponse.builder().subnets(
                Subnet.builder().subnetId("subnet-1").state("available").build(),
                Subnet.builder().subnetId("subnet-2").state("available").build()
        ).build());

        Instance instanceInOtherSubnet = Instance.builder().subnetId("some-other-subnet").build();

        assertThat(IntStream.range(0, 10)
                .mapToObj(i -> subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), List.of(instanceInOtherSubnet)).subnetId())
                .collect(toSet()))
                .containsExactly("subnet-1", "subnet-2");
    }

    @Test
    void shouldReturnSubnetIdWhichIsHavingMinimumEC2InstanceRunning() {
        final ArgumentCaptor<DescribeSubnetsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSubnetsRequest.class);

        when(pluginSettings.getSubnetIds()).thenReturn(List.of("subnet-1", "subnet-2", "subnet-3", "subnet-4", "subnet-5"));
        when(ec2Client.describeSubnets(argumentCaptor.capture())).thenReturn(DescribeSubnetsResponse.builder().subnets(
                Subnet.builder().subnetId("subnet-1").state("pending").build(),
                Subnet.builder().subnetId("subnet-2").state("available").build(),
                Subnet.builder().subnetId("subnet-3").state("available").build(),
                Subnet.builder().subnetId("subnet-4").state("available").build(),
                Subnet.builder().subnetId("subnet-5").state("available").build()
        ).build());

        final List<Instance> instances = List.of(
                Instance.builder().instanceId("instance-1.1").subnetId("subnet-1").build(),
                Instance.builder().instanceId("instance-2.1").subnetId("subnet-2").build(),
                Instance.builder().instanceId("instance-2.2").subnetId("subnet-2").build(),
                Instance.builder().instanceId("instance-3.1").subnetId("subnet-3").build(),
                Instance.builder().instanceId("instance-4.1").subnetId("subnet-4").build(),
                Instance.builder().instanceId("instance-4.2").subnetId("subnet-4").build(),
                Instance.builder().instanceId("instance-4.3").subnetId("subnet-4").build(),
                Instance.builder().instanceId("instance-5.1").subnetId("subnet-5").build()
        );

        assertThat(IntStream.range(0, 10)
                .mapToObj(i -> subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), instances).subnetId())
                .collect(toSet()))
                .containsExactly("subnet-3", "subnet-5");
    }
}
