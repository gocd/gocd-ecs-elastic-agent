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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class IntegerRangeFieldTest {

    @Test
    void shouldValidateValidFiledValuesInRange() {
        IntegerRangeField field = new IntegerRangeField("foo", "bar", "0", true, 0, 10, "1");

        assertThat(field.doValidate("0")).isNull();
        assertThat(field.doValidate("5")).isNull();
        assertThat(field.doValidate("10")).isNull();
    }

    @Test
    void shouldValidateReturnErrorMessageForInvalidValues() {
        IntegerRangeField field = new IntegerRangeField("foo", "bar", "0", true, 0, 10, "1");

        assertThat(field.doValidate("-1")).isEqualTo("bar must not be less than 0.");
        assertThat(field.doValidate("non-integer")).isEqualTo("bar must be an integer.");
        assertThat(field.doValidate("11")).isEqualTo("bar must not exceed 10.");
    }
}
