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

package com.thoughtworks.gocd.elasticagent.ecs.aws.wait;

import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.Timeout;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PollerTest {
    @Test
    void shouldExecuteGivenSupplier() {
        final Api api = mock(Api.class);
        when(api.get()).thenReturn(1).thenReturn(2).thenReturn(3);

        final Result<Integer> result = new Poller<Integer>()
                .poll(api::get)
                .stopWhen(count -> count >= 3)
                .timeout(Duration.ofSeconds(3))
                .retryAfter(Duration.ofMillis(1100))
                .await();

        assertThat(result.get()).isEqualTo(3);
        assertThat(result.isFailed()).isFalse();
        assertThat(result.getException()).isNull();
    }

    @Test
    void shouldReturnResultWhenGivenSupplierThrowsAnException() {
        final Api api = mock(Api.class);
        when(api.get()).thenReturn(1).thenReturn(2).thenThrow(new RuntimeException("Boom!!"));

        final Result<Integer> result = new Poller<Integer>()
                .poll(api::get)
                .stopWhen(count -> count >= 3)
                .timeout(Duration.ofSeconds(3))
                .retryAfter(Duration.ofMillis(1100))
                .await();

        assertThat(result.get()).isEqualTo(2);
        assertThat(result.isFailed()).isTrue();
        assertThat(result.getException()).isInstanceOf(RuntimeException.class).hasMessage("Boom!!");
    }

    @Test
    void shouldFailWithTimeoutWhenStopConditionIsNeverMet() {
        final Api api = mock(Api.class);
        when(api.get()).thenReturn(1);

        final Result<Integer> result = new Poller<Integer>()
                .poll(api::get)
                .stopWhen(count -> count >= 3)
                .timeout(Duration.ofMillis(300))
                .retryAfter(Duration.ofMillis(100))
                .await();

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getException()).isInstanceOf(TimeoutException.class);
        assertThat(result.get()).isEqualTo(1);
    }

    @Test
    @Timeout(value = 5, unit = TimeUnit.SECONDS)
    void shouldReturnPromptlyAtTimeoutEvenWhenSupplierIsBlocked() {
        final Result<Integer> result = new Poller<Integer>()
                .poll(() -> {
                    try {
                        Thread.sleep(Duration.ofSeconds(30));
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException(e);
                    }
                    return 1;
                })
                .stopWhen(count -> count >= 1)
                .timeout(Duration.ofMillis(300))
                .await();

        assertThat(result.isFailed()).isTrue();
        assertThat(result.getException()).isInstanceOf(TimeoutException.class);
        assertThat(result.get()).isNull();
    }

    static class Api {
        public int get() {
            return 0;
        }
    }
}
