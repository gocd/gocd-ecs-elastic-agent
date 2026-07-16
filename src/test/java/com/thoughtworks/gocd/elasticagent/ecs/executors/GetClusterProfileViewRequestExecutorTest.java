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

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetClusterProfileViewRequestExecutorTest {
    @Test
    void shouldRenderTheTemplateInJSON() {
        GoPluginApiResponse response = new GetClusterProfileViewRequestExecutor(() -> "some-server-id").execute();
        assertThat(response.responseCode()).isEqualTo(200);
        Map<String, String> hashSet = new Gson().fromJson(response.responseBody(), new TypeToken<HashMap<String, String>>() {
        }.getType());
        assertThat(hashSet).containsEntry("template", Util.readResource("/cluster-profile.template.html")
                .replace(GetClusterProfileViewRequestExecutor.EXTERNAL_ID_PLACEHOLDER, "gocd:server-id:some-server-id"));
    }

    @Test
    void shouldRenderTheServerIdBasedExternalIdWithinTheTemplate() {
        String template = templateRenderedWithServerId("some-server-id");

        assertThat(template).contains("<code>sts:ExternalId = \"gocd:server-id:some-server-id\"</code>");
        assertThat(template).doesNotContain(GetClusterProfileViewRequestExecutor.EXTERNAL_ID_PLACEHOLDER);
    }

    @Test
    void shouldRenderAGenericExternalIdIfTheServerIdIsNotAvailable() {
        String template = templateRenderedWithServerId(null);

        assertThat(template).contains("<code>sts:ExternalId = \"gocd:server-id:&lt;your GoCD server id&gt;\"</code>");
        assertThat(template).doesNotContain(GetClusterProfileViewRequestExecutor.EXTERNAL_ID_PLACEHOLDER);
    }

    private String templateRenderedWithServerId(String serverId) {
        GoPluginApiResponse response = new GetClusterProfileViewRequestExecutor(() -> serverId).execute();
        Map<String, String> body = new Gson().fromJson(response.responseBody(), new TypeToken<HashMap<String, String>>() {
        }.getType());
        return body.get("template");
    }
}
