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

package com.thoughtworks.gocd.elasticagent.ecs.aws.strategy;

import com.thoughtworks.gocd.elasticagent.ecs.aws.StopPolicy;

import static java.text.MessageFormat.format;

public class InstanceSelectionStrategyFactory {

    public InstanceSelectionStrategy strategyFor(StopPolicy stopPolicy) {
        switch (stopPolicy) {
            case StopOldestInstance:
                return new OldestInstanceSelectionStrategy();
            case StopIdleInstance:
                return new StopIdleInstanceSelectionStrategy();
        }

        throw new RuntimeException(format("No strategy available for stop policy {0}.", stopPolicy));
    }
}
