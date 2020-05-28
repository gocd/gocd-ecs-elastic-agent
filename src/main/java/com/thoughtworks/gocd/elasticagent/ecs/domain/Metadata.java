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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.gocd.elasticagent.ecs.domain.annotation.FieldType;
import org.apache.commons.lang3.StringUtils;

public class Metadata {

    @Expose
    @SerializedName("key")
    private String key;

    @Expose
    @SerializedName("metadata")
    private ProfileMetadata metadata;

    public Metadata(String key, boolean required, boolean secure, FieldType type) {
        this(key, new ProfileMetadata(required, secure, type));
    }

    public Metadata(String key, ProfileMetadata metadata) {
        this.key = key;
        this.metadata = metadata;
    }

    public ValidationError validate(String input) {
        String errorMessage = doValidate(input);
        if (StringUtils.isNotBlank(errorMessage)) {
            return new ValidationError(key, errorMessage);
        }
        return null;
    }

    protected String doValidate(String input) {
        if (isRequired()) {
            if (StringUtils.isBlank(input)) {
                return this.key + " must not be blank.";
            }
        }

        if (StringUtils.isNotBlank(input)) {
            return metadata.type.validate(input);
        }

        return null;
    }


    public String getKey() {
        return key;
    }

    public boolean isRequired() {
        return metadata.required;
    }

    public static class ProfileMetadata {
        @Expose
        @SerializedName("required")
        private Boolean required;

        @Expose
        @SerializedName("secure")
        private Boolean secure;

        private FieldType type;

        public ProfileMetadata(boolean required, boolean secure, FieldType type) {
            this.required = required;
            this.secure = secure;
            this.type = type;
        }
    }
}
