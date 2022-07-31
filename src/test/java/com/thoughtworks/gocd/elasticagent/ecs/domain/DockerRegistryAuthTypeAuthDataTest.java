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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

class DockerRegistryAuthTypeAuthDataTest {

    @Test
    void shouldDeserializeDockerCnfAuthData() throws Exception {
        DockerRegistryAuthData authData = new DockerRegistryAuthData("https://index.docker.io/v1/", "zq212MzEXAMPLE7o6T25Dk0i", "email@example.com");

        String expectedJson = "{\n" +
                "  \"https://index.docker.io/v1/\": {\n" +
                "    \"auth\": \"zq212MzEXAMPLE7o6T25Dk0i\",\n" +
                "    \"email\": \"email@example.com\"\n" +
                "  }\n" +
                "}";

        JSONAssert.assertEquals(expectedJson, authData.toJson(), true);
    }

    @Test
    void shouldDeserializeToDockerAuthData() throws Exception {
        DockerRegistryAuthData authData = new DockerRegistryAuthData("https://index.docker.io/v1/", "username", "my_password", "email@example.com");

        String expectedJson = "{\n" +
                "  \"https://index.docker.io/v1/\": {\n" +
                "    \"username\": \"username\",\n" +
                "    \"password\": \"my_password\",\n" +
                "    \"email\": \"email@example.com\"\n" +
                "  }\n" +
                "}";

        JSONAssert.assertEquals(expectedJson, authData.toJson(), true);
    }
}
