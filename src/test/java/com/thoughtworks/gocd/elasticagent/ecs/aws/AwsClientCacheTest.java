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

import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ecs.EcsClient;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AwsClientCacheTest {
    private static final Duration IDLE_TIMEOUT = Duration.ofMinutes(30);

    private Clock.TestClock testClock;
    private AwsClientCache clientCache;
    private AtomicInteger factoryInvocations;
    private Map<AwsClientCache.CachedClients, SdkHttpClient> httpClients;

    @BeforeEach
    void setUp() {
        testClock = new Clock.TestClock();
        factoryInvocations = new AtomicInteger();
        httpClients = new HashMap<>();
        clientCache = new AwsClientCache(IDLE_TIMEOUT, testClock, this::newCachedClients);
    }

    @Test
    void shouldReturnTheSameClientsForTheSameKey() {
        final AwsClientCache.CachedClients first = clientCache.get(key("cluster-one"));
        final AwsClientCache.CachedClients second = clientCache.get(key("cluster-one"));

        assertThat(second).isSameAs(first);
        assertThat(factoryInvocations).hasValue(1);
    }

    @Test
    void shouldReturnDifferentClientsForDifferentKeys() {
        final AwsClientCache.CachedClients first = clientCache.get(key("cluster-one"));
        final AwsClientCache.CachedClients second = clientCache.get(key("cluster-two"));

        assertThat(second).isNotSameAs(first);
        assertThat(factoryInvocations).hasValue(2);
    }

    @Test
    void shouldCloseAndEvictClientsIdleForLongerThanTheIdleTimeout() {
        final AwsClientCache.CachedClients idleClients = clientCache.get(key("cluster-one"));

        testClock.forward(IDLE_TIMEOUT.plusMinutes(1));
        clientCache.get(key("cluster-two"));

        verify(idleClients.ecs()).close();
        verify(idleClients.ec2()).close();
        verify(httpClients.get(idleClients)).close();
        assertThat(clientCache.get(key("cluster-one"))).isNotSameAs(idleClients);
    }

    @Test
    void shouldReuseClientsRequestedAtTheIdleBoundaryRatherThanEvictingThem() {
        final AwsClientCache.CachedClients clients = clientCache.get(key("cluster-one"));

        testClock.forward(IDLE_TIMEOUT.plusMinutes(1));

        assertThat(clientCache.get(key("cluster-one"))).isSameAs(clients);
        verify(clients.ecs(), never()).close();
        verify(clients.ec2(), never()).close();
    }

    @Test
    void shouldNotEvictClientsAccessedWithinTheIdleTimeout() {
        final AwsClientCache.CachedClients clients = clientCache.get(key("cluster-one"));

        testClock.forward(IDLE_TIMEOUT.minusMinutes(1));
        clientCache.get(key("cluster-one"));

        testClock.forward(IDLE_TIMEOUT.minusMinutes(1));
        assertThat(clientCache.get(key("cluster-one"))).isSameAs(clients);

        verify(clients.ecs(), never()).close();
        verify(clients.ec2(), never()).close();
    }

    @Test
    void shouldNotCacheAnEntryWhenClientConstructionFails() {
        final AtomicInteger attempts = new AtomicInteger();
        final AwsClientCache failingCache = new AwsClientCache(IDLE_TIMEOUT, testClock, k -> {
            attempts.incrementAndGet();
            throw new IllegalArgumentException("construction failed");
        });

        assertThatThrownBy(() -> failingCache.get(key("cluster-one"))).isInstanceOf(IllegalArgumentException.class);
        // no half-built entry is cached, so a later call retries construction rather than serving garbage
        assertThatThrownBy(() -> failingCache.get(key("cluster-one"))).isInstanceOf(IllegalArgumentException.class);
        assertThat(attempts).hasValue(2);
    }

    private AwsClientCache.ClientKey key(String clusterName) {
        return new AwsClientCache.ClientKey("us-east-1", "access-key", "secret-key", null, clusterName);
    }

    private AwsClientCache.CachedClients newCachedClients(AwsClientCache.ClientKey key) {
        factoryInvocations.incrementAndGet();
        final SdkHttpClient httpClient = mock(SdkHttpClient.class);
        final AwsClientCache.CachedClients cachedClients = new AwsClientCache.CachedClients(
                mock(EcsClient.class),
                mock(Ec2Client.class),
                StaticCredentialsProvider.create(AwsBasicCredentials.create("access-key", "secret-key")),
                httpClient);
        httpClients.put(cachedClients, httpClient);
        return cachedClients;
    }
}
