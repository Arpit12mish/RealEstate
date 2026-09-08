package com.brandPitara.sfs.config;

import jakarta.servlet.DispatcherType;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityRequestBypassTest {

    @Test
    void skipsOptionsHealthErrorAndAsyncDispatches() {
        assertThat(SecurityRequestBypass.shouldBypass(request("OPTIONS", "/api/home", DispatcherType.REQUEST))).isTrue();
        assertThat(SecurityRequestBypass.shouldBypass(request("GET", "/api/health/readiness", DispatcherType.REQUEST))).isTrue();
        assertThat(SecurityRequestBypass.shouldBypass(request("GET", "/actuator/health/liveness", DispatcherType.REQUEST))).isTrue();
        assertThat(SecurityRequestBypass.shouldBypass(request("GET", "/error", DispatcherType.ERROR))).isTrue();
        assertThat(SecurityRequestBypass.shouldBypass(request("GET", "/api/home", DispatcherType.ASYNC))).isTrue();
        assertThat(SecurityRequestBypass.shouldBypass(request("GET", "/api/home", DispatcherType.REQUEST))).isFalse();
    }

    private MockHttpServletRequest request(String method, String path, DispatcherType dispatcherType) {
        MockHttpServletRequest request = new MockHttpServletRequest(method, path);
        request.setDispatcherType(dispatcherType);
        return request;
    }
}
