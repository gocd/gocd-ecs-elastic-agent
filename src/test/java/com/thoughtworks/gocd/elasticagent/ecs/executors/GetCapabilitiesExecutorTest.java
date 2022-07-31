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

import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

class GetCapabilitiesExecutorTest {

    @Test
    void shouldAbleSupportPasswordAndSearchCapabilities() throws Exception {
        GoPluginApiResponse response = new GetCapabilitiesExecutor().execute();

        String expectedJSON = "{\n" +
                "    \"supports_plugin_status_report\":false,\n" +
                "    \"supports_cluster_status_report\":true,\n" +
                "    \"supports_agent_status_report\":true\n" +
                "}";

        JSONAssert.assertEquals(expectedJSON, response.responseBody(), true);
    }
}
