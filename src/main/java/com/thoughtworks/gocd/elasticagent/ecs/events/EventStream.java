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

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

public class EventStream {
    private final PassiveExpiringHolder<EventFingerprint, Event> errors = new PassiveExpiringHolder<>(Duration.ofMinutes(30));

    public void update(Event event) {
        errors.put(event.fingerprint(), event);
    }

    public Collection<Event> allErrors() {
        return errors.values().stream().filter(Event::isError).toList();
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

    private static class PassiveExpiringHolder<K, V> {
        private final ConcurrentMap<K, Entry<V>> map = new ConcurrentHashMap<>();
        private final long ttlNanos;

        PassiveExpiringHolder(Duration ttl) { this.ttlNanos = ttl.toNanos(); }

        void put(K key, V value) {
            map.put(key, new Entry<>(value, System.nanoTime() + ttlNanos));
        }

        public Collection<V> values() {
            long now = System.nanoTime();
            List<V> live = new ArrayList<>();
            for (var entry : map.entrySet()) {
                Entry<V> e = entry.getValue();
                if (e.isExpired(now)) {
                    map.remove(entry.getKey(), e);   // conditional: won't clobber a concurrent re-put
                } else {
                    live.add(e.value());
                }
            }
            return live;
        }

        void remove(K key) {
            map.remove(key);
        }

        private record Entry<V>(V value, long expiresAt) {
            private boolean isExpired(long nanos) {
                return nanos - expiresAt >= 0;
            }
        }
    }
}


