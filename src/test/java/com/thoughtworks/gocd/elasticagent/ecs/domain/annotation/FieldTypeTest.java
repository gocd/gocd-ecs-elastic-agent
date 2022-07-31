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

package com.thoughtworks.gocd.elasticagent.ecs.domain.annotation;

import org.junit.jupiter.api.Test;

import static com.thoughtworks.gocd.elasticagent.ecs.domain.annotation.FieldType.*;
import static org.assertj.core.api.Assertions.assertThat;

public class FieldTypeTest {

    @Test
    public void shouldValidateNumber() throws Exception {
        assertThat(NUMBER.validate("foo")).isEqualTo("must be number");
        assertThat(NUMBER.validate("3.14.1")).isEqualTo("must be number");
        assertThat(NUMBER.validate("-999")).isNull();
        assertThat(NUMBER.validate("3.14f")).isNull();
        assertThat(NUMBER.validate("3.14")).isNull();
        assertThat(NUMBER.validate("999")).isNull();
    }

    @Test
    public void shouldValidatePositiveDecimal() throws Exception {
        assertThat(POSITIVE_DECIMAL.validate("foo")).isEqualTo("must be positive decimal");
        assertThat(POSITIVE_DECIMAL.validate("-999")).isEqualTo("must be positive decimal");
        assertThat(POSITIVE_DECIMAL.validate("1.4")).isEqualTo("must be positive decimal");
        assertThat(POSITIVE_DECIMAL.validate("999")).isNull();
    }

    @Test
    public void shouldValidateMemory() throws Exception {
        assertThat(MEMORY.validate("4194305b")).describedAs("4194305b is equal to 4M").isNull();
        assertThat(MEMORY.validate("4096k")).describedAs("4096k is equal to 4M").isNull();
        assertThat(MEMORY.validate("10m")).isNull();
        assertThat(MEMORY.validate("1g")).isNull();
        assertThat(MEMORY.validate("1t")).isNull();

    }

    @Test
    public void shouldReturnErrorMessageIfMemoryConfigurationIsInvalid() {
        assertThat(MEMORY.validate("foo")).isEqualTo("Invalid size: `foo`");
        assertThat(MEMORY.validate("-999m")).isEqualTo("Invalid size: `-999m`");

        assertThat(MEMORY.validate("1024k")).isEqualTo("Invalid size: `1024k`. Minimum size for container to start is 4M");
        assertThat(MEMORY.validate("1b")).isEqualTo("Invalid size: `1b`. Minimum size for container to start is 4M");

        assertThat(MEMORY.validate("1024")).isEqualTo("Invalid size: `1024`. Must be a positive integer followed by unit (B, K, M, G or T)");
        assertThat(MEMORY.validate("1024S")).isEqualTo("Invalid unit: `S`. It require a suffix to indicate the unit of memory (B, K, M, G or T)");
        assertThat(MEMORY.validate("1.5G")).isEqualTo("Invalid size: `1.5G`. Must be a positive integer followed by unit (B, K, M, G or T)");
    }
}
