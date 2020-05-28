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

package com.thoughtworks.gocd.elasticagent.ecs.domain.annotation;

import com.thoughtworks.gocd.elasticagent.ecs.size.Size;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.google.common.base.Preconditions.checkArgument;
import static java.text.MessageFormat.format;

public enum FieldType {
    STRING {
        @Override
        public boolean doValidate(String value) {
            return true;
        }
    },

    POSITIVE_DECIMAL {
        @Override
        public boolean doValidate(String value) {
            return Long.parseLong(value) >= 0;
        }
    },

    NUMBER {
        @Override
        public boolean doValidate(String value) {
            Double.parseDouble(value);
            return true;
        }
    },

    MEMORY {
        @Override
        public boolean doValidate(String value) {
            final Size size = Size.parse(value);
            checkArgument(!(size.toMegabytes() < 4), format("Invalid size: `{0}`. Minimum size for container to start is 4M", value));
            return true;
        }

        protected String getErrorMessage(Exception e) {
            return e.getMessage();
        }
    };

    private static final Map<FieldType, String> errorsMap = Collections.unmodifiableMap(new HashMap<FieldType, String>() {{
        put(STRING, null);
        put(NUMBER, "must be number");
        put(POSITIVE_DECIMAL, "must be positive decimal");
        put(MEMORY, "invalid memory size");
    }});

    public String validate(String value) {
        try {
            return doValidate(value) ? null : errorsMap.get(this);
        } catch (Exception e) {
            return getErrorMessage(e);
        }
    }

    protected abstract boolean doValidate(String value);

    protected String getErrorMessage(Exception e) {
        return errorsMap.get(this);
    }
}
