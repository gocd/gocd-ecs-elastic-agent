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

import com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthType;
import com.thoughtworks.gocd.elasticagent.ecs.requests.ValidateClusterProfileRequest;

import static com.thoughtworks.gocd.elasticagent.ecs.executors.GetPluginConfigurationExecutor.*;

public class DockerRegistrySettingsValidator extends AbstractValidator {

    void validatePluginSettings(ValidateClusterProfileRequest settings) {
        final DockerRegistryAuthType dockerRegistryAuthType = DockerRegistryAuthType.from(settings.get(PRIVATE_DOCKER_REGISTRY_AUTH_TYPE));

        if (dockerRegistryAuthType == DockerRegistryAuthType.DEFAULT) {
            return;
        }

        assertNotBlank(PRIVATE_DOCKER_REGISTRY_EMAIL, settings.get(PRIVATE_DOCKER_REGISTRY_EMAIL));
        assertNotBlank(PRIVATE_DOCKER_REGISTRY_URL, settings.get(PRIVATE_DOCKER_REGISTRY_URL));

        if (dockerRegistryAuthType == DockerRegistryAuthType.AUTH_TOKEN) {
            assertNotBlank(PRIVATE_DOCKER_REGISTRY_AUTH_TOKEN, settings.get(PRIVATE_DOCKER_REGISTRY_AUTH_TOKEN));
        } else {
            assertNotBlank(PRIVATE_DOCKER_REGISTRY_USERNAME, settings.get(PRIVATE_DOCKER_REGISTRY_USERNAME));
            assertNotBlank(PRIVATE_DOCKER_REGISTRY_PASSWORD, settings.get(PRIVATE_DOCKER_REGISTRY_PASSWORD));
        }
    }
}
