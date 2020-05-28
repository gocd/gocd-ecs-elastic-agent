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

import com.amazonaws.auth.*;
import com.thoughtworks.gocd.elasticagent.ecs.exceptions.AWSCredentialsException;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import static com.thoughtworks.gocd.elasticagent.ecs.ECSElasticPlugin.LOG;
import static org.apache.commons.lang3.StringUtils.*;

public class AWSCredentialsProviderChain {
    private final List<AWSCredentialsProvider> credentialsProviders = new LinkedList<AWSCredentialsProvider>();

    public AWSCredentialsProviderChain() {
        this(new EnvironmentVariableCredentialsProvider(), new SystemPropertiesCredentialsProvider(), new InstanceProfileCredentialsProvider(false));
    }

    //used in test
    public AWSCredentialsProviderChain(AWSCredentialsProvider... awsCredentialsProviders) {
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
        final AWSStaticCredentialsProvider staticCredentialProvider = staticCredentialProvider(accessKey, secretKey);
        if (staticCredentialProvider != null) {
            credentialsProviders.add(0, staticCredentialProvider);
        }

        for (AWSCredentialsProvider provider : credentialsProviders) {
            try {
                AWSCredentials credentials = provider.getCredentials();

                if (credentials.getAWSAccessKeyId() != null && credentials.getAWSSecretKey() != null) {
                    LOG.debug("Loading credentials from " + provider.toString());
                    return provider;
                }
            } catch (Exception e) {
                LOG.debug("Unable to load credentials from " + provider.toString() + ": " + e.getMessage());
            }
        }

        throw new AWSCredentialsException("Unable to load AWS credentials from any provider in the chain");
    }
}
