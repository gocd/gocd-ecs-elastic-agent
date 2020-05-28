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
import com.google.gson.reflect.TypeToken;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.fields.Field;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import org.assertj.core.api.ListAssert;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class GetPluginSettingsViewRequestExecutorTest {

    @Test
    void shouldRenderTheTemplateInJSON() throws Exception {
        GoPluginApiResponse response = new GetClusterProfileViewRequestExecutor().execute();
        assertThat(response.responseCode()).isEqualTo(200);
        Map<String, String> hashSet = new Gson().fromJson(response.responseBody(), new TypeToken<HashMap<String, String>>() {
        }.getType());
        assertThat(hashSet).containsEntry("template", Util.readResource("/cluster-profile.template.html"));
    }

    @Test
    void allFieldsShouldBePresentInView() {
        String template = Util.readResource("/cluster-profile.template.html");
        final Document document = Jsoup.parse(template);

        for (Map.Entry<String, Field> fieldEntry : GetPluginConfigurationExecutor.FIELDS_MAP.entrySet()) {
            final Elements inputFieldForKey = document.getElementsByAttributeValue("ng-model", fieldEntry.getKey());

            final ListAssert<Element> listAssert = assertThat(inputFieldForKey)
                    .describedAs("For field " + fieldEntry.getKey());
            if (fieldEntry.getKey().equals("PrivateDockerRegistryAuthType")) {
                //has 3 radio buttons
                listAssert.hasSize(3);
            } else {
                listAssert.hasSize(1);
            }

            final Elements spanToShowError = document.getElementsByAttributeValue("ng-show", "GOINPUTNAME[" + fieldEntry.getKey() + "].$error.server");
            assertThat(spanToShowError).hasSize(1);
            assertThat(spanToShowError.text()).isEqualTo("{{GOINPUTNAME[" + fieldEntry.getKey() + "].$error.server}}");
        }
    }

}
