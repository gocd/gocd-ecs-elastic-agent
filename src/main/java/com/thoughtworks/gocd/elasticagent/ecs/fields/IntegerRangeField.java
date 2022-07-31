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

package com.thoughtworks.gocd.elasticagent.ecs.fields;

import org.apache.commons.lang3.StringUtils;

import static java.text.MessageFormat.format;

public class IntegerRangeField extends Field {
    private final int start;
    private final int end;

    public IntegerRangeField(String key, String displayName, String defaultValue, Boolean required, String displayOrder) {
        this(key, displayName, defaultValue, required, 0, Integer.MAX_VALUE, displayOrder);
    }

    public IntegerRangeField(String key, String displayName, String defaultValue, Boolean required, int start, String displayOrder) {
        this(key, displayName, defaultValue, required, start, Integer.MAX_VALUE, displayOrder);
    }

    public IntegerRangeField(String key, String displayName, String defaultValue, Boolean required, int start, int end, String displayOrder) {
        super(key, displayName, defaultValue, required, false, displayOrder);
        this.start = start;
        this.end = end;
    }

    @Override
    public String doValidate(String input) {
        if (!required && StringUtils.isBlank(input)) {
            return null;
        }

        try {
            final int parsedValue = Integer.parseInt(input);
            if (parsedValue < start) {
                return format("{0} must not be less than {1}.", this.displayName, this.start);
            }

            if (parsedValue > end) {
                return format("{0} must not exceed {1}.", this.displayName, this.end);
            }
        } catch (NumberFormatException e) {
            return format("{0} must be an integer.", this.displayName);
        }

        return null;
    }
}
