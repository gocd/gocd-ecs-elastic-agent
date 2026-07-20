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

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.gocd.elasticagent.ecs.aws.EC2Config;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import software.amazon.awssdk.services.ec2.model.GroupIdentifier;
import software.amazon.awssdk.services.ec2.model.Instance;

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class InstanceMatcher {
    private static final Logger LOG = Logger.getLoggerFor(InstanceMatcher.class);

    private static final String ERROR_MESSAGE_FOR_OBJECT = "Instance {0}({1}) of the instance({2}) does not match {3}.";
    private static final String ERROR_MESSAGE_FOR_LIST = "Instance {0}({1}) of the instance({2}) is not in {3}.";

    public boolean matches(EC2Config ec2Config, Instance instance) {
        if (instance == null) {
            return false;
        }

        final Platform platform = Platform.from(instance.platformAsString());
        if (platform != ec2Config.getPlatform()) {
            LOG.debug(ERROR_MESSAGE_FOR_OBJECT, "platform", platform, instance.instanceId(), ec2Config.getPlatform());
            return false;
        }

        if (!Objects.equals(instance.imageId(), ec2Config.getAmi())) {
            LOG.debug(ERROR_MESSAGE_FOR_OBJECT, "AMI", instance.imageId(), instance.instanceId(), ec2Config.getAmi());
            return false;
        }

        if (!Objects.equals(ec2Config.getInstanceType(), instance.instanceTypeAsString())) {
            LOG.debug(ERROR_MESSAGE_FOR_OBJECT, "InstanceType", instance.instanceTypeAsString(), instance.instanceId(), ec2Config.getInstanceType());
            return false;
        }

        if (notMatching(ec2Config.getSubnetIds(), instance.subnetId())) {
            LOG.debug(ERROR_MESSAGE_FOR_OBJECT, "SubnetId", instance.subnetId(), instance.instanceId(), ec2Config.getSubnetIds());
            return false;
        }

        if (notMatching(ec2Config.getSecurityGroups(), getInstanceSecurityGroups(instance))) {
            LOG.debug(ERROR_MESSAGE_FOR_LIST, "SecurityGroups", instance.securityGroups(), instance.instanceId(), ec2Config.getSecurityGroups());
            return false;
        }

        return ec2Config.runAsSpotInstance() == isSpotInstance(instance);
    }

    private boolean isSpotInstance(Instance instance) {
        return isNotEmpty(instance.spotInstanceRequestId());
    }

    private Set<String> getInstanceSecurityGroups(Instance instance) {
        return instance.securityGroups().stream().map(GroupIdentifier::groupId).collect(Collectors.toSet());
    }

    private boolean notMatching(Collection<String> valueFromConfig, String valueFromInstance) {
        return !valueFromConfig.isEmpty() && !valueFromConfig.contains(valueFromInstance);
    }

    private boolean notMatching(Collection<String> valueFromConfig, Collection<String> valueFromInstance) {
        return !valueFromConfig.isEmpty() && !valueFromInstance.containsAll(valueFromConfig);
    }
}
