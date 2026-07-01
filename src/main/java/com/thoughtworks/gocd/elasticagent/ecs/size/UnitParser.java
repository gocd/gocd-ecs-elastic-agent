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

import lombok.experimental.UtilityClass;

import java.util.Collections;
import java.util.NavigableMap;
import java.util.TreeMap;

@UtilityClass
public class UnitParser {
    private static final NavigableMap<String, SizeUnit> SUFFIXES = suffixes();

    private static NavigableMap<String, SizeUnit> suffixes() {
        var s = new TreeMap<String, SizeUnit>(String.CASE_INSENSITIVE_ORDER);
        s.put("B", SizeUnit.BYTES);
        s.put("byte", SizeUnit.BYTES);
        s.put("bytes", SizeUnit.BYTES);
        s.put("K", SizeUnit.KILOBYTES);
        s.put("KB", SizeUnit.KILOBYTES);
        s.put("KiB", SizeUnit.KILOBYTES);
        s.put("kilobyte", SizeUnit.KILOBYTES);
        s.put("kilobytes", SizeUnit.KILOBYTES);
        s.put("M", SizeUnit.MEGABYTES);
        s.put("MB", SizeUnit.MEGABYTES);
        s.put("MiB", SizeUnit.MEGABYTES);
        s.put("megabyte", SizeUnit.MEGABYTES);
        s.put("megabytes", SizeUnit.MEGABYTES);
        s.put("G", SizeUnit.GIGABYTES);
        s.put("GB", SizeUnit.GIGABYTES);
        s.put("GiB", SizeUnit.GIGABYTES);
        s.put("gigabyte", SizeUnit.GIGABYTES);
        s.put("gigabytes", SizeUnit.GIGABYTES);
        s.put("T", SizeUnit.TERABYTES);
        s.put("TB", SizeUnit.TERABYTES);
        s.put("TiB", SizeUnit.TERABYTES);
        s.put("terabyte", SizeUnit.TERABYTES);
        s.put("terabytes", SizeUnit.TERABYTES);
        return Collections.unmodifiableNavigableMap(s);
    }

    public static SizeUnit toUnit(String input) {
        return SUFFIXES.get(input);
    }
}
