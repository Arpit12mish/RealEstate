package com.brandPitara.sfs.cdn;

import com.brandPitara.sfs.cdn.config.CdnProperties;
import com.brandPitara.sfs.cdn.gateway.CdnInvalidationException;
import com.brandPitara.sfs.cdn.gateway.CloudFrontCdnInvalidationGateway;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.services.cloudfront.CloudFrontClient;
import software.amazon.awssdk.services.cloudfront.model.CloudFrontException;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationRequest;
import software.amazon.awssdk.services.cloudfront.model.CreateInvalidationResponse;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CloudFrontCdnInvalidationGatewayTest {

    @Test
    void sendsExactPathsAndUniqueCallerReferences() {
        CloudFrontClient client = mock(CloudFrontClient.class);
        when(client.createInvalidation(any(CreateInvalidationRequest.class)))
                .thenReturn(CreateInvalidationResponse.builder().build());
        CloudFrontCdnInvalidationGateway gateway = gateway(client);

        gateway.invalidate(List.of("/api/v2/public/projects/27"));
        gateway.invalidate(List.of("/api/v2/public/projects/27"));

        ArgumentCaptor<CreateInvalidationRequest> requests =
                ArgumentCaptor.forClass(CreateInvalidationRequest.class);
        verify(client, times(2)).createInvalidation(requests.capture());
        assertThat(requests.getAllValues()).allSatisfy(request -> {
            assertThat(request.distributionId()).isEqualTo("DIST-123");
            assertThat(request.invalidationBatch().paths().items())
                    .containsExactly("/api/v2/public/projects/27");
        });
        assertThat(requests.getAllValues().get(0).invalidationBatch().callerReference())
                .isNotEqualTo(requests.getAllValues().get(1).invalidationBatch().callerReference());
    }

    @Test
    void retriesRetryableFailuresAtMostThreeTimes() {
        CloudFrontClient client = mock(CloudFrontClient.class);
        CloudFrontException retryable = mock(CloudFrontException.class);
        when(retryable.statusCode()).thenReturn(503);
        when(client.createInvalidation(any(CreateInvalidationRequest.class)))
                .thenThrow(retryable)
                .thenThrow(retryable)
                .thenReturn(CreateInvalidationResponse.builder().build());

        gateway(client).invalidate(List.of("/api/v2/public/projects/27"));

        verify(client, times(3)).createInvalidation(any(CreateInvalidationRequest.class));
    }

    @Test
    void doesNotRetryPermissionOrConfigurationFailures() {
        CloudFrontClient client = mock(CloudFrontClient.class);
        CloudFrontException denied = mock(CloudFrontException.class);
        when(denied.statusCode()).thenReturn(403);
        when(client.createInvalidation(any(CreateInvalidationRequest.class))).thenThrow(denied);

        assertThatThrownBy(() -> gateway(client).invalidate(List.of("/api/v2/public/projects/27")))
                .isInstanceOf(CdnInvalidationException.class);
        verify(client).createInvalidation(any(CreateInvalidationRequest.class));
    }

    @Test
    void rejectsWildcardPathsBeforeAwsCall() {
        CloudFrontClient client = mock(CloudFrontClient.class);
        assertThatThrownBy(() -> gateway(client).invalidate(List.of("/api/v2/public/projects/*")))
                .isInstanceOf(IllegalArgumentException.class);
        verifyNoInteractions(client);
    }

    private CloudFrontCdnInvalidationGateway gateway(CloudFrontClient client) {
        CdnProperties properties = new CdnProperties();
        properties.setEnabled(true);
        properties.setDistributionId("DIST-123");
        return new CloudFrontCdnInvalidationGateway(client, properties);
    }
}
