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

import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.*;
import static org.apache.commons.lang3.StringUtils.isBlank;

public class VolumeSettingsValidator extends AbstractValidator {

    public static final String VOLUME_TYPE_IO_1 = "io1";

    void validatePluginSettings(ValidateClusterProfileRequest request) {
        validateVolume(request, LINUX_DOCKER_VOLUME_TYPE, LINUX_DOCKER_VOLUME_SIZE, LINUX_DOCKER_VOLUME_PROVISIONED_IOPS);
        validateVolume(request, LINUX_OS_VOLUME_TYPE, LINUX_OS_VOLUME_SIZE, LINUX_OS_VOLUME_PROVISIONED_IOPS);
        validateVolume(request, WINDOWS_OS_VOLUME_TYPE, WINDOWS_OS_VOLUME_SIZE, WINDOWS_OS_VOLUME_PROVISIONED_IOPS);
    }

    private void validateVolume(ValidateClusterProfileRequest request, String volumeTypeKey, String volumeSizeKey, String provisionedIOPSKey) {
        final String volumeType = request.get(volumeTypeKey);
        if (isBlank(volumeType) || "none".equals(volumeType)) {
            return;
        }
        if (isProvisionedIOPS(volumeType)) {
            assertNotBlank(provisionedIOPSKey, request.get(provisionedIOPSKey));
        }

        assertNotBlank(volumeSizeKey, request.get(volumeSizeKey));
    }

    private boolean isProvisionedIOPS(String volumeType) {
        return VOLUME_TYPE_IO_1.equals(volumeType);
    }
}
