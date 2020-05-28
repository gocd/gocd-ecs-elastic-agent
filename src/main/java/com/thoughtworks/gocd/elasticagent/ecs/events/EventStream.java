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

package com.thoughtworks.gocd.elasticagent.ecs.events;

import org.apache.commons.collections4.map.PassiveExpiringMap;

import java.util.Collection;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

public class EventStream {
    private PassiveExpiringMap<EventFingerprint, Event> errors = new PassiveExpiringMap<>(30, TimeUnit.MINUTES);

    public void update(Event event) {
        errors.put(event.fingerprint(), event);
    }

    public Collection<Event> allErrors() {
        return errors.values().stream().filter(Event::isError).collect(Collectors.toList());
    }

    public void remove(EventFingerprint fingerprint) {
        errors.remove(fingerprint);
    }

    public Collection<Event> all() {
        return errors.values();
    }

    public Collection<Event> allWarnings() {
        return errors.values().stream().filter(Event::isWarning).collect(Collectors.toList());
    }
}


