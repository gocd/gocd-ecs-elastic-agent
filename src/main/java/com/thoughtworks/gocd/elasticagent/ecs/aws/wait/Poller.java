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


import java.time.Duration;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.function.Supplier;

public class Poller<T> {
    private static final AtomicLong POLLER_THREAD_COUNTER = new AtomicLong();

    private Predicate<T> stopWhen;
    private Supplier<T> poller;
    private Duration timeout;
    private Duration retryInterval = Duration.ofSeconds(5);

    public Result<T> await() {
        validateConfiguration();
        final Result<T> result = new Result<>();

        // A virtual thread rather than an executor: virtual threads are always daemon, and blocking
        // java.net socket I/O on a virtual thread is interruptible (JEP 444), so on timeout the
        // interrupt genuinely cancels an in-flight AWS call instead of abandoning the thread.
        final Thread worker = Thread.ofVirtual()
                .name("ecs-plugin-poller-" + POLLER_THREAD_COUNTER.incrementAndGet())
                .start(() -> {
                    try {
                        do {
                            result.set(poller.get());

                            if (stopWhen.test(result.get())) {
                                break;
                            }

                            Thread.sleep(retryInterval);
                        } while (stopWhen.negate().test(result.get()));
                    } catch (InterruptedException e) {
                        // Only await() interrupts this thread, and it records the failure itself;
                        // exit without overwriting the caller's timeout with our interrupt.
                        Thread.currentThread().interrupt();
                    } catch (RuntimeException e) {
                        result.failed(e);
                    }
                });

        try {
            if (!worker.join(timeout)) {
                result.failed(new TimeoutException("Polling did not complete within " + timeout));
                worker.interrupt();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            result.failed(e);
            worker.interrupt();
        }

        return result;
    }

    private void validateConfiguration() {
        bombIf(stopWhen == null, "Must provide stopWhen predicate.");
        bombIf(poller == null, "Must provide poller supplier.");
        bombIf(timeout == null, "Must provide timeout period.");
    }

    private void bombIf(boolean condition, String errorMessage) {
        if (condition) throw new InvalidPollerConfiguration(errorMessage);
    }

    public Poller<T> timeout(Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public Poller<T> stopWhen(Predicate<T> stopWhen) {
        this.stopWhen = stopWhen;
        return this;
    }

    public Poller<T> poll(Supplier<T> poller) {
        this.poller = poller;
        return this;
    }

    public Poller<T> retryAfter(Duration retryInterval) {
        this.retryInterval = retryInterval;
        return this;
    }

}
