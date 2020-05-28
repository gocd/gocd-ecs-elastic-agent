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

package com.thoughtworks.gocd.elasticagent.ecs.aws.wait;

import org.joda.time.Period;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.function.Predicate;
import java.util.function.Supplier;

import static java.util.concurrent.TimeUnit.SECONDS;

public class Poller<T> {
    private Predicate<T> stopWhen;
    private Supplier<T> poller;
    private Period timeout;
    private int retryIntervalInMillis = 5000;

    public Result<T> start() {
        validateConfiguration();
        final Result<T> result = new Result<>();
        final ExecutorService service = Executors.newFixedThreadPool(1);

        Future<?> futureResult = service.submit(() -> {
            do {
                result.set(poller.get());

                if (stopWhen.test(result.get())) {
                    break;
                }

                try {
                    Thread.sleep(retryIntervalInMillis);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }

            } while (stopWhen.negate().test(result.get()));
        });

        try {
            futureResult.get(timeout.toStandardSeconds().getSeconds(), SECONDS);
        } catch (Exception e) {
            result.failed(e);
            futureResult.cancel(true);
        } finally {
            service.shutdown();
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

    public Poller<T> timeout(Period timeout) {
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

    public Poller<T> retryAfter(int retryIntervalInMillis) {
        this.retryIntervalInMillis = retryIntervalInMillis;
        return this;
    }

}

