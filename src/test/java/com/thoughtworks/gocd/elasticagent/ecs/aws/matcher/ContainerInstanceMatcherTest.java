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

package com.thoughtworks.gocd.elasticagent.ecs.aws.matcher;

import com.amazonaws.services.ecs.model.ContainerDefinition;
import com.amazonaws.services.ecs.model.ContainerInstance;
import com.amazonaws.services.ecs.model.Resource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerInstanceMatcherTest {

    private ContainerInstanceMatcher containerInstanceMatcher;
    private ContainerInstance containerInstance;
    private ContainerDefinition containerDefinition;

    @BeforeEach
    void setUp() {
        containerInstanceMatcher = new ContainerInstanceMatcher();
        containerInstance = mock(ContainerInstance.class);
        containerDefinition = mock(ContainerDefinition.class);
    }

    @Test
    void shouldReturnTrueIfContainerInstanceMatchesContainerDefinition() {
        when(containerInstance.getStatus()).thenReturn("ACTIVE");
        when(containerInstance.isAgentConnected()).thenReturn(true);

        when(containerInstance.getRemainingResources()).thenReturn(Arrays.asList(
                new Resource().withType("INTEGER").withIntegerValue(1024).withName("CPU"),
                new Resource().withType("INTEGER").withIntegerValue(2048).withName("MEMORY")
        ));

        when(containerDefinition.getCpu()).thenReturn(500);
        when(containerDefinition.getMemory()).thenReturn(200);

        final boolean matches = containerInstanceMatcher.matches(containerInstance, containerDefinition);
        assertThat(matches).isTrue();
    }

    @Test
    void shouldReturnFalseIfContainerInstanceIsNotConnected() {
        when(containerInstance.isAgentConnected()).thenReturn(false);

        containerInstanceMatcher.matches(containerInstance, containerDefinition);

        final boolean matches = containerInstanceMatcher.matches(containerInstance, containerDefinition);
        assertThat(matches).isFalse();
    }

    @Test
    void shouldReturnFalseIfContainerInstanceIsINACTIVE() {
        when(containerInstance.getStatus()).thenReturn("INACTIVE");

        containerInstanceMatcher.matches(containerInstance, containerDefinition);

        final boolean matches = containerInstanceMatcher.matches(containerInstance, containerDefinition);
        assertThat(matches).isFalse();
    }
}
