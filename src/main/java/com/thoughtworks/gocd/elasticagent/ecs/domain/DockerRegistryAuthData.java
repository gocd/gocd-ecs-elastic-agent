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

import com.google.gson.GsonBuilder;
import lombok.EqualsAndHashCode;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@EqualsAndHashCode
public class DockerRegistryAuthData {
    private Map<String, Authorization> authData;

    public DockerRegistryAuthData(String url, String auth, String email) {
        this.authData = Collections.singletonMap(url, new Authorization(auth, email));
    }

    public DockerRegistryAuthData(String url, String username, String password, String email) {
        this.authData = Collections.singletonMap(url, new Authorization(username, password, email));
    }

    public String toJson() {
        return new GsonBuilder().create().toJson(authData);
    }

    private static class Authorization {
        private String auth;
        private String username;
        private String password;
        private String email;

        public Authorization(String auth, String email) {
            this.email = email;
            this.auth = auth;
        }

        public Authorization(String username, String password, String email) {
            this.username = username;
            this.password = password;
            this.email = email;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            Authorization that = (Authorization) o;

            if (!Objects.equals(auth, that.auth)) return false;
            if (!Objects.equals(username, that.username)) return false;
            if (!Objects.equals(password, that.password)) return false;
            return Objects.equals(email, that.email);
        }

        @Override
        public int hashCode() {
            int result = auth != null ? auth.hashCode() : 0;
            result = 31 * result + (username != null ? username.hashCode() : 0);
            result = 31 * result + (password != null ? password.hashCode() : 0);
            result = 31 * result + (email != null ? email.hashCode() : 0);
            return result;
        }
    }
}


