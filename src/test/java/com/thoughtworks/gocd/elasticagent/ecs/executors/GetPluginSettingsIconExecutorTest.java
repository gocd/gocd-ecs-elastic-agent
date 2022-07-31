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
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

class GetPluginSettingsIconExecutorTest {

    @Test
    void rendersIconInBase64() throws Exception {
        GoPluginApiResponse response = new GetPluginSettingsIconExecutor().execute();
        HashMap<String, String> hashMap = new Gson().fromJson(response.responseBody(), new TypeToken<HashMap<String, String>>() {
        }.getType());

        assertThat(hashMap).hasSize(2);
        assertThat(hashMap).containsEntry("content_type", "image/svg+xml");
        assertThat(Util.readResource("/ecs-plain.svg")).isEqualTo(new String(Base64.decodeBase64(hashMap.get("data"))));
    }
}
