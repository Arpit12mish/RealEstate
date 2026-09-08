package com.brandPitara.sfs.media.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;

import static org.assertj.core.api.Assertions.assertThat;

class AwsS3ConfigTest {

    /**
     * Regression guard for the 403 InvalidAccessKeyId bug: a prior custom
     * StaticCredentialsProvider+AwsBasicCredentials branch discarded any
     * AWS_SESSION_TOKEN whenever AWS_ACCESS_KEY_ID was set, breaking presigned
     * S3 PUTs for temporary/session credentials (aws login, SSO, assumed role).
     * DefaultCredentialsProvider resolves both long-lived and session
     * credentials correctly on its own - no custom branch should reappear here.
     */
    @Test
    void awsCredentialsProviderIsAlwaysTheSdkDefaultChain() {
        AwsCredentialsProvider provider = new AwsS3Config().awsCredentialsProvider();

        assertThat(provider).isInstanceOf(DefaultCredentialsProvider.class);
    }
}
