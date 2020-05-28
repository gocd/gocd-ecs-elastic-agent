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

import com.thoughtworks.gocd.elasticagent.ecs.builders.ScriptBuilder;
import org.apache.commons.lang3.StringUtils;

import static org.apache.commons.lang3.StringUtils.isBlank;

public class EFS {
    private final String dnsOrIP;
    private final String mountDir;

    public EFS(String dnsOrIP, String mountDir) {
        this.dnsOrIP = dnsOrIP;
        this.mountDir = mountDir;
    }

    public String toScript() {
        if (isBlank(dnsOrIP) || isBlank(mountDir)) {
            return StringUtils.EMPTY;
        }

        return new ScriptBuilder()
                .addLine("mkdir %s", mountDir)
                .addLine("mount -t nfs4 -o nfsvers=4.1,rsize=1048576,wsize=1048576,hard,timeo=600,retrans=2 %s:/  %s", dnsOrIP, mountDir)
                .addLine("service docker restart")
                .toString();
    }
}
