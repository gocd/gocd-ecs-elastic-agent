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

package com.thoughtworks.gocd.elasticagent.ecs.requests;

import org.junit.jupiter.api.Test;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ValidateClusterProfileRequestTest {
    @Test
    void shouldDeserializeFromJSON() {
        String json = "{\n" +
                "    \"server_url\": \"http://localhost\", \n" +
                "    \"username\": \"bob\", \n" +
                "    \"password\": \"secret\"" +
                "}";

        ValidateClusterProfileRequest request = ValidateClusterProfileRequest.fromJSON(json);
        HashMap<String, String> expectedSettings = new HashMap<>();
        expectedSettings.put("server_url", "http://localhost");
        expectedSettings.put("username", "bob");
        expectedSettings.put("password", "secret");
        assertThat(request).isEqualTo(expectedSettings);
    }
}
