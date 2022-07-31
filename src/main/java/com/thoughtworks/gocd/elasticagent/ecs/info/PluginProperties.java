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

package com.thoughtworks.gocd.elasticagent.ecs.info;

import java.io.IOException;
import java.io.StringReader;
import java.util.Properties;

import static com.thoughtworks.gocd.elasticagent.ecs.utils.Util.readResource;

public class PluginProperties {
    private final Properties properties;
    private static PluginProperties pluginProperties;

    private PluginProperties(Properties properties) {
        this.properties = properties;
    }

    public String get(String propertyName) {
        return properties.getProperty(propertyName);
    }

    public static PluginProperties instance() {
        if (pluginProperties != null) {
            return pluginProperties;
        }

        synchronized (PluginProperties.class) {
            if (pluginProperties != null) {
                return pluginProperties;
            }

            final String propertiesAsAString = readResource("/plugin.properties");
            try {
                Properties properties = new Properties();
                properties.load(new StringReader(propertiesAsAString));
                pluginProperties = new PluginProperties(properties);
                return pluginProperties;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }
}
