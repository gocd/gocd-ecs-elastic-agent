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

import com.amazonaws.services.ec2.model.GroupIdentifier;
import com.amazonaws.services.ec2.model.LaunchSpecification;
import com.amazonaws.services.ec2.model.SpotInstanceRequest;
import com.amazonaws.services.ec2.model.Tag;
import com.thoughtworks.gocd.elasticagent.ecs.aws.EC2Config;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

public class SpotRequestMatcher {
    public boolean matches(EC2Config ec2Config, SpotInstanceRequest spotInstanceRequest) {
        if (spotInstanceRequest == null) {
            return false;
        }

        LaunchSpecification launchSpecification = spotInstanceRequest.getLaunchSpecification();
        final Platform platform = platformFor(spotInstanceRequest);

        if (platform != ec2Config.getPlatform()) {
            return false;
        }

        if (!StringUtils.equals(launchSpecification.getImageId(), ec2Config.getAmi())) {
            return false;
        }

        if (!StringUtils.equals(ec2Config.getInstanceType(), launchSpecification.getInstanceType())) {
            return false;
        }

        if (notMatching(ec2Config.getSubnetIds(), launchSpecification.getSubnetId())) {
            return false;
        }

        if (notMatching(ec2Config.getSecurityGroups(), getInstanceSecurityGroups(launchSpecification))) {
            return false;
        }

        return ec2Config.runAsSpotInstance();
    }

    private Platform platformFor(SpotInstanceRequest spotInstanceRequest) {
        Tag platformTag = spotInstanceRequest.getTags().stream().filter(tag -> "platform".equals(tag.getKey())).findFirst().orElse(null);

        return platformTag != null ? Platform.from(platformTag.getValue()) : null;
    }

    private Set<String> getInstanceSecurityGroups(LaunchSpecification launchSpecification) {
        return launchSpecification.getAllSecurityGroups().stream().map(GroupIdentifier::getGroupId).collect(Collectors.toSet());
    }

    private boolean notMatching(Collection<String> valueFromConfig, String valueFromInstance) {
        return !valueFromConfig.isEmpty() && !valueFromConfig.contains(valueFromInstance);
    }

    private boolean notMatching(Collection<String> valueFromConfig, Collection<String> valueFromInstance) {
        return !valueFromConfig.isEmpty() && !valueFromInstance.containsAll(valueFromConfig);
    }
}
