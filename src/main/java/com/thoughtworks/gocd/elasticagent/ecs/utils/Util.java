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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DurationFormatUtils;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;

public class Util {
    public static final Gson GSON = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();

    public static void checkArgument(boolean expression, Object errorMessage) {
        if (!expression) {
            throw new IllegalArgumentException(String.valueOf(errorMessage));
        }
    }

    public static String readResource(String resourceFile) {
        return new String(readResourceBytes(resourceFile), StandardCharsets.UTF_8);
    }

    public static byte[] readResourceBytes(String resourceFile) {
        try (InputStream in = requireNonNull(Util.class.getResourceAsStream(resourceFile))) {
            return in.readAllBytes();
        } catch (Exception e) {
            throw new RuntimeException("Could not find resource " + resourceFile, e);
        }
    }

    public static Collection<String> splitIntoLinesAndTrimSpaces(String lines) {
        return lines == null || lines.isBlank()
                ? Collections.emptyList()
                : lines.lines().map(String::trim).filter(s -> !s.isBlank()).toList();
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

    public static String formatDurationWordsFromNow(Instant date) {
        return formatDurationWords(Instant.now().toEpochMilli() - date.toEpochMilli());
    }

    public static String formatDurationWords(long millis) {
        return DurationFormatUtils.formatDurationWords(Math.max(millis, 0), true, true);
    }
}
