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

package com.thoughtworks.gocd.elasticagent.ecs.size;

import com.google.common.collect.ImmutableSortedMap;

import java.util.Map;

public class UnitParser {
    private static final Map<String, SizeUnit> SUFFIXES = ImmutableSortedMap.<String, SizeUnit>orderedBy(String.CASE_INSENSITIVE_ORDER)
            .put("B", SizeUnit.BYTES)
            .put("byte", SizeUnit.BYTES)
            .put("bytes", SizeUnit.BYTES)
            .put("K", SizeUnit.KILOBYTES)
            .put("KB", SizeUnit.KILOBYTES)
            .put("KiB", SizeUnit.KILOBYTES)
            .put("kilobyte", SizeUnit.KILOBYTES)
            .put("kilobytes", SizeUnit.KILOBYTES)
            .put("M", SizeUnit.MEGABYTES)
            .put("MB", SizeUnit.MEGABYTES)
            .put("MiB", SizeUnit.MEGABYTES)
            .put("megabyte", SizeUnit.MEGABYTES)
            .put("megabytes", SizeUnit.MEGABYTES)
            .put("G", SizeUnit.GIGABYTES)
            .put("GB", SizeUnit.GIGABYTES)
            .put("GiB", SizeUnit.GIGABYTES)
            .put("gigabyte", SizeUnit.GIGABYTES)
            .put("gigabytes", SizeUnit.GIGABYTES)
            .put("T", SizeUnit.TERABYTES)
            .put("TB", SizeUnit.TERABYTES)
            .put("TiB", SizeUnit.TERABYTES)
            .put("terabyte", SizeUnit.TERABYTES)
            .put("terabytes", SizeUnit.TERABYTES)
            .build();

    public static SizeUnit toUnit(String input) {
        return SUFFIXES.get(input);
    }
}
