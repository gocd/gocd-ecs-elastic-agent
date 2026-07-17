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

package com.thoughtworks.gocd.elasticagent.ecs.domain;

import lombok.EqualsAndHashCode;
import software.amazon.awssdk.services.ecs.model.Resource;

import java.util.List;

@EqualsAndHashCode
public class ContainerResources {

    private double cpu;
    private double memory;
    private List<String> ports;
    private List<String> portsUDP;

    public ContainerResources(List<Resource> resources) {

        for (Resource resource : resources) {
            if ("CPU".equalsIgnoreCase(resource.name())) {
                cpu = numeric(resource);
            } else if ("MEMORY".equalsIgnoreCase(resource.name())) {
                memory = numeric(resource);
            } else if ("PORTS".equalsIgnoreCase(resource.name())) {
                ports = resource.stringSetValue();
            } else if ("PORTS_UDP".equalsIgnoreCase(resource.name())) {
                portsUDP = resource.stringSetValue();
            }
        }
    }

    private static double numeric(Resource resource) {
        return "INTEGER".equalsIgnoreCase(resource.type()) ? resource.integerValue() : "LONG".equalsIgnoreCase(resource.type()) ? resource.longValue() : resource.doubleValue();
    }

    public double getCpu() {
        return cpu;
    }

    public double getMemory() {
        return memory;
    }

    public List<String> getPorts() {
        return ports;
    }

    public List<String> getPortsUDP() {
        return portsUDP;
    }

    @Override
    public String toString() {
        return "{ cpu: " + cpu + ", "
                + "memory: " + memory + ", "
                + "ports: " + ports + ", "
                + "portsUDP: " + portsUDP + "}";
    }
}
