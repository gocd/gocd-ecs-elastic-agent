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

package com.thoughtworks.gocd.elasticagent.ecs.executors;

import com.google.gson.JsonObject;
import com.thoughtworks.go.plugin.api.response.DefaultGoPluginApiResponse;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin;
import com.thoughtworks.gocd.elasticagent.ecs.RequestExecutor;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;

import java.util.function.Supplier;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.AWSCredentialsProviderChain.EXTERNAL_ID_PREFIX;
import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.GSON;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class GetClusterProfileViewRequestExecutor implements RequestExecutor {
    static final String EXTERNAL_ID_PLACEHOLDER = "__GOCD_EXTERNAL_ID__";

    private final Supplier<String> serverIdSupplier;

    public GetClusterProfileViewRequestExecutor() {
        this(ECSElasticPlugin::getServerId);
    }

    //used in test
    GetClusterProfileViewRequestExecutor(Supplier<String> serverIdSupplier) {
        this.serverIdSupplier = serverIdSupplier;
    }

    @Override
    public GoPluginApiResponse execute() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("template", Util.readResource("/cluster-profile.template.html").replace(EXTERNAL_ID_PLACEHOLDER, externalId()));
        return DefaultGoPluginApiResponse.success(GSON.toJson(jsonObject));
    }

    private String externalId() {
        final String serverId = serverIdSupplier.get();
        return EXTERNAL_ID_PREFIX + (isBlank(serverId) ? "&lt;your GoCD server id&gt;" : serverId);
    }
}
