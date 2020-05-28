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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class InstanceSelectionStrategyFactoryTest {

    @ParameterizedTest
    @MethodSource("inputs")
    void shouldReturnInstanceSelectionStrategy(StopPolicy stopPolicy, Class<?> instanceSelectionStrategyClass) {
        final InstanceSelectionStrategy instanceSelectionStrategy = new InstanceSelectionStrategyFactory().strategyFor(stopPolicy);
        assertThat(instanceSelectionStrategy).isInstanceOf(instanceSelectionStrategyClass);
    }

    static Stream<Arguments> inputs() {
        return Stream.of(
                Arguments.of(StopPolicy.StopOldestInstance, OldestInstanceSelectionStrategy.class),
                Arguments.of(StopPolicy.StopIdleInstance, StopIdleInstanceSelectionStrategy.class)
        );
    }
}
