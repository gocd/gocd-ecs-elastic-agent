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

import com.amazonaws.services.ec2.AmazonEC2;
import com.amazonaws.services.ec2.model.StopInstancesRequest;
import com.amazonaws.services.ec2.model.StopInstancesResult;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerInstanceMother.containerInstance;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StopOperationTest {

    private PluginSettings pluginSettings;
    private AmazonEC2 ec2Client;

    @BeforeEach
    void setUp() {
        pluginSettings = mock(PluginSettings.class);
        ec2Client = mock(AmazonEC2.class);

        when(pluginSettings.ec2Client()).thenReturn(ec2Client);
    }

    @Test
    void shouldStopInstance() {
        final ContainerInstance instanceToDeregister = containerInstance("i-abcde12", "container-instance-arn");
        ;
        final ArgumentCaptor<StopInstancesRequest> stopInstancesRequestArgumentCaptor = ArgumentCaptor.forClass(StopInstancesRequest.class);

        when(ec2Client.stopInstances(stopInstancesRequestArgumentCaptor.capture()))
                .thenReturn(new StopInstancesResult());

        new StopOperation().execute(pluginSettings, instanceToDeregister);

        final StopInstancesRequest stopInstancesRequest = stopInstancesRequestArgumentCaptor.getValue();

        assertThat(stopInstancesRequest.getInstanceIds())
                .hasSize(1)
                .contains("i-abcde12");
    }

}
