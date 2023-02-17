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

import com.google.common.collect.Collections2;
import com.google.common.io.ByteStreams;
import com.google.common.io.CharStreams;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.joda.time.format.PeriodFormatter;
import org.joda.time.format.PeriodFormatterBuilder;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Function;

public class Util {
    public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
    public static final PeriodFormatter PERIOD_FORMATTER = new PeriodFormatterBuilder()
            .appendYears().appendSuffix(" year ", " years ")
            .appendMonths().appendSuffix(" month ", " months ")
            .appendWeeks().appendSuffix(" week ", " weeks ")
            .appendDays().appendSuffix(" day ", " days ")
            .appendHours().appendSuffix(" hour ", " hours ")
            .printZeroAlways()
            .appendMinutes().appendSuffix(" min", " mins")
            .toFormatter();

    public static String readResource(String resourceFile) {
        try (InputStreamReader reader = new InputStreamReader(Util.class.getResourceAsStream(resourceFile), StandardCharsets.UTF_8)) {
            return CharStreams.toString(reader);
        } catch (IOException e) {
            throw new RuntimeException("Could not find resource " + resourceFile, e);
        }
    }

    public static byte[] readResourceBytes(String resourceFile) {
        try (InputStream in = Util.class.getResourceAsStream(resourceFile)) {
            return ByteStreams.toByteArray(in);
        } catch (IOException e) {
            throw new RuntimeException("Could not find resource " + resourceFile, e);
        }
    }

    public static Collection<String> splitIntoLinesAndTrimSpaces(String lines) {
        if (StringUtils.isBlank(lines)) {
            return Collections.emptyList();
        }

        return Collections2.transform(Arrays.asList(lines.split("[\r\n]+")), StringUtils::trim);
    }

    public static List<String> listFromCommaSeparatedString(String str) {
        if (StringUtils.isBlank(str)) {
            return Collections.emptyList();
        }
        return Arrays.asList(str.split("\\s*,\\s*"));
    }

    public static String getOrDefault(String value, String defaultValue) {
        return StringUtils.isBlank(value) ? defaultValue : value;
    }

    public static Integer getIntOrDefault(String value, Integer defaultValue) {
        if (StringUtils.isBlank(value)) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    public static Integer getOrDefault(Integer value, Integer defaultValue) {
        return value == null ? defaultValue : value;
    }

    public static <T> Collection<T> getOrDefault(Collection<T> value, Collection<T> defaultValue) {
        return value == null || value.isEmpty() ? defaultValue : value;
    }

    public static <L, K, V> Map<K, V> toMap(List<L> list, Function<L, K> keyFunction, Function<L, V> valueFunction) {
        Map<K, V> map = new LinkedHashMap<>();
        for (L item : list) {
            map.put(keyFunction.apply(item), valueFunction.apply(item));
        }
        return map;
    }
}
