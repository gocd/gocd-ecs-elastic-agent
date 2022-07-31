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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class JobIdentifierTest {
    @Test
    void shouldMatchJobIdentifier() {
        final JobIdentifier jobIdentifier = new JobIdentifier("up42", 98765L, "foo", "stage_1", "30000", "job_1", 876578L);

        final JobIdentifier deserializedJobIdentifier = JobIdentifier.fromJson(jobIdentifier.toJson());

        assertThat(jobIdentifier.equals(deserializedJobIdentifier)).isTrue();
    }

    @Test
    void shouldCreateRepresentationFromJobIdentifier() {
        final JobIdentifier jobIdentifier = new JobIdentifier("up42", 98765L, "foo", "stage_1", "30000", "job_1", 876578L);

        assertThat(jobIdentifier.getRepresentation()).isEqualTo("up42/98765/stage_1/30000/job_1");
    }

    @Test
    void shouldCreatePipelineHistoryPageLink() {
        final JobIdentifier jobIdentifier = new JobIdentifier("up42", 98765L, "foo", "stage_1", "30000", "job_1", 876578L);

        assertThat(jobIdentifier.getPipelineHistoryPageLink()).isEqualTo("/go/tab/pipeline/history/up42");
    }

    @Test
    void shouldCreateVSMPageLink() {
        final JobIdentifier jobIdentifier = new JobIdentifier("up42", 98765L, "foo", "stage_1", "30000", "job_1", 876578L);

        assertThat(jobIdentifier.getVsmPageLink()).isEqualTo("/go/pipelines/value_stream_map/up42/98765");
    }

    @Test
    void shouldCreateStageDetailsPageLink() {
        final JobIdentifier jobIdentifier = new JobIdentifier("up42", 98765L, "foo", "stage_1", "30000", "job_1", 876578L);

        assertThat(jobIdentifier.getStageDetailsPageLink()).isEqualTo("/go/pipelines/up42/98765/stage_1/30000");
    }

    @Test
    void shouldCreateJobDetailsPageLink() {
        final JobIdentifier jobIdentifier = new JobIdentifier("up42", 98765L, "foo", "stage_1", "30000", "job_1", 876578L);

        assertThat(jobIdentifier.getJobDetailsPageLink()).isEqualTo("/go/tab/build/detail/up42/98765/stage_1/30000/job_1");
    }
}
