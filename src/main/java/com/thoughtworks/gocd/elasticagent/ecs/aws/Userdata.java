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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

import com.amazonaws.util.EncodingSchemeEnum;
import com.google.gson.GsonBuilder;
import com.thoughtworks.gocd.elasticagent.ecs.builders.ScriptBuilder;
import com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthData;
import com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthType;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.elasticagent.ecs.utils.Util;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Map;

import static com.amazonaws.util.EncodingSchemeEnum.BASE64;
import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthType.AUTH_TOKEN;
import static com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthType.USERNAME_PASSWORD;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.stream.Collectors.toList;
import static org.apache.commons.lang3.StringUtils.EMPTY;

public class Userdata {
    private static final String KEY_ECS_CLUSTER = "ECS_CLUSTER";
    private static final String KEY_ECS_ENGINE_AUTH_TYPE = "ECS_ENGINE_AUTH_TYPE";
    private static final String KEY_ECS_ENGINE_AUTH_DATA = "ECS_ENGINE_AUTH_DATA";
    private static final String KEY_ECS_ENGINE_TASK_CLEANUP_WAIT_DURATION = "ECS_ENGINE_TASK_CLEANUP_WAIT_DURATION";
    private static final String KEY_ECS_IMAGE_MINIMUM_CLEANUP_AGE = "ECS_IMAGE_MINIMUM_CLEANUP_AGE";
    private static final String KEY_ECS_INSTANCE_ATTRIBUTES = "ECS_INSTANCE_ATTRIBUTES";
    private static final String LINUX_USER_DATA = Util.readResource("/userdata.template");

    private EFS efs;
    private String initScript;
    private Map<String, String> storageOptions = new HashMap<>();
    private Platform platform = Platform.LINUX;
    private Map<String, String> ecsConfig = new HashMap<>();
    private Map<String, String> customAttributes = new HashMap<>();

    public Userdata efs(String dnsOrIP, String mountDir) {
        this.efs = new EFS(dnsOrIP, mountDir);
        return this;
    }

    public Userdata initScript(String initScript) {
        this.initScript = initScript;
        return this;
    }

    public String toBase64() {
        addCustomAttributesToECSConfig();
        final String userdataScript = platform == Platform.LINUX ? toLinux() : toWindows();
        return BASE64.encodeAsString(userdataScript.getBytes());
    }

    private void addCustomAttributesToECSConfig() {
        if (!customAttributes.isEmpty()) {
            ecsConfig.put(KEY_ECS_INSTANCE_ATTRIBUTES, new GsonBuilder().serializeNulls().create().toJson(customAttributes));
        }
    }

    public static String decodeBase64(String userData) {
        return new String(EncodingSchemeEnum.BASE64.decode(userData));
    }

    public Userdata storageOption(String name, String value) {
        if (StringUtils.isAnyBlank(name, value)) {
            LOG.warn("Skipping storage option as either name or value is not specified.");
            return this;
        }

        storageOptions.put(name, value);
        return this;
    }

    public Userdata clusterName(String clusterName) {
        return addConfig(KEY_ECS_CLUSTER, clusterName);
    }

    public Userdata cleanupTaskAfter(int waitDuration, TimeUnit unit) {
        return addConfig(KEY_ECS_ENGINE_TASK_CLEANUP_WAIT_DURATION, unit.stringify(waitDuration));
    }

    private Userdata addConfig(String configKey, String configValue) {
        ecsConfig.put(configKey, configValue);
        return this;
    }

    public Userdata imageCleanupAge(int imageAge, TimeUnit unit) {
        return addConfig(KEY_ECS_IMAGE_MINIMUM_CLEANUP_AGE, unit.stringify(imageAge));
    }

    public Userdata dockerRegistry(DockerRegistryAuthType authType, DockerRegistryAuthData authData) {
        if (authType != null && (authType == AUTH_TOKEN || authType == USERNAME_PASSWORD)) {
            addConfig(KEY_ECS_ENGINE_AUTH_TYPE, authType.getValue());
            addConfig(KEY_ECS_ENGINE_AUTH_DATA, authData.toJson());
        }
        return this;
    }

    public Userdata platform(Platform platform) {
        this.platform = platform;
        return this;
    }

    private String toWindows() {
        final ScriptBuilder builder = new ScriptBuilder();
        builder.append("<powershell>");

        ecsConfig.forEach((key, value) -> builder.addLine("[Environment]::SetEnvironmentVariable(\"%s\", \"%s\", \"Machine\")", key, escapePowershell(value)));

        builder.addLine("Import-Module ECSTools")
                .addLine("Initialize-ECSAgent -Cluster '%s' -EnableTaskIAMRole", ecsConfig.get(KEY_ECS_CLUSTER))
                .addLine(initScript);

        builder.addLine("</powershell>");
        return builder.toString();
    }

    private String toLinux() {
        return LINUX_USER_DATA
                .replace("ECS_CONFIG_FILE", linuxECSConfig())
                .replace("EFS_CONFIG_SCRIPT", linuxEFSConfig())
                .replace("OVERRIDE_STORAGE_OPTION", linuxOverrideStorageOption())
                .replace("CUSTOM_USER_DATA_SCRIPT", StringUtils.isNotBlank(initScript) ? initScript : EMPTY);
    }

    private String linuxOverrideStorageOption() {
        if (storageOptions != null && !storageOptions.isEmpty()) {
            return join(" ", storageOptions.entrySet().stream().map(e -> format("--storage-opt %s", join("=", e.getKey(), e.getValue()))).collect(toList()));
        }
        return EMPTY;
    }

    private String linuxEFSConfig() {
        if (efs != null) {
            return StringUtils.stripToEmpty(efs.toScript());
        }
        return EMPTY;
    }

    private String linuxECSConfig() {
        final ScriptBuilder builder = new ScriptBuilder();
        if (!ecsConfig.isEmpty()) {
            builder.append("echo 'Creating the /etc/ecs/ecs.config files.'")
                    .addLine("cat <<EOT >> /etc/ecs/ecs.config");
            ecsConfig.forEach((key, value) -> builder.addLine("%s=%s", key, value));
            builder.addLine("EOT")
                    .addLine("echo 'File /etc/ecs/ecs.config successfully created.'");
        }
        return builder.toString();
    }

    private String escapePowershell(String stringToEscape) {
        return StringUtils.isBlank(stringToEscape) ? stringToEscape : stringToEscape.replaceAll("\"", "`\"");
    }

    public Userdata attribute(String name, String value) {
        if (StringUtils.isNotBlank(name)) {
            customAttributes.put(name, StringUtils.isBlank(value) ? EMPTY : value);
        }
        return this;
    }
}
