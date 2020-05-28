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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ContainerDataVolumeFieldTest {
    private Field field;

    @BeforeEach
    void setUp() {
        field = new ContainerDataVolumeField("MaxContainerDataVolumeSize", "1");
    }

    @Test
    void shouldAddErrorWhenValueIsNotANumber() {
        final Map<String, String> result = field.validate("foo");

        assertThat(result)
                .hasSize(2)
                .containsEntry("key", "MaxContainerDataVolumeSize")
                .containsEntry("message", "Container data volume size must be a positive integer.");
    }

    @Test
    void shouldNotAddErrorWhenInputIsNullValue() {
        final Map<String, String> result = field.validate(null);

        assertThat(result).isEmpty();
    }

    @Test
    void shouldNotAddErrorWhenInputIsEmptyString() {
        final Map<String, String> result = field.validate("             ");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldAddErrorWhenSpecifiedSizeIsBelow10G() {
        final Map<String, String> result = field.validate("9");

        assertThat(result)
                .hasSize(2)
                .containsEntry("key", "MaxContainerDataVolumeSize")
                .containsEntry("message", "Minimum required container volume 10G.");
    }

    @Test
    void shouldNotAddErrorIfValueIsIntegerAndGreaterThan9() {
        final Map<String, String> result = field.validate("10");

        assertThat(result).isEmpty();
    }
}
