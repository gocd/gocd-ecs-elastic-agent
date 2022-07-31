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

package com.thoughtworks.gocd.elasticagent.ecs.events;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


class EventStreamTest {

    @Test
    void update_shouldAddEventToEventStream() {
        final EventStream eventStream = new EventStream();
        final Event event = Event.errorEvent(new EventFingerprint("foo"), "error", "description");

        eventStream.update(event);

        assertThat(eventStream.all()).hasSize(1).contains(event);
    }

    @Test
    void remove_shouldDeleteEventFromEventStreamForGivenFingerPrint() {
        final EventFingerprint fingerprint = new EventFingerprint("foo");
        final Event event = Event.errorEvent(fingerprint, "error", "description");
        final EventStream eventStream = new EventStream();

        eventStream.update(event);
        assertThat(eventStream.all()).hasSize(1).contains(event);

        eventStream.remove(fingerprint);

        assertThat(eventStream.all()).hasSize(0);
    }

    @Test
    void allErrors_shouldListAllErrorEvents() {
        final Event errorEvent = Event.errorEvent(new EventFingerprint("foo"), "error", "description");
        final EventStream eventStream = new EventStream();

        eventStream.update(errorEvent);
        eventStream.update(new Event(EventType.WARNING, new EventFingerprint("warning"), "msg", "desc"));
        eventStream.update(new Event(EventType.INFO, new EventFingerprint("info"), "msg", "desc"));

        assertThat(eventStream.allErrors()).hasSize(1).contains(errorEvent);
    }

    @Test
    void allErrors_shouldListAllWarningEvents() {
        final Event warningEvent = Event.warningEvent(new EventFingerprint("foo"), "error", "description");
        final EventStream eventStream = new EventStream();

        eventStream.update(warningEvent);
        eventStream.update(new Event(EventType.ERROR, new EventFingerprint("error"), "msg", "desc"));
        eventStream.update(new Event(EventType.INFO, new EventFingerprint("info"), "msg", "desc"));

        assertThat(eventStream.allWarnings()).hasSize(1).contains(warningEvent);
    }
}
