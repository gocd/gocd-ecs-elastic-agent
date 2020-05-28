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

package com.thoughtworks.gocd.elasticagent.ecs.aws.matcher;

import com.amazonaws.services.ec2.model.*;
import com.thoughtworks.gocd.elasticagent.ecs.aws.EC2Config;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Arrays;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

        when(spotInstanceRequest.getLaunchSpecification()).thenReturn(launchSpecification);
    }

    @Test
    void shouldReturnFalseIfSpotInstanceRequestIsNull() {
        assertThat(spotRequestMatcher.matches(null, null)).isFalse();
    }

    @Test
    void shouldReturnTrueIfAllFieldMatches() {
        when(spotInstanceRequest.getLaunchSpecification()).thenReturn(launchSpecification);
        when(launchSpecification.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(spotInstanceRequest.getTags()).thenReturn(asList(new Tag().withKey("platform").withValue("linux")));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(launchSpecification.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(asList("s-foo-id"));

        when(launchSpecification.getAllSecurityGroups()).thenReturn(asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-abcde"));

        when(spotInstanceRequest.getSpotInstanceRequestId()).thenReturn("spot_id");
        when(ec2Config.runAsSpotInstance()).thenReturn(true);

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isTrue();
    }

    @Test
    void shouldFalseIfPlatformDoesNotMatch() {
        when(spotInstanceRequest.getTags()).thenReturn(asList(new Tag().withKey("platform").withValue("linux")));
        when(ec2Config.getPlatform()).thenReturn(Platform.WINDOWS);

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldReturnFalseIfInstanceTypeIsNotMatching() {

        when(spotInstanceRequest.getTags()).thenReturn(asList(new Tag().withKey("platform").withValue("linux")));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(launchSpecification.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.medium");

        when(launchSpecification.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(asList("s-foo-id"));

        when(launchSpecification.getAllSecurityGroups()).thenReturn(asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-abcde"));

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldReturnFalseIfSecurityGroupsAreNotMatching() {
        when(spotInstanceRequest.getTags()).thenReturn(asList(new Tag().withKey("platform").withValue("linux")));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(launchSpecification.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(launchSpecification.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(asList("s-foo-id"));

        when(launchSpecification.getAllSecurityGroups()).thenReturn(asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));

        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-abcde", "sg-xyz"));

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldReturnFalseIfSubnetIdIsNotMatching() {
        when(spotInstanceRequest.getTags()).thenReturn(asList(new Tag().withKey("platform").withValue("linux")));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(launchSpecification.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(launchSpecification.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(asList("different-subnet-id"));

        when(launchSpecification.getAllSecurityGroups()).thenReturn(asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-abcde"));

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldReturnFalseIfAMIIsNotMatching() {
        when(spotInstanceRequest.getTags()).thenReturn(asList(new Tag().withKey("platform").withValue("linux")));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("diffrent-ami");

        when(launchSpecification.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(launchSpecification.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(asList("s-foo-id"));

        when(launchSpecification.getAllSecurityGroups()).thenReturn(asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-abcde"));

        assertThat(spotRequestMatcher.matches(ec2Config, spotInstanceRequest)).isFalse();
    }

    @Test
    void shouldNotMatchAOnDemandInstanceIfProfileRequiresSpot() {
        when(launchSpecification.getImageId()).thenReturn("i-123456");
        when(ec2Config.getAmi()).thenReturn("i-123456");

        when(spotInstanceRequest.getTags()).thenReturn(asList(new Tag().withKey("platform").withValue("linux")));
        when(ec2Config.getPlatform()).thenReturn(Platform.LINUX);

        when(launchSpecification.getInstanceType()).thenReturn("t2.small");
        when(ec2Config.getInstanceType()).thenReturn("t2.small");

        when(launchSpecification.getSubnetId()).thenReturn("s-foo-id");
        when(ec2Config.getSubnetIds()).thenReturn(asList("s-foo-id"));

        when(launchSpecification.getAllSecurityGroups()).thenReturn(asList(
                new GroupIdentifier().withGroupId("sg-abcde")
        ));
        when(ec2Config.getSecurityGroups()).thenReturn(asList("sg-abcde"));

        when(spotInstanceRequest.getSpotInstanceRequestId()).thenReturn(null);
        when(ec2Config.runAsSpotInstance()).thenReturn(true);
    }
}
