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

package com.thoughtworks.gocd.elasticagent.ecs.validators;

import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractValidator implements Validator {
    private static final String KEY = "key";
    private static final String MESSAGE = "message";
    private final List<Map<String, String>> errorResult = new ArrayList<>();

    public List<Map<String, String>> validate(ValidateClusterProfileRequest request) {
        validatePluginSettings(request);
        return errorResult;
    }

    abstract void validatePluginSettings(ValidateClusterProfileRequest request);

    final void addError(String key, String message) {
        Map<String, String> result = new HashMap<>();
        result.put(KEY, key);
        result.put(MESSAGE, message);
        errorResult.add(result);
    }

    final void assertNotBlank(String key, String value) {
        if (StringUtils.isBlank(value)) {
            addError(key, key + " must not be blank.");
        }
    }

    final boolean hasError(String key) {
        return errorResult.stream().anyMatch(it -> key.equals(it.get(KEY)));
    }
}
