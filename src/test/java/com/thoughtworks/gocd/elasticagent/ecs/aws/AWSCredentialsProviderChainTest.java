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

import com.thoughtworks.gocd.elasticagent.ecs.exceptions.AWSCredentialsException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import software.amazon.awssdk.auth.credentials.*;
import uk.org.webcompere.systemstubs.environment.EnvironmentVariables;
import uk.org.webcompere.systemstubs.jupiter.SystemStub;
import uk.org.webcompere.systemstubs.jupiter.SystemStubsExtension;
import uk.org.webcompere.systemstubs.properties.SystemProperties;

import static org.apache.commons.lang3.StringUtils.repeat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_ACCESS_KEY_ID;
import static software.amazon.awssdk.core.SdkSystemSetting.AWS_SECRET_ACCESS_KEY;

@ExtendWith(SystemStubsExtension.class)
class AWSCredentialsProviderChainTest {

    @SystemStub
    EnvironmentVariables environmentVariables = new EnvironmentVariables()
            .set(AWS_ACCESS_KEY_ID.environmentVariable(), "")
            .set(AWS_SECRET_ACCESS_KEY.environmentVariable(), "");

    @SystemStub
    SystemProperties systemProperties;

    private AWSCredentialsProviderChain awsCredentialsProviderChain;

    @BeforeEach
    void setUp() {
        awsCredentialsProviderChain = new AWSCredentialsProviderChain(EnvironmentVariableCredentialsProvider.create(), SystemPropertyCredentialsProvider.create());
    }

    @Test
    void shouldUseAccessKeyAndSecretKeyAsACredentialsIfProvided() {
        final AwsCredentialsProvider credentialsProvider = awsCredentialsProviderChain.getAwsCredentialsProvider("access-key", "secret-key");

        assertThat(credentialsProvider).isInstanceOf(StaticCredentialsProvider.class);

        final AwsCredentials credentials = credentialsProvider.resolveCredentials();
        assertThat(credentials.accessKeyId()).isEqualTo("access-key");
        assertThat(credentials.secretAccessKey()).isEqualTo("secret-key");
    }

    @Test
    void shouldReadCredentialsFromEnvironmentIfNotProvidedInMethodCall() throws Exception {
        environmentVariables
                .set(AWS_ACCESS_KEY_ID.environmentVariable(), "access-key-from-env")
                .set(AWS_SECRET_ACCESS_KEY.environmentVariable(), "secret-key-from-env")
                .execute(() -> {
                    final AwsCredentialsProvider credentialsProvider = awsCredentialsProviderChain.getAwsCredentialsProvider(null, null);
                    assertThat(credentialsProvider).isInstanceOf(EnvironmentVariableCredentialsProvider.class);

                    final AwsCredentials credentials = credentialsProvider.resolveCredentials();
                    assertThat(credentials.accessKeyId()).isEqualTo("access-key-from-env");
                    assertThat(credentials.secretAccessKey()).isEqualTo("secret-key-from-env");
                });
    }

    @Test
    void shouldReadCredentialsFromSystemPropertiesWhenEnvCredentialsAreNotProvided() throws Exception {
        systemProperties
                .set(AWS_ACCESS_KEY_ID.property(), "access-key-from-system-prop")
                .set(AWS_SECRET_ACCESS_KEY.property(), "secret-key-from-system-prop")
                .execute(() -> {
                    final AwsCredentialsProvider credentialsProvider = awsCredentialsProviderChain.getAwsCredentialsProvider(null, null);
                    assertThat(credentialsProvider).isInstanceOf(SystemPropertyCredentialsProvider.class);

                    final AwsCredentials credentials = credentialsProvider.resolveCredentials();
                    assertThat(credentials.accessKeyId()).isEqualTo("access-key-from-system-prop");
                    assertThat(credentials.secretAccessKey()).isEqualTo("secret-key-from-system-prop");
                });
    }

    @Test
    void shouldAssumeRoleUsingSTSIfAssumeRoleArnIsProvided() throws Exception {
        environmentVariables
                .set("AWS_REGION", "us-east-1")
                .execute(() -> {
                    final AwsCredentialsProvider credentialsProvider = awsCredentialsProviderChain.getAwsCredentialsProvider("access-key", "secret-key", "arn:aws:iam::111111111111:role/gocd-ecs-plugin-role", "GoCD");

                    assertThat(credentialsProvider).isInstanceOf(AWSCredentialsProviderChain.AssumeRoleProviderOwningStsClient.class);
                });
    }

