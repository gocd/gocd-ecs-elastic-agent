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

import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.gocd.elasticagent.ecs.Clock;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.apache5.Apache5HttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.ec2.Ec2Client;
import software.amazon.awssdk.services.ecs.EcsClient;
import software.amazon.awssdk.utils.SdkAutoCloseable;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Caches AWS SDK v2 clients per distinct client configuration.
 *
 * <p>PluginSettings is deserialized fresh for every plugin request, so caching must be process-wide and
 * keyed by the values that determine client identity. v2 clients are thread-safe and designed to be
 * long-lived; building one per call wastes a connection pool, a TLS handshake per request, and (with an
 * assume-role configuration) an sts:AssumeRole round-trip per call, since the credential providers' own
 * session caching only works when the provider instance is reused.
 *
 * <p>Entries idle for longer than the idle timeout are closed and evicted by a lazy sweep on access. Idle
 * pooled connections inside a <em>live</em> entry are already closed by the SDK's idle-connection reaper
 * (default connectionMaxIdleTime of 60s), so an idle plugin holds no open sockets either way.
 */
public class AwsClientCache {
    private static final Logger LOG = Logger.getLoggerFor(AwsClientCache.class);
    private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);
    private static final AwsClientCache INSTANCE = new AwsClientCache(Duration.ofMinutes(30), Clock.DEFAULT, AwsClientCache::createClients);

    private final ConcurrentMap<ClientKey, CachedClients> cache = new ConcurrentHashMap<>();
    private final Duration idleTimeout;
    private final Clock clock;
    private final Function<ClientKey, CachedClients> factory;
    private final AtomicReference<Instant> nextSweepAt = new AtomicReference<>(Instant.EPOCH);

    AwsClientCache(Duration idleTimeout, Clock clock, Function<ClientKey, CachedClients> factory) {
        this.idleTimeout = idleTimeout;
        this.clock = clock;
        this.factory = factory;
    }

    public static AwsClientCache instance() {
        return INSTANCE;
    }

    /**
     * Returns the cached clients for {@code key}, building them on first use. The idle clock is stamped
     * here, on fetch — so callers must fetch immediately before each use (as PluginSettings.ecsClient()/
     * ec2Client() do per AWS call), not hold a returned client across an operation longer than the idle
     * timeout, or a concurrent sweep could close the shared HTTP pool mid-operation.
     */
    public CachedClients get(ClientKey key) {
        // compute (not computeIfAbsent) so the last-access stamp is updated under the map's own lock,
        // preventing a concurrent sweep from seeing a just-created or just-fetched entry as idle
        final CachedClients clients = cache.compute(key, (k, existing) -> {
            final CachedClients cached = existing != null ? existing : factory.apply(k);
            cached.lastAccessAt = clock.now();
            return cached;
        });

        // sweep only after touching this entry, so a request for clients sitting exactly at the idle
        // boundary reuses them rather than closing and immediately rebuilding them
        sweepIfDue();

        return clients;
    }

    /**
     * Builds clients exclusively from {@link ClientKey} components, so client construction cannot depend
     * on a setting that is not part of the cache key: anything new that affects construction must be added
     * to the key, which is exactly what keeps the cache correct.
     */
    private static CachedClients createClients(ClientKey key) {
        // Resolve the region first: it is an optional setting, so Region.of throws on a blank value, and
        // doing it before allocating anything avoids leaking a half-built client on that (or any other)
        // construction failure.
        final Region region = Region.of(key.region());

        AwsCredentialsProvider credentials = null;
        SdkHttpClient httpClient = null;
        try {
            credentials = new AWSCredentialsProviderChain()
                    .getAwsCredentialsProvider(key.accessKeyId(), key.secretAccessKey(), key.assumeRoleArn(), key.clusterName());
            httpClient = Apache5HttpClient.builder().build();
            return new CachedClients(
                    EcsClient.builder().httpClient(httpClient).credentialsProvider(credentials).region(region).build(),
                    Ec2Client.builder().httpClient(httpClient).credentialsProvider(credentials).region(region).build(),
                    credentials,
                    httpClient);
        } catch (RuntimeException e) {
            closeQuietly(httpClient);
            if (credentials instanceof SdkAutoCloseable closeable) {
                closeQuietly(closeable);
            }
            throw e;
        }
    }

    private void sweepIfDue() {
        final Instant now = clock.now();
        final Instant sweepDueAt = nextSweepAt.get();
        if (now.isBefore(sweepDueAt) || !nextSweepAt.compareAndSet(sweepDueAt, now.plus(SWEEP_INTERVAL))) {
            return;
        }

        // evicting via compute makes the idle-check, removal and close atomic with get()'s touch of the
        // same key (both run under the map's per-key lock), so no caller can be handed clients that a
        // concurrent sweep is closing
        //noinspection resource
        cache.forEach((key, ignored) -> cache.compute(key, (k, clients) -> {
            if (clients == null || Duration.between(clients.lastAccessAt, now).compareTo(idleTimeout) < 0) {
                return clients;
            }

            LOG.info("Closing AWS clients for cluster " + k.clusterName() + " which have been idle for over " + idleTimeout);
            clients.close();
            return null;
        }));
    }

    private static void closeQuietly(SdkAutoCloseable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (RuntimeException e) {
            LOG.warn("Error closing AWS client resource " + e);
        }
    }

    /** The plugin settings that determine client identity; anything else can change without a client rebuild. */
    public record ClientKey(String region, String accessKeyId, String secretAccessKey, String assumeRoleArn, String clusterName) {
    }

    public static final class CachedClients implements AutoCloseable {
        private final EcsClient ecsClient;
        private final Ec2Client ec2Client;
        private final AwsCredentialsProvider credentialsProvider;
        private final SdkHttpClient httpClient;
        private volatile Instant lastAccessAt;

        public CachedClients(EcsClient ecsClient, Ec2Client ec2Client, AwsCredentialsProvider credentialsProvider, SdkHttpClient httpClient) {
            this.ecsClient = ecsClient;
            this.ec2Client = ec2Client;
            this.credentialsProvider = credentialsProvider;
            this.httpClient = httpClient;
        }

        public EcsClient ecs() {
            return ecsClient;
        }

        public Ec2Client ec2() {
            return ec2Client;
        }

        @Override
        public void close() {
            closeQuietly(ecsClient);
            closeQuietly(ec2Client);
            if (credentialsProvider instanceof SdkAutoCloseable closeable) {
                closeQuietly(closeable);
            }
            // last: the SDK clients above may still hold leases on the shared pool until they close
            closeQuietly(httpClient);
        }
    }
}
