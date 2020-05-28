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

package com.thoughtworks.gocd.elasticagent.ecs.aws.strategy;

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.AmazonEC2Exception;
import com.amazonaws.services.ec2.model.TerminateInstancesRequest;
import com.amazonaws.services.ec2.model.TerminateInstancesResult;
import com.amazonaws.services.ecs.AmazonECS;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.DeregisterContainerInstanceRequest;
import com.amazonaws.services.ecs.model.DeregisterContainerInstanceResult;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TerminateOperationTest {

    private PluginSettings pluginSettings;
    private AmazonECS ecsClient;
    private AmazonEC2 ec2Client;
    private TerminateOperation terminateOperation;

    @BeforeEach
    void setUp() {
        pluginSettings = mock(PluginSettings.class);
        ecsClient = mock(AmazonECS.class);
        ec2Client = mock(AmazonEC2.class);

        when(pluginSettings.ecsClient()).thenReturn(ecsClient);
        when(pluginSettings.ec2Client()).thenReturn(ec2Client);
        terminateOperation = new TerminateOperation();
    }

    @Test
    void shouldTerminateInstance() {
        final ContainerInstance instanceToDeregister = containerInstance("i-abcde12", "container-instance-arn");

        final ArgumentCaptor<TerminateInstancesRequest> terminateInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(TerminateInstancesRequest.class);
        final ArgumentCaptor<DeregisterContainerInstanceRequest> deregisterContainerInstanceRequestArgumentCaptor = ArgumentCaptor.forClass(DeregisterContainerInstanceRequest.class);

        when(pluginSettings.getClusterName()).thenReturn("GoCD");
        when(ecsClient.deregisterContainerInstance(deregisterContainerInstanceRequestArgumentCaptor.capture()))
                .thenReturn(new DeregisterContainerInstanceResult().withContainerInstance(instanceToDeregister));

        when(ec2Client.terminateInstances(terminateInstancesRequestArgumentCaptor.capture()))
                .thenReturn(new TerminateInstancesResult());

        terminateOperation.execute(pluginSettings, instanceToDeregister);

        final DeregisterContainerInstanceRequest deregisterContainerInstanceRequest = deregisterContainerInstanceRequestArgumentCaptor.getValue();
        assertThat(deregisterContainerInstanceRequest.getCluster()).isEqualTo("GoCD");
        assertThat(deregisterContainerInstanceRequest.getContainerInstance()).isEqualTo("container-instance-arn");
        assertThat(deregisterContainerInstanceRequest.getForce()).isEqualTo(true);

        final TerminateInstancesRequest terminateInstancesRequest = terminateInstancesRequestArgumentCaptor.getValue();
        assertThat(terminateInstancesRequest.getInstanceIds())
                .hasSize(1)
                .contains("i-abcde12");
    }

    @Test
    void shouldErrorOutIfFailedToTerminateContainerInstance() {
        final ContainerInstance instanceToDeregister = containerInstance("i-abcde12", "container-instance-arn");;

        when(ecsClient.deregisterContainerInstance(any())).thenThrow(new AmazonEC2Exception("Request Timeout"));

        final AmazonEC2Exception exception = assertThrows(AmazonEC2Exception.class, () -> terminateOperation.execute(pluginSettings, instanceToDeregister));

        assertThat(exception.getMessage()).startsWith("Request Timeout");
    }
}
