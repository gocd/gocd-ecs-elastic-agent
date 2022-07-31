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

public class PositiveNumberField extends Field {
    public PositiveNumberField(String key, String displayName, String defaultValue, Boolean required, String displayOrder) {
        super(key, displayName, defaultValue, required, false, displayOrder);
    }

    @Override
    public String doValidate(String input) {
        try {
            if (!required && StringUtils.isBlank(input)) {
                return null;
            }

            if (Integer.parseInt(input) <= 0) {
                return this.displayName + " must be a positive integer.";
            }
        } catch (NumberFormatException e) {
            return this.displayName + " must be a positive integer.";
        }

        return null;
    }
}
