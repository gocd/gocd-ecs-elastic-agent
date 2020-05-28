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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

public enum TimeUnit {
    NANOSECONDS("ns"), MICROSECONDS("us"), MILLISECONDS("ms"), SECONDS("s"), MINUTES("m"), HOURS("h");

    private final String unit;

    TimeUnit(String unit) {
        this.unit = unit;
    }

    public String stringify(int value) {
        return new StringBuilder().append(value).append(unit).toString();
    }
}
