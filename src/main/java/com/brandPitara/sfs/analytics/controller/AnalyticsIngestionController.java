package com.brandPitara.sfs.analytics.controller;

import com.brandPitara.sfs.analytics.dto.AnalyticsEventBatchRequest;
import com.brandPitara.sfs.analytics.dto.AnalyticsIngestResponse;
import com.brandPitara.sfs.analytics.service.AnalyticsIngestionService;
import com.brandPitara.sfs.ratelimit.identity.RateLimitAuthenticationAttributes;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public, permitAll batch ingestion endpoint (see SecurityConfig) - reachable with or
 * without a token so a guest's very first app-open events aren't dropped waiting on
 * guest-session bootstrap. When a valid token IS present, JwtRequestFilter still runs
 * (it isn't in the security-bypass list) and populates the userId request attribute
 * used below, so authenticated users still get attributed activity.
 */
@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
public class AnalyticsIngestionController {

    private final AnalyticsIngestionService ingestionService;

    @PostMapping("/events/batch")
    public ResponseEntity<AnalyticsIngestResponse> ingest(
            @Valid @RequestBody AnalyticsEventBatchRequest request,
            HttpServletRequest httpRequest
    ) {
        Long userId = (Long) httpRequest.getAttribute(RateLimitAuthenticationAttributes.USER_ID);
        AnalyticsIngestResponse response = ingestionService.ingest(request, userId);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }
}