    @Test
    void shouldAssumeRoleWithServerIdBasedExternalIdWhenServerIdIsAvailable() throws Exception {
        environmentVariables
                .set("AWS_REGION", "us-east-1")
                .execute(() -> {
                    final AWSCredentialsProviderChain chain = new AWSCredentialsProviderChain(() -> "some-server-id", EnvironmentVariableCredentialsProvider.create(), SystemPropertyCredentialsProvider.create());
                    final AwsCredentialsProvider credentialsProvider = chain.getAwsCredentialsProvider("access-key", "secret-key", "arn:aws:iam::111111111111:role/gocd-ecs-plugin-role", "GoCD");

                    assertThat(credentialsProvider).isInstanceOf(AWSCredentialsProviderChain.AssumeRoleProviderOwningStsClient.class);
                });
    }

    @Test
    void shouldNotAssumeRoleIfAssumeRoleArnIsBlank() {
        final AwsCredentialsProvider credentialsProvider = awsCredentialsProviderChain.getAwsCredentialsProvider("access-key", "secret-key", " ", "GoCD");

        assertThat(credentialsProvider).isInstanceOf(StaticCredentialsProvider.class);
    }

    @Test
    void shouldDeriveExternalIdFromGoCDServerId() {
        assertThat(new AWSCredentialsProviderChain(() -> "some-server-id").externalId()).isEqualTo("gocd:server-id:some-server-id");
    }

    @Test
    void shouldHaveNoExternalIdWhenServerIdIsNotAvailable() {
        assertThat(new AWSCredentialsProviderChain(() -> (String) null).externalId()).isNull();
        assertThat(new AWSCredentialsProviderChain(() -> " ").externalId()).isNull();
    }

    @Test
    void shouldIncludeClusterNameInRoleSessionName() {
        assertThat(awsCredentialsProviderChain.roleSessionName("my-cluster")).isEqualTo("gocd-ecs-elastic-agent-plugin@my-cluster");
    }

    @Test
    void shouldUseBareRoleSessionNameWhenClusterNameIsBlank() {
        assertThat(awsCredentialsProviderChain.roleSessionName(null)).isEqualTo("gocd-ecs-elastic-agent-plugin");
        assertThat(awsCredentialsProviderChain.roleSessionName(" ")).isEqualTo("gocd-ecs-elastic-agent-plugin");
    }

    @Test
    void shouldSanitizeCharactersNotAllowedInRoleSessionNames() {
        assertThat(awsCredentialsProviderChain.roleSessionName("my cluster/name")).isEqualTo("gocd-ecs-elastic-agent-plugin@my-cluster-name");
    }

    @Test
    void shouldTruncateRoleSessionNameToMaximumAllowedLength() {
        final String roleSessionName = awsCredentialsProviderChain.roleSessionName(repeat("x", 100));

        assertThat(roleSessionName).hasSize(64);
        assertThat(roleSessionName).startsWith("gocd-ecs-elastic-agent-plugin@xxx");
    }

    @Test
    void shouldErrorOutIfItFailsToLoadCredentials() {
        assertThatThrownBy(() -> awsCredentialsProviderChain.getAwsCredentialsProvider(null, null))
                .isExactlyInstanceOf(RuntimeException.class)
                .hasMessage("Unable to load AWS credentials from any provider in the chain");
    }

    @Test
    void shouldErrorOutIfOnlyAccessKeyIsProvided() {
        assertThatThrownBy(() -> awsCredentialsProviderChain.getAwsCredentialsProvider("access-key", null))
                .isExactlyInstanceOf(AWSCredentialsException.class)
                .hasMessage("Secret key is mandatory if access key is provided");
    }

    @Test
    void shouldErrorOutIfOnlySecretKeyIsProvided() {
        assertThatThrownBy(() -> awsCredentialsProviderChain.getAwsCredentialsProvider(null, "secret-key"))
                .isExactlyInstanceOf(AWSCredentialsException.class)
                .hasMessage("Access key is mandatory if secret key is provided");
    }
}
