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

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import lombok.EqualsAndHashCode;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.GSON;

@EqualsAndHashCode
public class ServerInfo {
    @Expose
    @SerializedName("server_id")
    private String serverId;

    @Expose
    @SerializedName("site_url")
    private String siteUrl;

    @Expose
    @SerializedName("secure_site_url")
    private String secureSiteUrl;

    public String getServerId() {
        return serverId;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public String getSecureSiteUrl() {
        return secureSiteUrl;
    }

    public void setSecureSiteUrl(String secureSiteUrl) {
        this.secureSiteUrl = secureSiteUrl;
    }

    public static ServerInfo fromJSON(String json) {
        return GSON.fromJson(json, ServerInfo.class);
    }

    public String toJSON() {
        return GSON.toJson(this);
    }
}
