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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import software.amazon.awssdk.services.ec2.model.*;

import java.time.Instant;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static java.lang.String.format;
import static software.amazon.awssdk.services.ec2.model.InstanceStateName.RUNNING;

public class InstanceMother {
    public static Instance instance(String instanceId) {
        return instanceBuilder(instanceId).build();
    }

    public static Instance instance(String instanceId, InstanceStateName state, String platform) {
        return instanceBuilder(instanceId, state, platform)
                .build();
    }

    public static Instance runningInstance(String instanceId, Platform platform, Tag... tags) {
        return instanceBuilder(instanceId)
                .platform(platform.name())
                .tags(tags)
                .state(InstanceState.builder().name(RUNNING).build())
                .build();
    }

    public static Instance instance(String instanceId, InstanceType type, String imageId, Instant launchTime) {
        return linuxInstanceBuilder(instanceId, RUNNING, launchTime).instanceType(type).imageId(imageId).build();
    }

    public static Instance spotInstance(String instanceId, InstanceStateName state, String platform) {
        return spotInstanceBuilder(instanceId, state, platform)
                .build();
    }

    public static Instance.Builder spotInstanceBuilder(String instanceId, InstanceStateName state, String platform) {
        return instanceBuilder(instanceId)
                .state(InstanceState.builder().name(state).build())
                .platform(platform)
                .tags(Tag.builder().key("Name").value(format("GoCD_%s_SPOT_INSTANCE", platform.toUpperCase())).build())
                .spotInstanceRequestId("request_id");
    }

    public static Instance linuxInstance(String instanceId, InstanceStateName state) {
        return linuxInstanceBuilder(instanceId, state).build();
    }


    public static Instance runningLinuxInstance(String instanceId) {
        return linuxInstance(instanceId, RUNNING);
    }

    public static Instance runningLinuxSpotInstance(String instanceId) {
        return linuxInstanceBuilder(instanceId, RUNNING).spotInstanceRequestId("req_id").build();
    }

    public static Instance linuxInstance(String instanceId, Instant launchTime) {
        return linuxInstanceBuilder(instanceId, RUNNING).launchTime(launchTime).build();
    }

    public static Instance linuxInstance(String instanceId, InstanceStateName state, Instant launchTime) {
        return linuxInstanceBuilder(instanceId, state, launchTime).state(InstanceState.builder().name(state).build()).build();
    }

    public static Instance linuxInstanceWithTag(String instanceId, Tag... tag) {
        return linuxInstanceBuilder(instanceId, RUNNING).tags(tag).build();
    }

    public static Instance linuxInstanceWithTag(String instanceId, InstanceStateName state, Tag... tag) {
        return linuxInstanceBuilder(instanceId, state).tags(tag).build();
    }

    public static Instance windowsInstance(String instanceId, InstanceStateName state) {
        return windowsInstanceBuilder(instanceId, state).build();
    }

    public static Instance runningWindowsInstance(String instanceId) {
        return windowsInstance(instanceId, RUNNING);
    }

    public static Instance runningWindowsSpotInstance(String instanceId) {
        return windowsInstanceBuilder(instanceId, RUNNING).spotInstanceRequestId("req_id").build();
    }

    public static Instance windowsInstance(String instanceId, Instant launchTime) {
        return windowsInstanceBuilder(instanceId, RUNNING).launchTime(launchTime).build();
    }

    public static Instance windowsInstanceWithTag(String instanceId, Tag... tag) {
        return windowsInstanceBuilder(instanceId, RUNNING).tags(tag).build();
    }

    private static Instance.Builder instanceBuilder(String instanceId) {
        return Instance.builder().instanceId(instanceId);
    }

    public static Instance.Builder instanceBuilder(String instanceId, InstanceStateName state, String platform) {
        return instanceBuilder(instanceId)
                .platform(platform)
                .tags(Tag.builder().key("Name").value(format("GoCD_%s_INSTANCE", platform.toUpperCase())).build())
                .state(InstanceState.builder().name(state).build());
    }

    private static Instance.Builder linuxInstanceBuilder(String instanceId, InstanceStateName state) {
        return instanceBuilder(instanceId, state, LINUX.name());
    }

    private static Instance.Builder linuxInstanceBuilder(String instanceId, InstanceStateName state, Instant launchTime) {
        return linuxInstanceBuilder(instanceId, state).launchTime(launchTime);
    }

    private static Instance.Builder windowsInstanceBuilder(String instanceId, InstanceStateName state) {
        return instanceBuilder(instanceId, state, WINDOWS.name());
    }
}
