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

import com.amazonaws.auth.*;
import com.amazonaws.services.securitytoken.AWSSecurityTokenServiceClientBuilder;
import com.thoughtworks.go.plugin.api.logging.Logger;
import com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.AWSCredentialsException;

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

    private final List<AWSCredentialsProvider> credentialsProviders = new LinkedList<>();
    private final Supplier<String> serverIdSupplier;

    public AWSCredentialsProviderChain() {
        this(ECSElasticPlugin::getServerId, new EnvironmentVariableCredentialsProvider(), new SystemPropertiesCredentialsProvider(), new InstanceProfileCredentialsProvider(false));
    }

    //used in test
    public AWSCredentialsProviderChain(AWSCredentialsProvider... awsCredentialsProviders) {
        this(ECSElasticPlugin::getServerId, awsCredentialsProviders);
    }

    //used in test
    public AWSCredentialsProviderChain(Supplier<String> serverIdSupplier, AWSCredentialsProvider... awsCredentialsProviders) {
        this.serverIdSupplier = serverIdSupplier;
        credentialsProviders.addAll(Arrays.asList(awsCredentialsProviders));
    }

    private AWSStaticCredentialsProvider staticCredentialProvider(String accessKey, String secretKey) {
        if (isNoneBlank(accessKey, secretKey)) {
            return new AWSStaticCredentialsProvider(new BasicAWSCredentials(accessKey, secretKey));
        }

        if (isBlank(accessKey) && isNotBlank(secretKey)) {
            throw new AWSCredentialsException("Access key is mandatory if secret key is provided");
        }

        if (isNotBlank(accessKey) && isBlank(secretKey)) {
            throw new AWSCredentialsException("Secret key is mandatory if access key is provided");
        }
        return null;
    }

    public AWSCredentialsProvider getAWSCredentialsProvider(String accessKey, String secretKey) {
        return getAWSCredentialsProvider(accessKey, secretKey, null, null);
    }

    public AWSCredentialsProvider getAWSCredentialsProvider(String accessKey, String secretKey, String assumeRoleArn, String clusterName) {
        final AWSStaticCredentialsProvider staticCredentialProvider = staticCredentialProvider(accessKey, secretKey);
        if (staticCredentialProvider != null) {
            credentialsProviders.addFirst(staticCredentialProvider);
        }

        for (AWSCredentialsProvider provider : credentialsProviders) {
            try {
                AWSCredentials credentials = provider.getCredentials();

                if (credentials.getAWSAccessKeyId() != null && credentials.getAWSSecretKey() != null) {
                    LOG.debug("Loading credentials from {}", provider);
                    return withAssumedRoleIfConfigured(provider, assumeRoleArn, clusterName);
                }
            } catch (Exception e) {
                LOG.debug("Unable to load credentials from " + provider.toString() + ": " + e.getMessage());
            }
        }

        throw new RuntimeException("Unable to load AWS credentials from any provider in the chain");
    }

    private AWSCredentialsProvider withAssumedRoleIfConfigured(AWSCredentialsProvider provider, String assumeRoleArn, String clusterName) {
        if (isBlank(assumeRoleArn)) {
            return provider;
        }

        LOG.debug("Assuming role " + assumeRoleArn + " using credentials from " + provider.toString());
        final STSAssumeRoleSessionCredentialsProvider.Builder builder = new STSAssumeRoleSessionCredentialsProvider.Builder(assumeRoleArn, roleSessionName(clusterName))
                .withStsClient(AWSSecurityTokenServiceClientBuilder.standard()
                        .withCredentials(provider)
                        .build());

        final String externalId = externalId();
        if (externalId == null) {
            LOG.warn("GoCD server id is not available; assuming role " + assumeRoleArn + " without an external id");
        } else {
            builder.withExternalId(externalId);
        }
        return builder.build();
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
