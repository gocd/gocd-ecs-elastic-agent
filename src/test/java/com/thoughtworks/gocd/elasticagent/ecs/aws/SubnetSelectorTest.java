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

import static java.util.Collections.emptyList;
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
        when(pluginSettings.getSubnetIds()).thenReturn(Arrays.asList("subnet-1"));
        when(ec2Client.describeSubnets(any(DescribeSubnetsRequest.class))).thenReturn(new DescribeSubnetsResult().withSubnets(
                new Subnet().withSubnetId("subnet-1").withState("pending")
        ));

        final SubnetNotAvailableException exception = assertThrows(SubnetNotAvailableException.class, () -> subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), null));

        assertThat(exception.getMessage()).isEqualTo("None of the subnet available to launch ec2 instance from list subnet-1");
    }

    @Test
    void shouldReturnFirstAvailableSubnetIdIfNoContainerInstanceRunning() {
        final ArgumentCaptor<DescribeSubnetsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSubnetsRequest.class);

        when(pluginSettings.getSubnetIds()).thenReturn(Arrays.asList("subnet-1"));
        when(ec2Client.describeSubnets(argumentCaptor.capture())).thenReturn(new DescribeSubnetsResult().withSubnets(
                new Subnet().withSubnetId("subnet-1").withState("available")
        ));

        final Subnet subnet = subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), emptyList());

        assertThat(subnet.getSubnetId()).isEqualTo("subnet-1");
        assertThat(argumentCaptor.getValue().getSubnetIds()).isEqualTo(Arrays.asList("subnet-1"));
    }

    @Test
    void shouldReturnSubnetIdWhichIsHavingMinimumEC2InstanceRunning() {
        final ArgumentCaptor<DescribeSubnetsRequest> argumentCaptor = ArgumentCaptor.forClass(DescribeSubnetsRequest.class);

        when(pluginSettings.getSubnetIds()).thenReturn(Arrays.asList("subnet-1"));
        when(ec2Client.describeSubnets(argumentCaptor.capture())).thenReturn(new DescribeSubnetsResult().withSubnets(
                new Subnet().withSubnetId("subnet-1").withState("pending"),
                new Subnet().withSubnetId("subnet-2").withState("available"),
                new Subnet().withSubnetId("subnet-3").withState("available"),
                new Subnet().withSubnetId("subnet-4").withState("available")
        ));

        final List<Instance> instances = Arrays.asList(
                new Instance().withInstanceId("instance-1").withSubnetId("subnet-1"),
                new Instance().withInstanceId("instance-2").withSubnetId("subnet-2"),
                new Instance().withInstanceId("instance-3").withSubnetId("subnet-2"),
                new Instance().withInstanceId("instance-4").withSubnetId("subnet-3"),
                new Instance().withInstanceId("instance-5").withSubnetId("subnet-4"),
                new Instance().withInstanceId("instance-6").withSubnetId("subnet-4"),
                new Instance().withInstanceId("instance-7").withSubnetId("subnet-4")
        );

        final Subnet subnet = subnetSelector.selectSubnetWithMinimumEC2Instances(pluginSettings, pluginSettings.getSubnetIds(), instances);

        assertThat(subnet.getSubnetId()).isEqualTo("subnet-3");
    }
}
