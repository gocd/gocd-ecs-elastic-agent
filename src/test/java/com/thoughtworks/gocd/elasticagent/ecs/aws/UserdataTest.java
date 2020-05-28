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

package com.thoughtworks.gocd.elasticagent.ecs.aws;

import com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthData;
import com.thoughtworks.gocd.elasticagent.ecs.domain.DockerRegistryAuthType;
import com.thoughtworks.gocd.elasticagent.ecs.domain.Platform;
import com.thoughtworks.gocd.extensions.FileSource;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;

import static com.thoughtworks.gocd.elasticagent.ecs.aws.Userdata.decodeBase64;
import static org.assertj.core.api.Assertions.assertThat;

class UserdataTest {
    @ParameterizedTest
    @FileSource(files = "/userdata/linux-default.sh")
    void shouldUseLinuxAsDefaultPlatformWhenNotSpecified(String expectedUserdata) {
        final String userdataScript = new Userdata().toBase64();
        assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdata);
    }

    @Nested
    class Linux {
        private Userdata userdata;

        @BeforeEach
        void setUp() {
            userdata = new Userdata().platform(Platform.LINUX);
        }

        @ParameterizedTest
        @FileSource(files = "/userdata/linux-with-cluster.sh")
        void shouldBuildUserdataWithClusterName(String expectedUserdataScript) {
            final String userdataScript = userdata
                    .clusterName("example-cluster")
                    .toBase64();

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @ParameterizedTest
        @FileSource(files = "/userdata/linux-with-task-cleanup.sh")
        void shouldBuildUserdataWithTaskCleanUp(String expectedUserdataScript) {
            String userdataScript = userdata.cleanupTaskAfter(10, TimeUnit.MINUTES)
                    .toBase64();

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @ParameterizedTest
        @FileSource(files = "/userdata/linux-with-image-cleanup.sh")
        void shouldBuildUserdataWithImageCleanupAge(String expectedUserdataScript) {
            String userdataScript = userdata.imageCleanupAge(24, TimeUnit.HOURS)
                    .toBase64();

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @ParameterizedTest
        @FileSource(files = "/userdata/linux-with-docker-registry.sh")
        void shouldBuildUserdataScriptWithDockerRegistry(String expectedUserdataScript) {
            final String userdataScript = userdata.dockerRegistry(DockerRegistryAuthType.AUTH_TOKEN, new DockerRegistryAuthData("url", "some-token", "email"))
                    .toBase64();


            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @ParameterizedTest
        @FileSource(files = "/userdata/linux-with-init-script.sh")
        void shouldBuildUserdataWithInitScript(String expectedUserdataScript) {
            final String userdataScript = userdata.initScript("echo \"this is an example init script.\"")
                    .toBase64();

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @ParameterizedTest
        @FileSource(files = "/userdata/with-storage-config.sh")
        void shouldCreateUserdataWithStorageOptions(String expectedUserdataScript) {
            final String userdataScript = userdata.storageOption("dm.basesize", "20G")
                    .storageOption("dm.fs", "ext4").toBase64();

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @ParameterizedTest
        @FileSource(files = "/userdata/full-userdata.sh")
        void shouldBuildCompleteUserdataScript(String expectedUserdataScript) {
            final String userdataScript = userdata.clusterName("some-cluster")
                    .cleanupTaskAfter(1, TimeUnit.MINUTES)
                    .imageCleanupAge(24, TimeUnit.HOURS)
                    .dockerRegistry(DockerRegistryAuthType.AUTH_TOKEN, new DockerRegistryAuthData("url", "some-token", "email"))
                    .storageOption("foo", "bsfds")
                    .efs("3.4.5.7", "/dir/")
                    .initScript("some-script")
                    .toBase64();

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @ParameterizedTest
        @FileSource(files = "/userdata/with-custom-attributes.sh")
        void shouldBuildUserdataWithCustomAttributes(String expectedUserdataScript) {
            final String userdataScript = userdata
                    .attribute("FirstName", "Bob")
                    .attribute("LastName", "Ford")
                    .toBase64();

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }
    }

    @Nested
    class Windows {
        private Userdata userdata;

        @BeforeEach
        void setUp() {
            userdata = new Userdata().platform(Platform.WINDOWS);
        }

        @Test
        void shouldBuildUserdataWithClusterNameAsEnvironmentVariable() {
            final String userdataScript = userdata
                    .clusterName("GoCD")
                    .toBase64();

            final String expectedUserdataScript = "<powershell>\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_CLUSTER\", \"GoCD\", \"Machine\")\n" +
                    "Import-Module ECSTools\n" +
                    "Initialize-ECSAgent -Cluster 'GoCD' -EnableTaskIAMRole\n" +
                    "</powershell>";

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @Test
        void shouldBuildUserdataWithTaskCleanUpDurationAsEnvironmentVariable() {
            final String userdataScript = userdata.cleanupTaskAfter(10, TimeUnit.MINUTES)
                    .toBase64();

            final String expectedUserdataScript = "<powershell>\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_ENGINE_TASK_CLEANUP_WAIT_DURATION\", \"10m\", \"Machine\")\n" +
                    "Import-Module ECSTools\n" +
                    "Initialize-ECSAgent -Cluster 'null' -EnableTaskIAMRole\n" +
                    "</powershell>";

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @Test
        void shouldBuildUserdataWithImageCleanupAgeAsEnvironmentVariable() {
            final String userdataScript = userdata.imageCleanupAge(24, TimeUnit.HOURS)
                    .toBase64();

            final String expectedUserdataScript = "<powershell>\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_IMAGE_MINIMUM_CLEANUP_AGE\", \"24h\", \"Machine\")\n" +
                    "Import-Module ECSTools\n" +
                    "Initialize-ECSAgent -Cluster 'null' -EnableTaskIAMRole\n" +
                    "</powershell>";

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @Test
        void shouldBuildUserdataWithDockerRegistryAsEnvironmentVariable() {
            final String userdataScript = userdata.dockerRegistry(DockerRegistryAuthType.AUTH_TOKEN, new DockerRegistryAuthData("url", "some-token", "email"))
                    .toBase64();

            final String expectedUserdataScript = "<powershell>\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_ENGINE_AUTH_TYPE\", \"dockercfg\", \"Machine\")\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_ENGINE_AUTH_DATA\", \"{`\"url`\":{`\"auth`\":`\"some-token`\",`\"email`\":`\"email`\"}}\", \"Machine\")\n" +
                    "Import-Module ECSTools\n" +
                    "Initialize-ECSAgent -Cluster 'null' -EnableTaskIAMRole\n" +
                    "</powershell>";

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);

        }

        @Test
        void shouldBuildUserdataWithInitScript() {
            final String userdataScript = userdata.initScript("some-script")
                    .toBase64();

            final String expectedUserdataScript = "<powershell>\n" +
                    "Import-Module ECSTools\n" +
                    "Initialize-ECSAgent -Cluster 'null' -EnableTaskIAMRole\n" +
                    "some-script\n" +
                    "</powershell>";

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @Test
        @Disabled("Do not know how to change docker storage option read: https://docs.microsoft.com/en-us/virtualization/windowscontainers/manage-docker/configure-docker-daemon")
        void shouldCreateUserdataWithStorageOptions() {
            final String userdataScript = userdata.storageOption("dm.basesize", "20G")
                    .storageOption("dm.fs", "ext4").toBase64();

            final String expectedUserdataScript = "<powershell>\n" +
                    "" +
                    "</powershell>";

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }

        @Test
        void shouldBuildUserdataWithCustomAttributes() {
            final String userdataScript = userdata
                    .attribute("FirstName", "Bob")
                    .attribute("LastName", "Ford")
                    .toBase64();

            final String expectedUserdataScript = "<powershell>\n" +
                    "[Environment]::SetEnvironmentVariable(\"ECS_INSTANCE_ATTRIBUTES\", \"{`\"FirstName`\":`\"Bob`\",`\"LastName`\":`\"Ford`\"}\", \"Machine\")\n" +
                    "Import-Module ECSTools\n" +
                    "Initialize-ECSAgent -Cluster 'null' -EnableTaskIAMRole\n" +
                    "</powershell>";

            assertThat(decodeBase64(userdataScript)).isEqualTo(expectedUserdataScript);
        }
    }
}
