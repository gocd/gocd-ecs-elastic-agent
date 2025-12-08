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

import org.apache.commons.lang3.Strings;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.google.common.base.Preconditions.checkArgument;
import static java.text.MessageFormat.format;
import static org.apache.commons.lang3.StringUtils.contains;

public class SizeValidator {
    private static final Pattern SIZE_PATTERN = Pattern.compile("(\\d+\\.?\\d?)\\s*(\\S+)");
    private static final String INVALID_UNIT_ERROR_MESSAGE = "Invalid unit: `{0}`. It require a suffix to indicate the unit of memory (B, K, M, G or T)";
    private static final String MUST_BE_INT_ERROR_MESSAGE = "Invalid size: `{0}`. Must be a positive integer followed by unit (B, K, M, G or T)";
    private static final String INVALID_SIZE_ERROR_MESSAGE = "Invalid size: `{0}`";

    public void validate(String size) {
        checkArgument(!NumberUtils.isCreatable(size), format(MUST_BE_INT_ERROR_MESSAGE, size));
        checkArgument(!Strings.CS.contains(size, "."), format(MUST_BE_INT_ERROR_MESSAGE, size));

        final Matcher matcher = SIZE_PATTERN.matcher(size);
        checkArgument(matcher.matches(), format(INVALID_SIZE_ERROR_MESSAGE, size));

        final String unit = matcher.group(2);
        checkArgument(UnitParser.toUnit(unit) != null, format(INVALID_UNIT_ERROR_MESSAGE, unit));
    }
}
