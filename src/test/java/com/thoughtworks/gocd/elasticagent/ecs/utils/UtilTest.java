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

package com.thoughtworks.gocd.elasticagent.ecs.utils;

import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.getOrDefault;
import static org.assertj.core.api.Assertions.assertThat;

class UtilTest {

    @Test
    void listFromCommaSeparatedString_shouldParseCommaSeparatedStringWithoutSpace() {
        String csvStringWithoutSpace = "sg-45b00b38,sg-30d2694d,sg-30d2694a,sg-30d2694s,sg-q0d2694d";
        List<String> resultOfWithoutSpace = Util.listFromCommaSeparatedString(csvStringWithoutSpace);

        assertThat(resultOfWithoutSpace).hasSize(5);
        assertThat(resultOfWithoutSpace).contains("sg-45b00b38", "sg-30d2694d", "sg-30d2694a", "sg-30d2694s", "sg-q0d2694d");
    }

    @Test
    void listFromCommaSeparatedString_shouldParseCommaSeparatedStringWithSpace() {
        String csvStringWithoutSpace = "sg-45b00b38   ,    sg-30d2694d        ,          sg-30d2694a ,         sg-30d2694s       ,        sg-q0d2694d";
        List<String> resultOfWithoutSpace = Util.listFromCommaSeparatedString(csvStringWithoutSpace);

        assertThat(resultOfWithoutSpace).hasSize(5);
        assertThat(resultOfWithoutSpace).contains("sg-45b00b38", "sg-30d2694d", "sg-30d2694a", "sg-30d2694s", "sg-q0d2694d");
    }

    @Test
    void listFromCommaSeparatedString_shouldParseCommaSeparatedStringWithNewLine() {
        String csvStringWithoutSpace = """
                sg-45b00b38,
                sg-30d2694d,
                sg-30d2694a,
                sg-30d2694s,
                sg-q0d2694d""";
        List<String> resultOfWithoutSpace = Util.listFromCommaSeparatedString(csvStringWithoutSpace);

        assertThat(resultOfWithoutSpace).hasSize(5);
        assertThat(resultOfWithoutSpace).contains("sg-45b00b38", "sg-30d2694d", "sg-30d2694a", "sg-30d2694s", "sg-q0d2694d");
    }

    @Test
    void test_getOrDefault_ForStringValue() {
        final String value = null, defaultValue = null;

        assertThat(getOrDefault(value, defaultValue)).isNull();
        assertThat(getOrDefault(null, "XYZ")).isEqualTo("XYZ");
        assertThat(getOrDefault("", "XYZ")).isEqualTo("XYZ");
        assertThat(getOrDefault("ABC", null)).isEqualTo("ABC");
        assertThat(getOrDefault("ABC", "")).isEqualTo("ABC");
        assertThat(getOrDefault("ABC", "XYZ")).isEqualTo("ABC");

    }

    @Test
    void test_getOrDefault_ForCollectionsValue() {
        final Collection<String> bothValuesProvided = getOrDefault(Collections.singletonList("ABC"), Collections.singletonList("XYZ"));

        assertThat(bothValuesProvided).contains("ABC");
        assertThat(bothValuesProvided.contains("XYZ")).isFalse();

        final Collection<String> defaultValueNotProvided = getOrDefault(Collections.singletonList("ABC"), null);

        assertThat(defaultValueNotProvided).contains("ABC");

        final Collection<String> valueNotProvidedAndDefaultValueProvided = getOrDefault(null, Collections.singletonList("XYZ"));

        assertThat(valueNotProvidedAndDefaultValueProvided).contains("XYZ");
        assertThat(valueNotProvidedAndDefaultValueProvided.contains("ABC")).isFalse();
    }

    @Test
    void getIntOrDefault_shouldParseStringIfNotBlank() {
        assertThat(Util.getIntOrDefault("20", 0)).isEqualTo(20);
    }

    @Test
    void getIntOrDefault_shouldReturnDefaultValueIfGivenValueIsNull() {
        assertThat(Util.getIntOrDefault(null, 0)).isEqualTo(0);
    }

    @Test
    void formatDurationWords_shouldFormatPositiveDurations() {
        assertThat(Util.formatDurationWords(0)).isEqualTo("0 seconds");
        assertThat(Util.formatDurationWords(5_000)).isEqualTo("5 seconds");
        assertThat(Util.formatDurationWords(90_000)).isEqualTo("1 minute 30 seconds");
    }

    @Test
    void formatDurationWords_shouldClampNegativeDurationsToZeroRatherThanThrow() {
        // AWS-side timestamps (task createdAt, instance launchTime) can be slightly ahead of the local
        // clock; DurationFormatUtils.formatDurationWords throws for negative input, so Util must clamp.
        assertThat(Util.formatDurationWords(-1)).isEqualTo("0 seconds");
        assertThat(Util.formatDurationWords(Long.MIN_VALUE)).isEqualTo("0 seconds");
    }

    @Test
    void formatDurationWordsFromNow_shouldClampDatesInTheFutureToZeroRatherThanThrow() {
        assertThat(Util.formatDurationWordsFromNow(Instant.now().plusSeconds(60))).isEqualTo("0 seconds");
    }

    @Test
    void formatDurationWordsFromNow_shouldFormatDatesInThePast() {
        assertThat(Util.formatDurationWordsFromNow(Instant.now().minusSeconds(3_600))).startsWith("1 hour");
    }
}
