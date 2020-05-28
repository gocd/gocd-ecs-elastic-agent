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

public class Result<V> {
    private V object = null;
    private boolean isFailed = false;
    private Throwable exception;

    public boolean isFailed() {
        return isFailed;
    }

    public V get() {
        return object;
    }

    public Throwable getException() {
        return exception;
    }

    public void set(V object) {
        this.object = object;
    }

    public void failed(Throwable throwable) {
        this.isFailed = true;
        this.exception = throwable;
    }
}
