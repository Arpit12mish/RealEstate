package com.brandPitara.sfs.buildercredibility;

import com.brandPitara.sfs.ratelimit.enums.RateLimitPolicy;
import com.brandPitara.sfs.ratelimit.resolver.RateLimitPolicyResolver;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Hardens GAP-030's security/rate-limit/no-external-call dimensions.
 * <p>
 * There is no existing MockMvc/WebMvcTest precedent anywhere in this test
 * suite for exercising the real Spring Security filter chain (SecurityConfig
 * wires 6 filter/handler beans, several of which are OncePerRequestFilters
 * that would need hand-rolled pass-through stubbing to avoid breaking the
 * chain under @MockBean). Rather than introduce that new, fragile test
 * category for a hardening-only phase, this class follows the same
 * "read the real file, don't hand-copy it" discipline already established by
 * BuilderSlugMigrationTest and RateLimitConfigLoadingTest: it reads the real,
 * tracked SecurityConfig/BuilderCredibilityServiceImpl source from the
 * classpath and asserts the exact rule text is present, so a future edit that
 * removes/narrows public access or introduces an external call is caught here
 * even without a live end-to-end HTTP round trip. RateLimitPolicyResolver, by
 * contrast, needs no such workaround - it is a plain, dependency-free class
 * and is exercised directly and for real below.
 */
class BuilderCredibilitySecurityAndRateLimitRegressionTest {

    @Test
    void builderCredibilityDetailRouteResolvesToThePublicBuilderReadRateLimitPolicy() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver();
        HttpServletRequest request = mockRequest("GET", "/api/builders/42/credibility");

        Optional<RateLimitPolicy> policy = resolver.resolve(request);

        assertThat(policy).contains(RateLimitPolicy.PUBLIC_BUILDER_READ);
    }

    @Test
    void builderCredibilityCardsRouteResolvesToThePublicBuilderReadRateLimitPolicy() {
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver();
        HttpServletRequest request = mockRequest("GET", "/api/builders/credibility/cards");

        Optional<RateLimitPolicy> policy = resolver.resolve(request);

        assertThat(policy).contains(RateLimitPolicy.PUBLIC_BUILDER_READ);
    }

    @Test
    void builderCredibilitySummaryRouteResolvesToThePublicBuilderReadRateLimitPolicy() {
        // publicGetCredibilitySummary is served by the same
        // BuilderCredibilityService but has no dedicated public controller
        // wired yet - documented here so a future controller addition is
        // proven to inherit rate-limit coverage from the same broad
        // "GET /api/builders/**" rule rather than needing a new policy entry.
        RateLimitPolicyResolver resolver = new RateLimitPolicyResolver();
        HttpServletRequest request = mockRequest("GET", "/api/builders/42/credibility/summary");

        Optional<RateLimitPolicy> policy = resolver.resolve(request);

        assertThat(policy).contains(RateLimitPolicy.PUBLIC_BUILDER_READ);
    }

    @Test
    void securityConfigStillPermitsAllGetRequestsToApiBuildersWithoutAuthentication() throws IOException {
        String source = readClasspathAdjacentSource(
            "com/brandPitara/sfs/config/SecurityConfig.java"
        );

        assertThat(source)
            .as("GET /api/builders/** must remain permitAll for both credibility endpoints to stay public")
            .contains(".requestMatchers(HttpMethod.GET, \"/api/builders/**\").permitAll()");
    }

    @Test
    void builderCredibilityServiceImplMakesNoExternalProviderOrAiCallDuringComputation() throws IOException {
        String source = readClasspathAdjacentSource(
            "com/brandPitara/sfs/buildercredibility/service/impl/BuilderCredibilityServiceImpl.java"
        );

        // Credibility is 100% derived from already-persisted project-meter
        // data at request time - it must never reach out to an HTTP client,
        // AI/LLM provider, or third-party SDK while serving a GET.
        assertThat(source)
            .as("BuilderCredibilityServiceImpl must stay a pure read/compute path with no outbound calls")
            .doesNotContainIgnoringCase("RestTemplate")
            .doesNotContainIgnoringCase("WebClient")
            .doesNotContainIgnoringCase("HttpClient")
            .doesNotContainIgnoringCase("openai")
            .doesNotContainIgnoringCase("anthropic")
            .doesNotContainIgnoringCase("twilio")
            .doesNotContainIgnoringCase("s3client")
            .doesNotContain("new URL(");
    }

    private HttpServletRequest mockRequest(String method, String uri) {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getMethod()).thenReturn(method);
        when(request.getRequestURI()).thenReturn(uri);
        when(request.getContextPath()).thenReturn("");
        return request;
    }

    private String readClasspathAdjacentSource(String relativeJavaPath) throws IOException {
        // Main sources aren't on the test classpath as .java files, but the
        // project layout is fixed (src/main/java mirrors src/test/java), so
        // the real tracked file is read directly from disk the same way
        // BuilderSlugMigrationTest reads the real migration SQL rather than
        // hand-copying it into the test.
        ClassPathResource marker = new ClassPathResource(".");
        java.io.File testClasses = marker.getFile();
        java.io.File projectRoot = testClasses.getParentFile().getParentFile();
        java.io.File sourceFile = new java.io.File(projectRoot, "src/main/java/" + relativeJavaPath);
        return Files.readString(sourceFile.toPath(), StandardCharsets.UTF_8);
    }
}
