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

package com.thoughtworks.gocd.elasticagent.ecs.aws.matcher;

import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.thoughtworks.gocd.elasticagent.ecs.aws.EC2Config;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class InstanceMatcherTest {

    private EC2Config ec2Config;
    private InstanceMatcher instanceMatcher;
    private Instance instance;

    @BeforeEach
    void setUp() {
        ec2Config = mock(EC2Config.class);
        instance = mock(Instance.class);
        instanceMatcher = new InstanceMatcher();
    }

    @Test
    void shouldReturnFalseIfInstanceIsNull() {
        assertThat(instanceMatcher.matches(null, null)).isFalse();
    }

    @Test
    void shouldReturnTrueIfAllFieldMatches() {
        when(instance.getState()).thenReturn(new InstanceState().withName(RUNNING));
        when(instance.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.getPlatform()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(instance.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(Arrays.asList("s-foo-id"));

        when(instance.getSecurityGroups()).thenReturn(Arrays.asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(Arrays.asList("sg-abcde"));

        when(instance.getSpotInstanceRequestId()).thenReturn("spot_id");
        when(ec2Config.runAsSpotInstance()).thenReturn(true);

        assertThat(instanceMatcher.matches(ec2Config, instance)).isTrue();
    }

    @Test
    void shouldFalseIfPlatformDoesNotMatch() {
        when(instance.getState()).thenReturn(new InstanceState().withName(RUNNING));
        when(instance.getPlatform()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.WINDOWS);

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldReturnFalseIfInstanceTypeIsNotMatching() {
        when(instance.getState()).thenReturn(new InstanceState().withName(RUNNING));
        when(instance.getPlatform()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.medium");

        when(instance.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(Arrays.asList("s-foo-id"));

        when(instance.getSecurityGroups()).thenReturn(Arrays.asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(Arrays.asList("sg-abcde"));

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldReturnFalseIfSecurityGroupsAreNotMatching() {
        when(instance.getState()).thenReturn(new InstanceState().withName(RUNNING));
        when(instance.getPlatform()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(instance.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(Arrays.asList("s-foo-id"));

        when(instance.getSecurityGroups()).thenReturn(Arrays.asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));

        when(ec2Config.getSecurityGroups()).thenReturn(Arrays.asList("sg-abcde", "sg-xyz"));

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldReturnFalseIfSubnetIdIsNotMatching() {
        when(instance.getState()).thenReturn(new InstanceState().withName(RUNNING));
        when(instance.getPlatform()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(instance.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(Arrays.asList("different-subnet-id"));

        when(instance.getSecurityGroups()).thenReturn(Arrays.asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(Arrays.asList("sg-abcde"));

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldReturnFalseIfAMIIsNotMatching() {
        when(instance.getState()).thenReturn(new InstanceState().withName(RUNNING));
        when(instance.getPlatform()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("diffrent-ami");

        when(instance.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(instance.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(Arrays.asList("s-foo-id"));

        when(instance.getSecurityGroups()).thenReturn(Arrays.asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(Arrays.asList("sg-abcde"));

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldNotMatchAOnDemandInstanceIfProfileRequiresSpot() {
        when(instance.getState()).thenReturn(new InstanceState().withName(RUNNING));
        when(instance.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.getPlatform()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(instance.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(Arrays.asList("s-foo-id"));

        when(instance.getSecurityGroups()).thenReturn(Arrays.asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(Arrays.asList("sg-abcde"));

        when(instance.getSpotInstanceRequestId()).thenReturn(null);
        when(ec2Config.runAsSpotInstance()).thenReturn(true);

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @ParameterizedTest
    @ValueSource(strings = {STOPPING, STOPPED, SHUTTING_DOWN, TERMINATED})
    void shouldReturnFalseIfInstanceIsInState(String instanceState) {
        when(instance.getState()).thenReturn(new InstanceState().withName(instanceState));

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }
}
