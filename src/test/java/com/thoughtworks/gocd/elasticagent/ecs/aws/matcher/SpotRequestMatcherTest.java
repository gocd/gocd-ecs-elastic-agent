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
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.LaunchSpecification;
import software.amazon.awssdk.services.ec2.model.SpotInstanceRequest;
import software.amazon.awssdk.services.ec2.model.Tag;

import java.util.Collections;
import java.util.List;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static software.amazon.awssdk.services.ec2.model.InstanceType.T2_MEDIUM;
import static software.amazon.awssdk.services.ec2.model.InstanceType.T2_SMALL;

class SpotRequestMatcherTest {

    private EC2Config ec2Config;
    private SpotRequestMatcher spotRequestMatcher;
    private SpotInstanceRequest spotInstanceRequest;
    private LaunchSpecification launchSpecification;

    @BeforeEach
    void setUp() {
        ec2Config = mock(EC2Config.class);
        spotInstanceRequest = mock(SpotInstanceRequest.class);
        launchSpecification = mock(LaunchSpecification.class);
        spotRequestMatcher = new SpotRequestMatcher();

        when(spotInstanceRequest.launchSpecification()).thenReturn(launchSpecification);
    }

    @Test
    void shouldReturnFalseIfSpotInstanceRequestIsNull() {
        assertThat(spotRequestMatcher.matches(null, null)).isFalse();
    }

    @Test
    void shouldReturnTrueIfAllFieldMatches() {
        when(spotInstanceRequest.launchSpecification()).thenReturn(launchSpecification);
        when(launchSpecification.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(spotInstanceRequest.tags()).thenReturn(Collections.singletonList(Tag.builder().key("platform").value("linux").build()));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(launchSpecification.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(launchSpecification.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        when(spotInstanceRequest.spotInstanceRequestId()).thenReturn("spot_id");
        when(ec2Config.runAsSpotInstance()).thenReturn(true);

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isTrue();
    }

    @Test
    void shouldFalseIfPlatformDoesNotMatch() {
        when(spotInstanceRequest.tags()).thenReturn(Collections.singletonList(Tag.builder().key("platform").value("linux").build()));
        when(ec2Config.getPlatform()).thenReturn(Platform.WINDOWS);

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldReturnFalseIfInstanceTypeIsNotMatching() {

        when(spotInstanceRequest.tags()).thenReturn(Collections.singletonList(Tag.builder().key("platform").value("linux").build()));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(launchSpecification.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_MEDIUM.toString());

        when(launchSpecification.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(launchSpecification.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldReturnFalseIfSecurityGroupsAreNotMatching() {
        when(spotInstanceRequest.tags()).thenReturn(Collections.singletonList(Tag.builder().key("platform").value("linux").build()));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(launchSpecification.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(launchSpecification.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(launchSpecification.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));

        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-abcde", "sg-xyz"));

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldReturnFalseIfSubnetIdIsNotMatching() {
        when(spotInstanceRequest.tags()).thenReturn(Collections.singletonList(Tag.builder().key("platform").value("linux").build()));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(launchSpecification.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(launchSpecification.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("different-subnet-id"));

        when(launchSpecification.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldReturnFalseIfAMIIsNotMatching() {
        when(spotInstanceRequest.tags()).thenReturn(Collections.singletonList(Tag.builder().key("platform").value("linux").build()));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("diffrent-ami");

        when(launchSpecification.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(launchSpecification.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(launchSpecification.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldNotMatchAOnDemandInstanceIfProfileRequiresSpot() {
        when(launchSpecification.imageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(spotInstanceRequest.tags()).thenReturn(Collections.singletonList(Tag.builder().key("platform").value("linux").build()));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.instanceTypeAsString()).thenReturn(T2_SMALL.toString());
        when(ec2Config.getInstanceType()).thenReturn(T2_SMALL.toString());

        when(launchSpecification.subnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(List.of("s-foo-id"));

        when(launchSpecification.securityGroups()).thenReturn(Collections.singletonList(
                GroupIdentifier.builder().groupId("sg-abcde").build()
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(List.of("sg-abcde"));

        when(spotInstanceRequest.spotInstanceRequestId()).thenReturn(null);
        when(ec2Config.runAsSpotInstance()).thenReturn(true);
    }
}
