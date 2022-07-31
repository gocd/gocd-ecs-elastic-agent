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

package com.thoughtworks.gocd.elasticagent.ecs.reports;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StatusReportGenerationErrorTest {

    @Test
    void shouldConvertStatusReportGenerationExceptionToStatusReportGenerationError() {
        final StatusReportGenerationException exception = StatusReportGenerationException.noRunningTask("Foo");

        final StatusReportGenerationError error = new StatusReportGenerationError(exception);

        assertThat(error.getMessage()).isEqualTo("ECS task is not running.");
        assertThat(error.getDescription()).isEqualTo("Can not find a running task for the provided elastic agent id 'Foo'.");
    }

    @Test
    void shouldConvertExceptionWithMessageToStatusReportGenerationError() {
        final RuntimeException exception = new RuntimeException("This is message.");

        final StatusReportGenerationError error = new StatusReportGenerationError(exception);

        assertThat(error.getMessage()).isEqualTo("This is message.");
        assertThat(error.getDescription()).isEqualTo("Please check the plugin logs for more information.");
    }

    @Test
    void shouldConvertExceptionMessageToStatusReportGenerationError() {
        final RuntimeException exception = new RuntimeException();

        final StatusReportGenerationError error = new StatusReportGenerationError(exception);

        assertThat(error.getMessage()).isEqualTo("We're sorry, but something went wrong.");
        assertThat(error.getDescription()).isEqualTo("Please check the plugin logs for more information.");
    }
}
