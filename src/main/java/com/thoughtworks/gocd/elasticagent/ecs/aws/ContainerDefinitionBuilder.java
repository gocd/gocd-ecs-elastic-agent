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

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.KeyValuePair;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.PluginSettings;
import com.thoughtworks.gocd.elasticagent.ecs.requests.CreateAgentRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.*;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.LINUX;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.Platform.WINDOWS;
import static java.text.MessageFormat.format;

public class ContainerDefinitionBuilder {
    private String taskName;
    private PluginSettings pluginSettings;
    private CreateAgentRequest request;
    private String serverId;

    public ContainerDefinition build() {
        final ElasticAgentProfileProperties elasticAgentProfileProperties = request.elasticProfile();
        final Integer memoryReservation = elasticAgentProfileProperties.platform() == LINUX ? elasticAgentProfileProperties.getReservedMemory() : null;

        return new ContainerDefinition().withName(taskName)
                .withImage(image(elasticAgentProfileProperties.getImage()))
                .withMemory(elasticAgentProfileProperties.getMaxMemory())
                .withMemoryReservation(memoryReservation)
                .withEnvironment(environmentFrom())
                .withDockerLabels(labelsFrom())
                .withCommand(elasticAgentProfileProperties.getCommand())
                .withCpu(elasticAgentProfileProperties.getCpu())
                .withPrivileged(elasticAgentProfileProperties.platform() != WINDOWS && elasticAgentProfileProperties.isPrivileged())
                .withLogConfiguration(pluginSettings.logConfiguration());
    }

    public ContainerDefinitionBuilder pluginSettings(PluginSettings pluginSettings) {
        this.pluginSettings = pluginSettings;
        return this;
    }

    public ContainerDefinitionBuilder createAgentRequest(CreateAgentRequest request) {
        this.request = request;
        return this;
    }

    private Collection<KeyValuePair> environmentFrom() {
        Collection<KeyValuePair> env = getKeyValuePairs(pluginSettings.getEnvironmentVariables());
        env.addAll(getKeyValuePairs(request.elasticProfile().getEnvironment()));
        env.add(new KeyValuePair().withName("GO_EA_MODE").withValue(mode()));
        env.add(new KeyValuePair().withName("GO_EA_SERVER_URL").withValue(pluginSettings.getGoServerUrl()));
        env.addAll(request.autoRegisterPropertiesAsEnvironmentVars(taskName));

        return env;
    }

    private Collection<KeyValuePair> getKeyValuePairs(Collection<String> lines) {
        Collection<KeyValuePair> env = new HashSet<>();
        for (String variable : lines) {
            if (StringUtils.contains(variable, "=")) {
                String[] pair = variable.split("=", 2);
                env.add(new KeyValuePair().withName(pair[0]).withValue(pair[1]));
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

    private String mode() {
        final boolean isCompressedJS = Boolean.parseBoolean(System.getProperty("rails.use.compressed.js"));
        return isCompressedJS ? "prod" : "dev";
    }

    private HashMap<String, String> labelsFrom() {
        final HashMap<String, String> labels = new HashMap<>();
        labels.put(CREATED_BY_LABEL_KEY, PLUGIN_ID);
        if (StringUtils.isNotBlank(request.environment())) {
            labels.put(ENVIRONMENT_LABEL_KEY, request.environment());
        }

        labels.put(CONFIGURATION_LABEL_KEY, request.elasticProfile().toJson());
        labels.put(LABEL_JOB_IDENTIFIER, request.getJobIdentifier().toJson());
        labels.put(LABEL_SERVER_ID, serverId);
        return labels;
    }

    public ContainerDefinitionBuilder withName(String taskName) {
        this.taskName = taskName;
        return this;
    }

    public ContainerDefinitionBuilder withServerId(String serverId) {
        this.serverId = serverId;
        return this;
    }
}
