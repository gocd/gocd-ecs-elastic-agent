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

import software.amazon.awssdk.services.ecs.model.LogDriver;

import java.util.Arrays;

import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class LogDriverNameField extends Field {

    public LogDriverNameField(String displayOrder) {
        super("LogDriver", "Log driver name", null, false, false, displayOrder);
    }

    @Override
    public String doValidate(String logDriverName) {
        if (isBlank(logDriverName)) {
            return null;
        }

        if (LogDriver.fromValue(logDriverName) != LogDriver.UNKNOWN_TO_SDK_VERSION) {
            return null;
        }

        final var supportedDrivers = Arrays.stream(LogDriver.values()).filter(driver -> driver != LogDriver.UNKNOWN_TO_SDK_VERSION).toList();
        return format("Log driver {0} is not supported. Supported log drivers are {1} ", logDriverName, supportedDrivers);
    }
}
