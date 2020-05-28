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

package com.thoughtworks.gocd.elasticagent.ecs;

import com.thoughtworks.go.plugin.api.GoPluginIdentifier;
import com.thoughtworks.gocd.elasticagent.ecs.info.PluginProperties;

import java.util.Collections;

public interface Constants {
    String PLUGIN_ID = PluginProperties.instance().get("id");

    // The type of this extension
    String EXTENSION_TYPE = "elastic-agent";

    // The extension point API version that this plugin understands
    String PROCESSOR_API_VERSION = "1.0";
    String EXTENSION_API_VERSION = "5.0";
    String SERVER_INFO_API_VERSION = "1.0";
    String CONSOLE_LOG_API_VERSION = "1.0";

    // the identifier of this plugin
    GoPluginIdentifier PLUGIN_IDENTIFIER = new GoPluginIdentifier(EXTENSION_TYPE, Collections.singletonList(EXTENSION_API_VERSION));

    // requests that the plugin makes to the server
    String REQUEST_SERVER_PREFIX = "go.processor";
    String REQUEST_SERVER_DISABLE_AGENT = REQUEST_SERVER_PREFIX + ".elastic-agents.disable-agents";
    String REQUEST_SERVER_DELETE_AGENT = REQUEST_SERVER_PREFIX + ".elastic-agents.delete-agents";
    String REQUEST_SERVER_LIST_AGENTS = REQUEST_SERVER_PREFIX + ".elastic-agents.list-agents";
    String REQUEST_SERVER_INFO = REQUEST_SERVER_PREFIX + ".server-info.get";
    String REQUEST_SERVER_APPEND_TO_CONSOLE_LOG = REQUEST_SERVER_PREFIX + ".console-log.append";

    String CREATED_BY_LABEL_KEY = "elastic-agent-created-by";
    String ENVIRONMENT_LABEL_KEY = "elastic-agent-environment-name";
    String CONFIGURATION_LABEL_KEY = "elastic-agent-configuration";
    String LABEL_JOB_IDENTIFIER = "job-identifier";
    String LABEL_SERVER_ID = "server-id";
    String LABEL_SPOT_REQUEST = "spot-request-name";

    String LAST_SEEN_IDLE = "LAST_SEEN_IDLE";
    String STOPPED_AT = "STOPPED_AT";
}
