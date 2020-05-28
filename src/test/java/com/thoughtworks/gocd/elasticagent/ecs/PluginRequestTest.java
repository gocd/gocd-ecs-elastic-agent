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

import com.thoughtworks.go.plugin.api.GoApplicationAccessor;
import com.thoughtworks.go.plugin.api.request.DefaultGoApiRequest;
import com.thoughtworks.go.plugin.api.response.DefaultGoApiResponse;
import com.thoughtworks.gocd.elasticagent.ecs.domain.ServerInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static com.thoughtworks.gocd.elasticagent.ecs.Constants.REQUEST_SERVER_INFO;
import static com.thoughtworks.gocd.elasticagent.ecs.Constants.SERVER_INFO_API_VERSION;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PluginRequestTest {
    private GoApplicationAccessor accessor;
    private PluginRequest pluginRequest;

    @BeforeEach
    void setUp() {
        accessor = mock(GoApplicationAccessor.class);
        pluginRequest = new PluginRequest(accessor);
    }

    @Test
    void shouldGetServerInfoFromServer() {
        ArgumentCaptor<DefaultGoApiRequest> requestArgumentCaptor = ArgumentCaptor.forClass(DefaultGoApiRequest.class);
        when(accessor.submit(requestArgumentCaptor.capture())).thenReturn(DefaultGoApiResponse.success("{\n" +
                "  \"server_id\": \"foobar\",\n" +
                "  \"site_url\": \"http://your.server.url\",\n" +
                "  \"secure_site_url\": \"https://your.server.url\"" +
                "}"));

        final ServerInfo severInfo = pluginRequest.getSeverInfo();

        DefaultGoApiRequest request = requestArgumentCaptor.getValue();
        assertThat(request.api()).isEqualTo(REQUEST_SERVER_INFO);
        assertThat(request.apiVersion()).isEqualTo(SERVER_INFO_API_VERSION);

        assertThat(severInfo.getServerId()).isEqualTo("foobar");
        assertThat(severInfo.getSiteUrl()).isEqualTo("http://your.server.url");
        assertThat(severInfo.getSecureSiteUrl()).isEqualTo("https://your.server.url");
    }
}
