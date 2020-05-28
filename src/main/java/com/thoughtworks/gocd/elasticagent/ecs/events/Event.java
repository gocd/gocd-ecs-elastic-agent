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

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class Event {
    private final EventFingerprint fingerprint;
    private final EventType eventType;
    private final String message;
    private final String description;

    public Event(EventType eventType, EventFingerprint fingerprint, String message, String description) {
        this.eventType = eventType;
        this.fingerprint = fingerprint;
        this.message = message;
        this.description = description;
    }

    public EventType getEventType() {
        return eventType;
    }

    public String getMessage() {
        return message;
    }

    public String getDescription() {
        return description;
    }

    public EventFingerprint fingerprint() {
        return this.fingerprint;
    }

    public boolean isError() {
        return this.eventType == EventType.ERROR;
    }

    public boolean isWarning() {
        return this.eventType == EventType.WARNING;
    }

    public static Event errorEvent(EventFingerprint fingerprint, String message, String description) {
        return new Event(EventType.ERROR, fingerprint, message, description);
    }

    public static Event warningEvent(EventFingerprint fingerprint, String message, String description) {
        return new Event(EventType.WARNING, fingerprint, message, description);
    }

    @Override
    public String toString() {
        return "Event{" +
                "fingerprint=" + fingerprint +
                ", eventType=" + eventType +
                ", message='" + message + '\'' +
                ", description='" + description + '\'' +
                '}';
    }
}
