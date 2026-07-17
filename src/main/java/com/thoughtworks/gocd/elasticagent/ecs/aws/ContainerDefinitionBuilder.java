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

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.Strings;
import software.amazon.awssdk.services.ecs.model.ContainerDefinition;
import software.amazon.awssdk.services.ecs.model.KeyValuePair;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.*;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static java.text.MessageFormat.format;

public class ContainerDefinitionBuilder {
    private static final Logger LOG = Logger.getLoggerFor(ContainerDefinitionBuilder.class);

    private CreateAgentRequest request;

    private String taskName;
    private PluginSettings pluginSettings;
    private String serverId;

    public ContainerDefinitionBuilder(CreateAgentRequest createAgentRequest) {
        this.request = createAgentRequest;
    }

    public ContainerDefinition.Builder build() {
        final PlacementRequirement placementRequirement = buildPlacementRequirement();

        return ContainerDefinition.builder()
                .name(taskName)
                .image(image(request.elasticProfile().getImage()))
                .cpu(placementRequirement.cpu())
                .memory(placementRequirement.memory())
                .memoryReservation(placementRequirement.memoryReservation())
                .environment(environmentFrom())
                .dockerLabels(labelsFrom())
                .command(request.elasticProfile().getCommand())
                .privileged(request.elasticProfile().platform() != WINDOWS && request.elasticProfile().isPrivileged())
                .logConfiguration(pluginSettings.logConfiguration());
    }

    public PlacementRequirement buildPlacementRequirement() {
        return new PlacementRequirement(
                request.elasticProfile().getCpu(),
                request.elasticProfile().getMaxMemory(),
                request.elasticProfile().platform() == LINUX ? request.elasticProfile().getReservedMemory() : null
        );
    }

    public ContainerDefinitionBuilder pluginSettings(PluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
        return this;
    }

    private Collection<KeyValuePair> environmentFrom() {
        Collection<KeyValuePair> env = getKeyValuePairs(pluginSettings.getEnvironmentVariables());
        env.addAll(getKeyValuePairs(request.elasticProfile().getEnvironment()));
        env.add(KeyValuePair.builder().name("GO_EA_SERVER_URL").value(pluginSettings.getGoServerUrl()).build());
        env.addAll(request.autoRegisterPropertiesAsEnvironmentVars(taskName));

        return env;
    }

    private Collection<KeyValuePair> getKeyValuePairs(Collection<String> lines) {
        Collection<KeyValuePair> env = new HashSet<>();
        for (String variable : lines) {
            if (Strings.CS.contains(variable, "=")) {
                String[] pair = variable.split("=", 2);
                env.add(KeyValuePair.builder().name(pair[0]).value(pair[1]).build());
            } else {
                LOG.warn(format("Ignoring variable {0} as it is not in acceptable format, Variable must follow `VARIABLE_NAME=VARIABLE_VALUE` format.", variable));
            }
        }
        return env;
    }

    private String image(String image) {
        if (StringUtils.isBlank(image)) {
            throw new IllegalArgumentException("Must provide `Image` attribute.");
        }

        if (!image.contains(":")) {
            image = image + ":latest";
        }
        return image;
    }

    private Map<String, String> labelsFrom() {
        final Map<String, String> labels = new HashMap<>();
        labels.put(CREATED_BY_LABEL_KEY, PLUGIN_ID);
        if (StringUtils.isNotBlank(request.environment())) {
            labels.put(ENVIRONMENT_LABEL_KEY, request.environment());
        }

        labels.put(CONFIGURATION_LABEL_KEY, request.elasticProfile().toJson());
        labels.put(LABEL_JOB_IDENTIFIER, request.getJobIdentifier().toJson());
        labels.put(LABEL_SERVER_ID, serverId);
        return labels;
    }

    public ContainerDefinitionBuilder name(String taskName) {
        this.taskName = taskName;
        return this;
    }

    public ContainerDefinitionBuilder serverId(String serverId) {
        this.serverId = serverId;
        return this;
    }

    public record PlacementRequirement(Integer cpu, Integer memory, Integer memoryReservation) {
        public PlacementRequirement() {
            this(null, null, null);
        }
    }
}
