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

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.PERIOD_FORMATTER;
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
        String csvStringWithoutSpace = "sg-45b00b38,\n" +
                "sg-30d2694d,\n" +
                "sg-30d2694a,\n" +
                "sg-30d2694s,\n" +
                "sg-q0d2694d";
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
    void periodFormatter_shouldProduceStringWithZeroMinutes() {
        final DateTime now = DateTime.parse("2017-12-21T07:50:40.514+05:30");
        final DateTime start = now.minusSeconds(30);

        assertThat(PERIOD_FORMATTER.print(new Period(start, now))).isEqualTo("0 mins");
    }

    @Test
    void periodFormatter_shouldProduceStringWithMinutes() {
        final DateTime now = DateTime.parse("2017-12-21T07:50:40.514+05:30");
        final DateTime start = now.minusMinutes(30);

        assertThat(PERIOD_FORMATTER.print(new Period(start, now))).isEqualTo("30 mins");
    }

    @Test
    void periodFormatter_shouldProduceStringWithHours() {
        final DateTime now = DateTime.parse("2017-12-21T07:50:40.514+05:30");
        final DateTime start = now.minusMinutes(30).minusHours(5);

        assertThat(PERIOD_FORMATTER.print(new Period(start, now))).isEqualTo("5 hours 30 mins");
    }

    @Test
    void periodFormatter_shouldProduceStringWithDays() {
        final DateTime now = DateTime.parse("2017-12-21T07:50:40.514+05:30");
        final DateTime start = now.minusMinutes(30).minusHours(5).minusDays(3);

        assertThat(PERIOD_FORMATTER.print(new Period(start, now))).isEqualTo("3 days 5 hours 30 mins");
    }

    @Test
    void periodFormatter_shouldProduceStringWithWeeks() {
        final DateTime now = DateTime.parse("2017-12-21T07:50:40.514+05:30");
        final DateTime start = now.minusMinutes(30).minusHours(5).minusDays(9);

        assertThat(PERIOD_FORMATTER.print(new Period(start, now))).isEqualTo("1 week 2 days 5 hours 30 mins");
    }

    @Test
    void periodFormatter_shouldProduceStringWithMonths() {
        final DateTime now = DateTime.parse("2017-12-21T07:50:40.514+05:30");
        final DateTime start = now.minusMinutes(30).minusHours(5).minusDays(45);

        assertThat(PERIOD_FORMATTER.print(new Period(start, now))).isEqualTo("1 month 2 weeks 1 day 5 hours 30 mins");
    }

    @Test
    void periodFormatter_shouldProduceStringWithYears() {
        final DateTime now = DateTime.parse("2017-12-21T07:50:40.514+05:30");
        final DateTime start = now.minusMinutes(30).minusHours(5).minusDays(390);

        assertThat(PERIOD_FORMATTER.print(new Period(start, now))).isEqualTo("1 year 3 weeks 4 days 5 hours 30 mins");
    }

    @Test
    void getIntOrDefault_shouldParseStringIfNotBlank() {
        assertThat(Util.getIntOrDefault("20", 0)).isEqualTo(20);
    }

    @Test
    void getIntOrDefault_shouldReturnDefaultValueIfGivenValueIsNull() {
        assertThat(Util.getIntOrDefault(null, 0)).isEqualTo(0);
    }
}
