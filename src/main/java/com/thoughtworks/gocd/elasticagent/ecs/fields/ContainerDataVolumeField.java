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

package com.thoughtworks.gocd.elasticagent.ecs.fields;

import org.apache.commons.lang3.StringUtils;

public class ContainerDataVolumeField extends PositiveNumberField {
    public ContainerDataVolumeField(String name, String displayOrder) {
        super(name, "Container data volume size", "10", false, displayOrder);
    }

    @Override
    public String doValidate(String input) {
        if (StringUtils.isBlank(input)) {
            return null;
        }

        final String errorMessage = super.doValidate(input);
        if (StringUtils.isNotBlank(errorMessage)) {
            return errorMessage;
        }

        if (Integer.parseInt(input) < 10) {
            return "Minimum required container volume 10G.";
        }

        return null;
    }
}
