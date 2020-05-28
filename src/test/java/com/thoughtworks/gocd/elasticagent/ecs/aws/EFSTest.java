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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class EFSTest {

    @Test
    void shouldBuildEFSScript() {
        final String userDataScript = new EFS("foo", "bar").toScript();

        final String expectedUserdataScript = "\nmkdir bar\n" +
                "mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2 foo:/  bar\n" +
                "service docker restart";

        assertThat(userDataScript).isEqualTo(expectedUserdataScript);
    }

    @Test
    void shouldReturnEmptyStringIfDNSOrMountDirEmpty() {
        assertThat(new EFS("", "bar").toScript()).isEmpty();
        assertThat(new EFS("foo", "").toScript()).isEmpty();
    }
}
