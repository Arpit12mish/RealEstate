package com.brandPitara.sfs.dashboard.scraping.util;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ScrapeCaptchaDetector {

    private static final List<String> BLOCKING_SIGNALS = List.of(
            "captcha",
            "g-recaptcha",
            "hcaptcha",
            "verify you are human",
            "are you a robot",
            "cloudflare",
            "ddos-guard",
            "access denied",
            "blocked",
            "bot detection",
            "unusual traffic",
            "security check"
    );

    public boolean detect(String html) {
        if (html == null || html.isBlank()) return false;
        String lower = html.toLowerCase();
        return BLOCKING_SIGNALS.stream().anyMatch(lower::contains);
    }
}
