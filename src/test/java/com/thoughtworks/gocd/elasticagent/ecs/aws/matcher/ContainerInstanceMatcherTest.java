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

import com.thoughtworks.gocd.elasticagent.ecs.aws.ContainerDefinitionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ecs.model.ContainerInstance;
import software.amazon.awssdk.services.ecs.model.Resource;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ContainerInstanceMatcherTest {

    private ContainerInstanceMatcher containerInstanceMatcher;
    private ContainerInstance containerInstance;
    private ContainerDefinitionBuilder.PlacementRequirement placementRequirement;

    @BeforeEach
    void setUp() {
        containerInstanceMatcher = new ContainerInstanceMatcher();
        containerInstance = mock(ContainerInstance.class);
        placementRequirement = mock(ContainerDefinitionBuilder.PlacementRequirement.class);
    }

    @Test
    void shouldReturnTrueIfContainerInstanceMatchesContainerDefinition() {
        when(containerInstance.status()).thenReturn("ACTIVE");
        when(containerInstance.agentConnected()).thenReturn(true);

        when(containerInstance.remainingResources()).thenReturn(Arrays.asList(
                Resource.builder().type("INTEGER").integerValue(1024).name("CPU").build(),
                Resource.builder().type("INTEGER").integerValue(2048).name("MEMORY").build()
        ));

        when(placementRequirement.cpu()).thenReturn(500);
        when(placementRequirement.memory()).thenReturn(200);

        final boolean matches = containerInstanceMatcher.matches(containerInstance, placementRequirement);
        assertThat(matches).isTrue();
    }

    @Test
    void shouldReturnFalseIfContainerInstanceIsNotConnected() {
        when(containerInstance.agentConnected()).thenReturn(false);

        containerInstanceMatcher.matches(containerInstance, placementRequirement);

        final boolean matches = containerInstanceMatcher.matches(containerInstance, placementRequirement);
        assertThat(matches).isFalse();
    }

    @Test
    void shouldReturnFalseIfContainerInstanceIsINACTIVE() {
        when(containerInstance.status()).thenReturn("INACTIVE");

        containerInstanceMatcher.matches(containerInstance, placementRequirement);

        final boolean matches = containerInstanceMatcher.matches(containerInstance, placementRequirement);
        assertThat(matches).isFalse();
    }
}
