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

package com.thoughtworks.gocd.elasticagent.ecs.reports;

import com.google.gson.JsonParser;
import com.thoughtworks.go.plugin.api.response.GoPluginApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.builders.PluginStatusReportViewBuilder;
import freemarker.template.TemplateException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StatusReportGenerationErrorHandlerTest {

    @Test
    void shouldGenerateErrorViewForException() {
        final StatusReportGenerationException exception = StatusReportGenerationException.noRunningTask("foo");

        final GoPluginApiResponse response = StatusReportGenerationErrorHandler.handle(PluginStatusReportViewBuilder.instance(), exception);

        assertThat(response.responseCode()).isEqualTo(200);

        final String view = JsonParser.parseString(response.responseBody()).getAsJsonObject().get("view").getAsString();
        final Document document = Jsoup.parse(view);

        assertThat(document.select(".outer-container .container .error-container blockquote header").text())
                .isEqualTo("ECS task is not running.");
        assertThat(document.select(".outer-container .container .error-container blockquote p").text())
                .isEqualTo("Can not find a running task for the provided elastic agent id 'foo'.");
    }

    @Test
    void shouldReturnErrorResponseWhenItFailsToGenerateErrorView() throws IOException, TemplateException {
        final StatusReportGenerationException exception = StatusReportGenerationException.noRunningTask("foo");
        final PluginStatusReportViewBuilder viewBuilder = mock(PluginStatusReportViewBuilder.class);

        when(viewBuilder.build(any(), any()))
                .thenThrow(new RuntimeException("error-in-generating-view"));

        final GoPluginApiResponse response = StatusReportGenerationErrorHandler.handle(viewBuilder, exception);

        assertThat(response.responseCode()).isEqualTo(500);
        assertThat(response.responseBody()).isEqualTo("Failed to generate error report: com.thoughtworks.gocd.elasticagent.ecs.reports.StatusReportGenerationException: ECS task is not running.");
    }
}
