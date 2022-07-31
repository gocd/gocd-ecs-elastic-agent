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

package com.thoughtworks.gocd.elasticagent.ecs.validators;

import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;
import org.apache.commons.lang3.StringUtils;

import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.*;

public class AmiAndInstanceTypeValidator extends AbstractValidator {

    @Override
    void validatePluginSettings(ValidateClusterProfileRequest request) {
        final int minLinuxInstance = parseToInt(request.get(MIN_LINUX_INSTANCE_COUNT));
        if (minLinuxInstance > 0) {
            assertNotBlank(LINUX_AMI, request.get(LINUX_AMI));
            assertNotBlank(LINUX_INSTANCE_TYPE, request.get(LINUX_INSTANCE_TYPE));
        }

        final int minWindowsInstance = parseToInt(request.get(MIN_WINDOWS_INSTANCE_COUNT));
        if (minWindowsInstance > 0) {
            assertNotBlank(WINDOWS_AMI, request.get(WINDOWS_AMI));
            assertNotBlank(WINDOWS_INSTANCE_TYPE, request.get(WINDOWS_INSTANCE_TYPE));
        }
    }

    private int parseToInt(String string) {
        if (StringUtils.isBlank(string)) {
            return 0;
        }
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            //Ignore: This will validated by other validator.
            return 0;
        }
    }
}
