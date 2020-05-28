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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ValidationResultTest {

    @Test
    void shouldCheckIfValidationResultIsHavingKey() {
        final ValidationError validationError = mock(ValidationError.class);
        when(validationError.key()).thenReturn("Image");

        final ValidationResult validationResult = new ValidationResult(Collections.singleton(validationError));

        assertThat(validationResult.hasKey("Image")).isTrue();
        assertThat(validationResult.hasKey("Foo")).isFalse();
    }

    @Test
    void shouldReturnTrueIfValidationResultHasErrors() {
        final ValidationError validationError = mock(ValidationError.class);
        when(validationError.key()).thenReturn("Image");

        final ValidationResult validationResult = new ValidationResult(Collections.singleton(validationError));

        assertThat(validationResult.hasErrors()).isTrue();
    }

    @Test
    void shouldReturnFalseIfValidationResultHasErrors() {
        final ValidationResult validationResult = new ValidationResult();

        assertThat(validationResult.hasErrors()).isFalse();
    }

    @Test
    void shouldAddValidationErrorObject() {
        final ValidationResult validationResult = new ValidationResult();

        validationResult.addError(new ValidationError("Image", "some-error"));

        assertThat(validationResult.hasErrors()).isTrue();
        assertThat(validationResult.hasKey("Image")).isTrue();
    }

    @Test
    void shouldNotAddNullValidationErrorObject() {
        final ValidationResult validationResult = new ValidationResult();

        validationResult.addError(null);

        assertThat(validationResult.hasErrors()).isFalse();
    }

    @Test
    void shouldAddValidationErrorFromKeyValue() {
        final ValidationResult validationResult = new ValidationResult();

        validationResult.addError("Image", "some-error");

        assertThat(validationResult.hasErrors()).isTrue();
        assertThat(validationResult.hasKey("Image")).isTrue();
    }

    @Test
    void shouldSerializeToJSON() throws Exception {
        final ValidationResult validationResult = new ValidationResult(Collections.singleton(new ValidationError("Image", "some-error")));

        final String expectedJSON = "[\n" +
                "  {\n" +
                "    \"key\": \"Image\",\n" +
                "    \"message\": \"some-error\"\n" +
                "  }\n" +
                "]";

        JSONAssert.assertEquals(expectedJSON, validationResult.toJSON(), true);
    }
}
