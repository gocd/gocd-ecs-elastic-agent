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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

import com.amazonaws.services.ec2.model.Instance;
import com.amazonaws.services.ec2.model.InstanceState;
import com.amazonaws.services.ec2.model.InstanceType;
import com.amazonaws.services.ec2.model.Tag;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;

import java.util.Date;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.EC2InstanceState.RUNNING;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static java.lang.String.format;

public class InstanceMother {
    public static Instance instance(String instanceId) {
        return new Instance().withInstanceId(instanceId);
    }

    public static Instance instance(String instanceId, String state, String platform) {
        return instance(instanceId)
                .withPlatform(platform)
                .withTags(new Tag("Name", format("GoCD_%s_INSTANCE", platform.toUpperCase())))
                .withState(new InstanceState().withName(state));
    }

    public static Instance runningInstance(String instanceId, Platform platform, Tag... tags) {
        return instance(instanceId)
                .withPlatform(platform.name())
                .withTags(tags)
                .withState(new InstanceState().withName(RUNNING));
    }

    public static Instance instance(String instanceId, InstanceType type, String imageId, Date launchTime) {
        return linuxInstance(instanceId, launchTime).withInstanceType(type).withImageId(imageId);
    }

    public static Instance spotInstance(String instanceId, String state, String platform) {
        return instance(instanceId)
                .withState(new InstanceState().withName(state))
                .withPlatform(platform)
                .withTags(new Tag("Name", format("GoCD_%s_SPOT_INSTANCE", platform.toUpperCase())))
                .withSpotInstanceRequestId("request_id");
    }

    public static Instance linuxInstance(String instanceId, String state) {
        return instance(instanceId, state, LINUX.name());
    }

    public static Instance runningLinuxInstance(String instanceId) {
        return linuxInstance(instanceId, RUNNING);
    }

    public static Instance runningLinuxSpotInstance(String instanceId) {
        return linuxInstance(instanceId, RUNNING).withSpotInstanceRequestId("req_id");
    }

    public static Instance linuxInstance(String instanceId, Date launchTime) {
        return linuxInstance(instanceId, RUNNING).withLaunchTime(launchTime);
    }

    public static Instance linuxInstance(String instanceId, String state, Date launchTime) {
        return linuxInstance(instanceId, launchTime).withState(new InstanceState().withName(state));
    }

    public static Instance linuxInstanceWithTag(String instanceId, Tag... tag) {
        return linuxInstance(instanceId, RUNNING).withTags(tag);
    }

    public static Instance linuxInstanceWithTag(String instanceId, String state, Tag... tag) {
        return linuxInstance(instanceId, state).withTags(tag);
    }

    public static Instance windowsInstance(String instanceId, String state) {
        return instance(instanceId, state, WINDOWS.name());
    }

    public static Instance runningWindowsInstance(String instanceId) {
        return windowsInstance(instanceId, RUNNING);
    }

    public static Instance runningWindowsSpotInstance(String instanceId) {
        return windowsInstance(instanceId, RUNNING).withSpotInstanceRequestId("req_id");
    }

    public static Instance windowsInstance(String instanceId, Date launchTime) {
        return windowsInstance(instanceId, RUNNING).withLaunchTime(launchTime);
    }

    public static Instance windowsInstanceWithTag(String instanceId, Tag... tag) {
        return windowsInstance(instanceId, RUNNING).withTags(tag);
    }
}
