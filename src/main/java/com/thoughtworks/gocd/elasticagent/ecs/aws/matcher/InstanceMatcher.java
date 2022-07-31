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
import com.thoughtworks.gocd.elasticagent.ecs.aws.EC2Config;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static org.apache.commons.lang3.StringUtils.isNotEmpty;

public class InstanceMatcher {
    private static final String ERROR_MESSAGE_FOR_OBJECT = "Instance {0}({1}) of the instance({2}) does not match {3}.";
    private static final String ERROR_MESSAGE_FOR_LIST = "Instance {0}({1}) of the instance({2}) is not in {3}.";

    public boolean matches(EC2Config ec2Config, Instance instance) {
        if (instance == null) {
            return false;
        }

        final Platform platform = Platform.from(instance.getPlatform());
        if (platform != ec2Config.getPlatform()) {
            LOG.debug(ERROR_MESSAGE_FOR_OBJECT, "platform", platform, instance.getInstanceId(), ec2Config.getPlatform());
            return false;
        }

        if (!StringUtils.equals(instance.getImageId(), ec2Config.getAmi())) {
            LOG.debug(ERROR_MESSAGE_FOR_OBJECT, "AMI", instance.getImageId(), instance.getInstanceId(), ec2Config.getAmi());
            return false;
        }

        if (!StringUtils.equals(ec2Config.getInstanceType(), instance.getInstanceType())) {
            LOG.debug(ERROR_MESSAGE_FOR_OBJECT, "InstanceType", instance.getInstanceType(), instance.getInstanceId(), ec2Config.getInstanceType());
            return false;
        }

        if (notMatching(ec2Config.getSubnetIds(), instance.getSubnetId())) {
            LOG.debug(ERROR_MESSAGE_FOR_OBJECT, "SubnetId", instance.getSubnetId(), instance.getInstanceId(), ec2Config.getSubnetIds());
            return false;
        }

        if (notMatching(ec2Config.getSecurityGroups(), getInstanceSecurityGroups(instance))) {
            LOG.debug(ERROR_MESSAGE_FOR_LIST, "SecurityGroups", instance.getSecurityGroups(), instance.getInstanceId(), ec2Config.getSecurityGroups());
            return false;
        }

        if (ec2Config.runAsSpotInstance() != isSpotInstance(instance)) {
            return false;
        }

        return true;
    }

    private boolean isSpotInstance(Instance instance) {
        return isNotEmpty(instance.getSpotInstanceRequestId());
    }

    private Set<String> getInstanceSecurityGroups(Instance instance) {
        return instance.getSecurityGroups().stream().map(GroupIdentifier::getGroupId).collect(Collectors.toSet());
    }

    private boolean notMatching(Collection<String> valueFromConfig, String valueFromInstance) {
        return !valueFromConfig.isEmpty() && !valueFromConfig.contains(valueFromInstance);
    }

    private boolean notMatching(Collection<String> valueFromConfig, Collection<String> valueFromInstance) {
        return !valueFromConfig.isEmpty() && !valueFromInstance.containsAll(valueFromConfig);
    }
}
