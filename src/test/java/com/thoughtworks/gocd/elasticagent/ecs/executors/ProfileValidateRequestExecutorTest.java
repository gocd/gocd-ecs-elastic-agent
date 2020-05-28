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

import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ProfileValidateRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;
import org.skyscreamer.jsonassert.JSONCompareMode;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties.*;

class ProfileValidateRequestExecutorTest {
    @Test
    void shouldBarfWhenUnknownKeysArePassed() throws Exception {
        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(Collections.singletonMap("foo", "bar")));
        String json = executor.execute().responseBody();

        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"message\": \"Image must not be blank.\",\n" +
                "    \"key\": \"Image\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"message\": \"MaxMemory must not be blank.\",\n" +
                "    \"key\": \"MaxMemory\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"message\": \"ReservedMemory must not be blank.\",\n" +
                "    \"key\": \"ReservedMemory\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"message\": \"CPU must not be blank.\",\n" +
                "    \"key\": \"CPU\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"foo\",\n" +
                "    \"message\": \"Is an unknown property\"\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldValidateMandatoryKeys() throws Exception {
        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(Collections.emptyMap()));
        String json = executor.execute().responseBody();
        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"message\": \"Image must not be blank.\",\n" +
                "    \"key\": \"Image\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"message\": \"MaxMemory must not be blank.\",\n" +
                "    \"key\": \"MaxMemory\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"message\": \"ReservedMemory must not be blank.\",\n" +
                "    \"key\": \"ReservedMemory\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"message\": \"CPU must not be blank.\",\n" +
                "    \"key\": \"CPU\"\n" +
                "  }\n" +
                "]";
        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldValidateValidMemorySpecification() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "6G");
        properties.put(KEY_RESERVED_MEMORY, "5G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));

        String json = executor.execute().responseBody();
        JSONAssert.assertEquals("[]", json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldValidateIfMaxMemoryIsLessThanTheReservedMemory() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "4G");
        properties.put(KEY_RESERVED_MEMORY, "5G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));

        String json = executor.execute().responseBody();
        final String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"MaxMemory\",\n" +
                "    \"message\": \"Must be greater than or equal to `ReservedMemory`.\"\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldValidateIfMaxMemoryIsSpecifiedWithoutSizeUnitSuffix() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "1000");
        properties.put(KEY_RESERVED_MEMORY, "5G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));

        String json = executor.execute().responseBody();
        final String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"MaxMemory\",\n" +
                "    \"message\": \"Invalid size: `1000`. Must be a positive integer followed by unit (B, K, M, G or T)\"\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldValidateWhenProfileHasAllValuesThatSettingsDoesNotHave() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "4G");
        properties.put(KEY_RESERVED_MEMORY, "2G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");
        properties.put(AMI, "ami-1234");
        properties.put(INSTANCE_TYPE, "instanceType");
        properties.put(IAM_INSTANCE_PROFILE, "foobar");
        properties.put(SECURITY_GROUP_IDS, "foobar");
        properties.put(SUBNET_IDS, "foobar");

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));

        String json = executor.execute().responseBody();

        JSONAssert.assertEquals("[]", json, JSONCompareMode.NON_EXTENSIBLE);
    }

    //need to think about validating AMI ID and Instance type
    @Test
    @Disabled
    void shouldErrorOutWhenInstanceTypAndAMIAreNotSpecifiedInProfileOrPluginSettings() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "4G");
        properties.put(KEY_RESERVED_MEMORY, "2G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));
        String json = executor.execute().responseBody();

        final String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"AMI\",\n" +
                "    \"message\": \"AMI must not be blank.\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"key\": \"InstanceType\",\n" +
                "    \"message\": \"Instance type must not be blank.\"\n" +
                "  }\n" +
                "]";
        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);

    }

    @Test
    void shouldNotErrorOutForReservedMemoryWhenPlatformIsWindows() throws Exception {
        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(Collections.singletonMap(PLATFORM, Platform.WINDOWS.name())));
        String json = executor.execute().responseBody();
        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"message\": \"Image must not be blank.\",\n" +
                "    \"key\": \"Image\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"message\": \"MaxMemory must not be blank.\",\n" +
                "    \"key\": \"MaxMemory\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"message\": \"CPU must not be blank.\",\n" +
                "    \"key\": \"CPU\"\n" +
                "  }\n" +
                "]";
        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldErrorOutIfBindMountIsNotValid() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "4G");
        properties.put(KEY_RESERVED_MEMORY, "2G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");
        properties.put(BIND_MOUNT, "[{\"SourcePath\": \"/ecs/data\", \"ContainerPath\": \"/var/data\"}]");

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));
        String json = executor.execute().responseBody();

        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"BindMount\",\n" +
                "    \"message\": \"Invalid BindMount configuration:\nErrors in BindMount at index 0, 'Name cannot be empty.'\"" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldErrorOutIfBindMountIsNotValidJSON() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "4G");
        properties.put(KEY_RESERVED_MEMORY, "2G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");
        properties.put(BIND_MOUNT, "[{{\"ContainerPath\": \"/var/data\"}]");

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));
        String json = executor.execute().responseBody();

        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"BindMount\",\n" +
                "    \"message\": \"There were errors parsing the BindMount JSON, check if the given JSON is valid.\"\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldErrorOutIfEitherOfTheBindMountConfigurationsHasErrors_whenMultipleBindMountConfigsAreProvided() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "4G");
        properties.put(KEY_RESERVED_MEMORY, "2G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");

        String invalidConfig = "[\n" +
                "  {\n" +
                "    \"name\": \"data\",\n" +
                "    \"SourcePath\": \"/ecs/data\",\n" +
                "    \"ContainerPath\": \"/var/data\"\n" +
                "  },\n" +
                "  {\n" +
                "    \"SourcePath\": \"/ecs/data\",\n" +
                "    \"ContainerPath\": \"/var/data\"\n" +
                "  }\n" +
                "]";
        properties.put(BIND_MOUNT, invalidConfig);

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));
        String json = executor.execute().responseBody();

        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"BindMount\",\n" +
                "    \"message\": \"Invalid BindMount configuration:\nErrors in BindMount at index 0, 'Name cannot be empty.'\"\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldErrorOutIfSpotPriceStringIsNotParsable() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "4G");
        properties.put(KEY_RESERVED_MEMORY, "2G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");
        properties.put(RUN_AS_SPOT_INSTANCE, "true");
        properties.put(SPOT_PRICE, "4G");

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));
        String json = executor.execute().responseBody();

        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"SpotPrice\",\n" +
                "    \"message\": \"Error parsing Spot Price, should be a valid double.\"\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }

    @Test
    void shouldErrorOutIfSpotRequestExpiryStringIsNotParsable() throws Exception {
        final Map<String, String> properties = new HashMap<>();
        properties.put(KEY_MAX_MEMORY, "4G");
        properties.put(KEY_RESERVED_MEMORY, "2G");
        properties.put("CPU", "0");
        properties.put("Image", "alpine");
        properties.put(RUN_AS_SPOT_INSTANCE, "true");
        properties.put(SPOT_REQUEST_EXPIRES_AFTER, "4G");

        ProfileValidateRequestExecutor executor = new ProfileValidateRequestExecutor(new ProfileValidateRequest(properties));
        String json = executor.execute().responseBody();

        String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"SpotRequestExpiresAfter\",\n" +
                "    \"message\": \"Error parsing Spot Request Expires After, should be a valid integer.\"\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, json, JSONCompareMode.NON_EXTENSIBLE);
    }
}
