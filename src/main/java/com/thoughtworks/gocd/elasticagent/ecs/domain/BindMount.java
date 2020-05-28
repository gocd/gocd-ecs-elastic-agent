/*
 * Copyright 2020 ThoughtWorks, Inc.
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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.thoughtworks.gocd.elasticagent.ecs.domain.annotation.Metadata;
import org.apache.commons.collections4.map.HashedMap;

import java.util.Map;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isAnyEmpty;
import static org.apache.commons.lang3.StringUtils.isEmpty;

public class BindMount {
    @Expose
    @SerializedName("Name")
    @Metadata(key = "Name", required = true, secure = false)
    public String name;

    @Expose
    @SerializedName("SourcePath")
    @Metadata(key = "SourcePath", required = true, secure = false)
    public String sourcePath;

    @Expose
    @SerializedName("ContainerPath")
    @Metadata(key = "ContainerPath", required = true, secure = false)
    public String containerPath;

    private Map<String, String> errors = new HashedMap<>();

    public BindMount() {
    }

    public BindMount(String name, String sourcePath, String containerPath) {
        this.name = name;
        this.sourcePath = sourcePath;
        this.containerPath = containerPath;
    }

    public boolean isValid() {
        validateName();
        validateSourcePath();
        validateContainerPath();

        return errors.isEmpty();
    }

    public String errors() {
        return errors.values().stream().collect(joining(" , "));
    }

    private void validateName() {
        if(isEmpty(this.name)) {
            errors.put("Name", "Name cannot be empty.");
        }
    }

    private void validateSourcePath() {
        if(isEmpty(this.sourcePath)) {
            errors.put("SourcePath", "SourcePath cannot be empty.");
        }
    }

    private void validateContainerPath() {
        if(isEmpty(this.containerPath)) {
            errors.put("ContainerPath", "ContainerPath cannot be empty.");
        }
    }

    public String getName() {
        return name;
    }

    public String getSourcePath() {
        return sourcePath;
    }

    public String getContainerPath() {
        return containerPath;
    }
}
