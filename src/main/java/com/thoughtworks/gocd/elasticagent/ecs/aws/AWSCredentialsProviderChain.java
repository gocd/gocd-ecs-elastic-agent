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
import com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.AWSCredentialsException;
import software.amazon.awssdk.auth.credentials.*;
import software.amazon.awssdk.services.sts.StsClient;
import software.amazon.awssdk.services.sts.auth.StsAssumeRoleCredentialsProvider;
import software.amazon.awssdk.services.sts.model.AssumeRoleRequest;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Supplier;

import static org.apache.commons.lang3.StringUtils.*;

public class AWSCredentialsProviderChain {
    private static final Logger LOG = Logger.getLoggerFor(AWSCredentialsProviderChain.class);
    private static final String ROLE_SESSION_NAME = "gocd-ecs-elastic-agent-plugin";
    private static final int MAX_ROLE_SESSION_NAME_LENGTH = 64;
    // Documented for use within sts:ExternalId trust policy conditions; changing the format breaks existing policies.
    public static final String EXTERNAL_ID_PREFIX = "gocd:server-id:";

    private final List<AwsCredentialsProvider> credentialsProviders = new LinkedList<>();
    private final Supplier<String> serverIdSupplier;

    public AWSCredentialsProviderChain() {
        this(ECSElasticPlugin::getServerId, EnvironmentVariableCredentialsProvider.create(), SystemPropertyCredentialsProvider.create(), InstanceProfileCredentialsProvider.builder().asyncCredentialUpdateEnabled(false).build());
    }

    //used in test
    public AWSCredentialsProviderChain(AwsCredentialsProvider... awsCredentialsProviders) {
        this(ECSElasticPlugin::getServerId, awsCredentialsProviders);
    }

    //used in test
    public AWSCredentialsProviderChain(Supplier<String> serverIdSupplier, AwsCredentialsProvider... awsCredentialsProviders) {
        this.serverIdSupplier = serverIdSupplier;
        credentialsProviders.addAll(Arrays.asList(awsCredentialsProviders));
    }

    private StaticCredentialsProvider staticCredentialProvider(String accessKey, String secretKey) {
        if (isNoneBlank(accessKey, secretKey)) {
            return StaticCredentialsProvider.create(AwsBasicCredentials.create(accessKey, secretKey));
        }

        if (isBlank(accessKey) && isNotBlank(secretKey)) {
            throw new AWSCredentialsException("Access key is mandatory if secret key is provided");
        }

        if (isNotBlank(accessKey) && isBlank(secretKey)) {
            throw new AWSCredentialsException("Secret key is mandatory if access key is provided");
        }
        return null;
    }

    public AwsCredentialsProvider getAwsCredentialsProvider(String accessKey, String secretKey) {
        return getAwsCredentialsProvider(accessKey, secretKey, null, null);
    }

    public AwsCredentialsProvider getAwsCredentialsProvider(String accessKey, String secretKey, String assumeRoleArn, String clusterName) {
        final StaticCredentialsProvider staticCredentialProvider = staticCredentialProvider(accessKey, secretKey);
        if (staticCredentialProvider != null) {
            credentialsProviders.addFirst(staticCredentialProvider);
        }

        for (AwsCredentialsProvider provider : credentialsProviders) {
            try {
                AwsCredentials credentials = provider.resolveCredentials();

                if (credentials.accessKeyId() != null && credentials.secretAccessKey() != null) {
                    LOG.debug("Loading credentials from {}", provider);
                    return withAssumedRoleIfConfigured(provider, assumeRoleArn, clusterName);
                }
            } catch (Exception e) {
                LOG.debug("Unable to load credentials from " + provider.toString() + ": " + e.getMessage());
            }
        }

        throw new RuntimeException("Unable to load AWS credentials from any provider in the chain");
    }

    private AwsCredentialsProvider withAssumedRoleIfConfigured(AwsCredentialsProvider provider, String assumeRoleArn, String clusterName) {
        if (isBlank(assumeRoleArn)) {
            return provider;
        }

        LOG.debug("Assuming role " + assumeRoleArn + " using credentials from " + provider.toString());
        return StsAssumeRoleCredentialsProvider.builder().asyncCredentialUpdateEnabled(false)
                .stsClient(StsClient.builder().credentialsProvider(provider).build())
                .refreshRequest(() -> AssumeRoleRequest.builder()
                        .roleArn(assumeRoleArn)
                        .roleSessionName(roleSessionName(clusterName))
                        .externalId(externalId())
                        .build())
                .build();
    }

    String externalId() {
        final String serverId = serverIdSupplier.get();
        return isBlank(serverId) ? null : EXTERNAL_ID_PREFIX + serverId;
    }

    String roleSessionName(String clusterName) {
        if (isBlank(clusterName)) {
            return ROLE_SESSION_NAME;
        }

        final String sessionName = ROLE_SESSION_NAME + "@" + clusterName.replaceAll("[^\\w+=,.@-]", "-");
        return sessionName.length() <= MAX_ROLE_SESSION_NAME_LENGTH ? sessionName : sessionName.substring(0, MAX_ROLE_SESSION_NAME_LENGTH);
    }

}
