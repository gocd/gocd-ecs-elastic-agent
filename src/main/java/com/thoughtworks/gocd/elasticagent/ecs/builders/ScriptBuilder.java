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

package com.thoughtworks.gocd.elasticagent.ecs.builders;

import static java.lang.String.format;

public class ScriptBuilder {
    private StringBuilder builder = new StringBuilder();

    public ScriptBuilder append(String text) {
        if (text == null) {
            return this;
        }

        builder.append(text);
        return this;
    }

    public ScriptBuilder newLine() {
        builder.append("\n");
        return this;
    }

    public ScriptBuilder addLine(String line) {
        if (line == null) {
            return this;
        }

        return newLine().append(line);
    }

    public ScriptBuilder addIfNotPresent(String line) {
        if (line == null) {
            return this;
        }

        if (builder.indexOf(line) == -1) {
            addLine(line);
        }

        return this;
    }

    public ScriptBuilder addLine(String format, Object... args) {
        return addLine(format(format, args));
    }

    @Override
    public String toString() {
        return builder.toString();
    }
}
