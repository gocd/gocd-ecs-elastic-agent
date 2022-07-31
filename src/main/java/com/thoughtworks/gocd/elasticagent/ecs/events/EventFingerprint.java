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

package com.thoughtworks.gocd.elasticagent.ecs.events;

import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import lombok.EqualsAndHashCode;

import static java.text.MessageFormat.format;

@EqualsAndHashCode
public class EventFingerprint {
    String fingerprint;

    public EventFingerprint(String fingerprint) {
        this.fingerprint = fingerprint;
    }

    public static EventFingerprint forElasticProfile(ElasticAgentProfileProperties profile) {
        return new EventFingerprint(profile.toJson());
    }

    public static EventFingerprint forStatusReport() {
        return new EventFingerprint("status_report");
    }

    public static EventFingerprint forTerminatingIdleEC2Instances() {
        return new EventFingerprint("terminate_idle_ec2_instances");
    }

    public static EventFingerprint forEnsureClusterMinSize() {
        return new EventFingerprint("ensure_cluster_min_size");
    }

    public static EventFingerprint forTerminateAgent(String agentId) {
        return new EventFingerprint(format("terminate_{0}", agentId));
    }

    public static EventFingerprint forRefreshContainers() {
        return new EventFingerprint("refresh_all_containers");
    }

    public static EventFingerprint forCreateEC2Instance() {
        return new EventFingerprint("create_ec2_instance");
    }
}
