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
import com.thoughtworks.gocd.elasticagent.ecs.domain.ElasticAgentProfileProperties;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Metadata;
import com.thoughtworks.gocd.elasticagent.ecs.domain.MetadataExtractor;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.lang.String.format;
import static org.assertj.core.api.Assertions.assertThat;

class GetProfileViewExecutorTest {

    private String template;
    private Document document;

    @BeforeEach
    void setUp() {
        template = Util.readResource("/profile.template.html");
        document = Jsoup.parse(template);
    }

    @Test
    void shouldRenderTheTemplateInJSON() throws Exception {
        GoPluginApiResponse response = new GetProfileViewExecutor().execute();
        assertThat(response.responseCode()).isEqualTo(200);
        Map<String, String> hashSet = new Gson().fromJson(response.responseBody(), new TypeToken<HashMap<String, String>>() {
        }.getType());
        assertThat(hashSet).containsEntry("template", template);
    }

    @Test
    void shouldHaveRadioDefinedForOSSelection() {
        final Elements Platform = document.getElementsByAttributeValue("ng-model", "Platform");

        assertThat(Platform).hasSize(2);

        final Element linuxOS = Platform.get(0);
        assertThat(linuxOS.attr("id")).isEqualTo("linux-os");
        assertThat(linuxOS.attr("value")).isEqualTo("linux");

        final Element windowsOS = Platform.get(1);
        assertThat(windowsOS.attr("id")).isEqualTo("windows-os");
        assertThat(windowsOS.attr("value")).isEqualTo("windows");

        assertThat(document.getElementsByAttributeValue("for", "linux-os").text()).isEqualTo("Linux");
        assertThat(document.getElementsByAttributeValue("for", "windows-os").text()).isEqualTo("Windows");
    }

    @ParameterizedTest
    @MethodSource("fieldArguments")
    void shouldHaveInputDefinedForDockerImage(String fieldName, String labelText) {
        final Element image = document.getElementsByAttributeValue("ng-model", fieldName).get(0);
        assertThat(image).isNotNull();

        assertThat(image.attr("ng-class"))
                .describedAs(format("ng-class attribute is not defined on %s", fieldName))
                .isEqualTo(format("{'is-invalid-input': GOINPUTNAME[%s].$error.server}", fieldName));

        final Elements div = image.parents();

        final Element label = div.select("label").get(0);
        assertThat(label.text()).contains(labelText);
        assertThat(label.attr("ng-class"))
                .isEqualTo(format("{'is-invalid-label': GOINPUTNAME[%s].$error.server}", fieldName));

        final Element errorSpan = div.select("span[class='form_error form-error']").get(0);
        assertThat(errorSpan.text()).isEqualTo(format("{{GOINPUTNAME[%s].$error.server}}", fieldName));
        assertThat(errorSpan.attr("ng-class")).isEqualTo(format("{'is-visible': GOINPUTNAME[%s].$error.server}", fieldName));
        assertThat(errorSpan.attr("ng-show")).isEqualTo(format("GOINPUTNAME[%s].$error.server", fieldName));

    }

    static Stream<Arguments> fieldArguments() {
        return Stream.of(
                Arguments.of("Image", "Docker image"),
                Arguments.of("MaxMemory", "Hard memory limit for container"),
                Arguments.of("ReservedMemory", "Reserved memory for container"),
                Arguments.of("CPU", "CPU"),
                Arguments.of("MountDockerSocket", "Mount Docker Socket"),
                Arguments.of("Privileged", "Privileged"),
                Arguments.of("Command", "Docker Command"),
                Arguments.of("Environment", "Environment Variables"),

                Arguments.of("AMI", "AMI ID"),
                Arguments.of("InstanceType", "Instance type"),
                Arguments.of("IAMInstanceProfile", "IAM Instance Profile"),
                Arguments.of("SecurityGroupIds", "Security Group Id(s)"),
                Arguments.of("SubnetIds", "Subnet id(s)")
        );
    }

    @Test
    void allFieldsShouldBePresentInView() {
        final List<Metadata> metadataList = new MetadataExtractor().extract(ElasticAgentProfileProperties.class);
        final String[] fieldsFromElasticProfile = metadataList.stream().map(Metadata::getKey).toArray(String[]::new);

        final Elements inputs = document.select("textarea,input,select");

        final Set<String> fieldsDefinedInHTML = inputs.stream().map(e -> e.attr("ng-model")).collect(Collectors.toSet());

        assertThat(fieldsDefinedInHTML)
                .describedAs("should contains only inputs that defined in ElasticAgentProfileProperties.java")
                .hasSize(fieldsFromElasticProfile.length)
                .containsExactlyInAnyOrder(fieldsFromElasticProfile);
    }
}
