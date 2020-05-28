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

package com.thoughtworks.gocd.elasticagent.ecs.executors;


import com.google.gson.Gson;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.RequestExecutor;
import com.thoughtworks.gocd.elasticagent.ecs.domain.*;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ProfileValidateRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.*;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties.*;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;
import static org.apache.commons.lang3.StringUtils.isNotBlank;

public class ProfileValidateRequestExecutor implements RequestExecutor {
    private static final Gson GSON = new Gson();
    private final ProfileValidateRequest request;

    public ProfileValidateRequestExecutor(ProfileValidateRequest request) {
        this.request = request;
    }

    @Override
    public GoPluginApiResponse execute() throws Exception {
        ValidationResult validationResult = validate(request.getProperties());
        return DefaultGoPluginApiResponse.success(validationResult.toJSON());
    }

    private ValidationResult validate(Map<String, String> configuration) {
        ElasticAgentProfileProperties elasticAgentProfileProperties = ElasticAgentProfileProperties.fromJson(GSON.toJson(configuration));

        ValidationResult validationResult = validateMetadata(configuration);

        validateReservedMemory(elasticAgentProfileProperties, validationResult);

        validateBindMounts(elasticAgentProfileProperties, validationResult);

        validateSpotInstanceConfiguration(elasticAgentProfileProperties, validationResult);

        return validationResult;
    }

    private void validateBindMounts(ElasticAgentProfileProperties elasticAgentProfileProperties, ValidationResult validationResult) {
        try {
            List<BindMount> bindMounts = elasticAgentProfileProperties.bindMounts();

            if (bindMounts.isEmpty()) {
                return;
            }

            boolean isInValid = bindMounts.stream().anyMatch(bindMount -> !bindMount.isValid());
            if (isInValid) {
                validationResult.addError("BindMount", format("Invalid BindMount configuration:%s", bindMountErrors(bindMounts)));
            }
        } catch (Exception e) {
            validationResult.addError("BindMount", "There were errors parsing the BindMount JSON, check if the given JSON is valid.");
        }
    }

    private String bindMountErrors(List<BindMount> bindMounts) {
        StringBuilder errors = new StringBuilder();
        for (int i = 0; i < bindMounts.size(); i++) {
            BindMount bindMount = bindMounts.get(i);
            if (!bindMount.errors().isEmpty()) {
                errors.append(format("\nErrors in BindMount at index %s, '%s'", i, bindMount.errors()));
            }
        }

        return errors.toString();
    }

    private void validateReservedMemory(ElasticAgentProfileProperties elasticAgentProfileProperties, ValidationResult validationResult) {
        if (elasticAgentProfileProperties.platform() == Platform.WINDOWS) {
            return;
        }

        if (elasticAgentProfileProperties.getReservedMemory() == null) {
            validationResult.addError(KEY_RESERVED_MEMORY, "ReservedMemory must not be blank.");
        } else if (elasticAgentProfileProperties.getMaxMemory() != null
                && elasticAgentProfileProperties.getMaxMemory() < elasticAgentProfileProperties.getReservedMemory()) {
            validationResult.addError(KEY_MAX_MEMORY, "Must be greater than or equal to `ReservedMemory`.");
        }
    }

    private void validateSpotInstanceConfiguration(ElasticAgentProfileProperties elasticAgentProfileProperties, ValidationResult validationResult) {
        if(elasticAgentProfileProperties.runAsSpotInstance()) {
            try {
                if(isNotBlank(elasticAgentProfileProperties.getSpotPrice())) {
                    parseDouble(elasticAgentProfileProperties.getSpotPrice());
                }
            } catch (NumberFormatException e) {
                validationResult.addError(SPOT_PRICE, "Error parsing Spot Price, should be a valid double.");
            }

            try {
                elasticAgentProfileProperties.getSpotRequestExpiresAfter();
            } catch (Exception e) {
                validationResult.addError(SPOT_REQUEST_EXPIRES_AFTER, "Error parsing Spot Request Expires After, should be a valid integer.");
            }
        }
    }

    private ValidationResult validateMetadata(Map<String, String> configuration) {
        final ValidationResult validationResult = new ValidationResult();
        final List<String> knownFields = new ArrayList<>();

        for (Metadata field : new MetadataExtractor().extract(ElasticAgentProfileProperties.class)) {
            knownFields.add(field.getKey());
            validationResult.addError(field.validate(configuration.get(field.getKey())));
        }

        Set<String> unknownFields = new HashSet<>(configuration.keySet());
        unknownFields.removeAll(knownFields);

        if (!unknownFields.isEmpty()) {
            for (String key : unknownFields) {
                validationResult.addError(key, "Is an unknown property");
            }
        }

        return validationResult;
    }
}
