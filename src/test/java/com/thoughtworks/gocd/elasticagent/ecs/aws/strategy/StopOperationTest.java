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

package com.thoughtworks.gocd.elasticagent.ecs.aws.strategy;

import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ec2.model.StopInstancesRequest;
import software.amazon.awssdk.services.ec2.model.StopInstancesResponse;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StopOperationTest {

    private PluginSettings pluginSettings;
    private Ec2Client ec2Client;

    @BeforeEach
    void setUp() {
        pluginSettings = mock(PluginSettings.class);
        ec2Client = mock(Ec2Client.class);

        when(pluginSettings.ec2Client()).thenReturn(ec2Client);
    }

    @Test
    void shouldStopInstance() {
        final ContainerInstance instanceToDeregister = containerInstance("i-abcde12", "container-instance-arn");
        ;
        final ArgumentCaptor<StopInstancesRequest> stopInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(StopInstancesRequest.class);

        when(ec2Client.stopInstances(stopInstancesRequestArgumentCaptor.capture()))
                .thenReturn(StopInstancesResponse.builder().build());

        new StopOperation().execute(pluginSettings, instanceToDeregister);

        final StopInstancesRequest stopInstancesRequest = stopInstancesRequestArgumentCaptor.getValue();

        assertThat(stopInstancesRequest.instanceIds())
                .hasSize(1)
                .contains("i-abcde12");
    }

}
