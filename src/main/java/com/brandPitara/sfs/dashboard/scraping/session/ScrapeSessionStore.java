package com.brandPitara.sfs.dashboard.scraping.session;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory store for short-lived Playwright scrape sessions.
 * Sessions are created when a CAPTCHA is detected and kept alive
 * until the admin submits the captcha or the TTL expires.
 *
 * A scheduled job evicts and closes expired sessions every 60 seconds.
 */
@Slf4j
@Component
public class ScrapeSessionStore {

    private final ConcurrentHashMap<String, ActiveScrapeSession> sessions = new ConcurrentHashMap<>();

    public void put(ActiveScrapeSession session) {
        sessions.put(session.getSessionId(), session);
        log.info("Scrape session created: sessionId={} reraNumber={} expires={}",
                session.getSessionId(), session.getReraNumber(), session.getExpiresAt());
    }

    public Optional<ActiveScrapeSession> get(String sessionId) {
        if (sessionId == null) return Optional.empty();
        ActiveScrapeSession session = sessions.get(sessionId);
        if (session == null) return Optional.empty();
        if (OffsetDateTime.now().isAfter(session.getExpiresAt())) {
            remove(sessionId);
            log.info("Scrape session expired on access: sessionId={}", sessionId);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    /** Removes the session from the store and closes its Playwright resources. */
    public void remove(String sessionId) {
        ActiveScrapeSession removed = sessions.remove(sessionId);
        if (removed != null) {
            removed.close();
            log.info("Scrape session removed: sessionId={}", sessionId);
        }
    }

    public void updateCandidateId(String sessionId, Long candidateId) {
        ActiveScrapeSession session = sessions.get(sessionId);
        if (session != null) {
            session.setCandidateId(candidateId);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    public void cleanupExpired() {
        OffsetDateTime now = OffsetDateTime.now();
        sessions.entrySet().removeIf(entry -> {
            if (now.isAfter(entry.getValue().getExpiresAt())) {
                entry.getValue().close();
                log.info("Expired scrape session evicted: sessionId={} candidateId={}",
                        entry.getKey(), entry.getValue().getCandidateId());
                return true;
            }
            return false;
        });
    }
}
