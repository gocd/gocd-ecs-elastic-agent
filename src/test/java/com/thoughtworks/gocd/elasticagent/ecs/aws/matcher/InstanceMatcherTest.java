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

import com.thoughtworks.gocd.elasticagent.ecs.aws.EC2Config;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.Instance;
import software.amazon.awssdk.services.ec2.model.InstanceState;
import software.amazon.awssdk.services.ec2.model.InstanceStateName;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.ec2.model.InstanceStateName.RUNNING;
import static software.amazon.awssdk.services.ec2.model.InstanceType.T2_MEDIUM;
import static software.amazon.awssdk.services.ec2.model.InstanceType.T2_SMALL;

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
        when(instance.state()).thenReturn(InstanceState.builder().name(RUNNING).build());
        when(instance.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.platformAsString()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(instance.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(instance.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        when(instance.spotInstanceRequestId()).thenReturn("spot_id");
        when(ec2Config.runAsSpotInstance()).thenReturn(true);

        assertThat(instanceMatcher.matches(ec2Config, instance)).isTrue();
    }

    @Test
    void shouldFalseIfPlatformDoesNotMatch() {
        when(instance.state()).thenReturn(InstanceState.builder().name(RUNNING).build());
        when(instance.platformAsString()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.WINDOWS);

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldReturnFalseIfInstanceTypeIsNotMatching() {
        when(instance.state()).thenReturn(InstanceState.builder().name(RUNNING).build());
        when(instance.platformAsString()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_MEDIUM.toString());

        when(instance.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(instance.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldReturnFalseIfSecurityGroupsAreNotMatching() {
        when(instance.state()).thenReturn(InstanceState.builder().name(RUNNING).build());
        when(instance.platformAsString()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(instance.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(instance.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));

        when(ec2Config.getSecurityGroups()).thenReturn(Arrays.asList("sg-abcde", "sg-xyz"));

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldReturnFalseIfSubnetIdIsNotMatching() {
        when(instance.state()).thenReturn(InstanceState.builder().name(RUNNING).build());
        when(instance.platformAsString()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(instance.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("different-subnet-id"));

        when(instance.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldReturnFalseIfAMIIsNotMatching() {
        when(instance.state()).thenReturn(InstanceState.builder().name(RUNNING).build());
        when(instance.platformAsString()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("diffrent-ami");

        when(instance.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(instance.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(instance.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @Test
    void shouldNotMatchAOnDemandInstanceIfProfileRequiresSpot() {
        when(instance.state()).thenReturn(InstanceState.builder().name(RUNNING).build());
        when(instance.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(instance.platformAsString()).thenReturn("linux");
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(instance.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(instance.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(instance.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        when(instance.spotInstanceRequestId()).thenReturn(null);
        when(ec2Config.runAsSpotInstance()).thenReturn(true);

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }

    @ParameterizedTest
    @EnumSource(value = InstanceStateName.class, names = {"STOPPING", "STOPPED", "SHUTTING_DOWN", "TERMINATED"})
    void shouldReturnFalseIfInstanceIsInState(InstanceStateName instanceState) {
        when(instance.state()).thenReturn(InstanceState.builder().name(instanceState).build());

        assertThat(instanceMatcher.matches(ec2Config, instance)).isFalse();
    }
}
