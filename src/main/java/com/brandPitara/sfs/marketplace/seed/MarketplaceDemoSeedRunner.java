package com.brandPitara.sfs.marketplace.seed;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.sql.DataSource;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Loads the Marketplace demo stores and Allied Services workers ({@code marketplace/demo-seed.sql})
 * once, on startup, after Flyway. Enabled by {@code sfs.marketplace.demo-seed.enabled} (production
 * default true, off everywhere else).
 *
 * <p>Stores are created once and never re-created or overwritten, so restarts never duplicate or
 * reset the data and dashboard edits survive. Later starts only fill in what is still missing: the
 * contact number on demo stores/workers that have none, and demo workers not created yet (e.g. once
 * their login numbers are configured). It runs in one transaction; a failure is logged and rolled
 * back and never stops the application.
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sfs.marketplace.demo-seed.enabled", havingValue = "true")
public class MarketplaceDemoSeedRunner implements ApplicationRunner {

    static final String SCRIPT = "marketplace/demo-seed.sql";

    /** Demo workers in the order their login numbers are listed. */
    static final List<String> WORKERS = List.of("Manish Singh", "Rakesh Kumar", "Anil Verma", "Suresh Pal");

    private final DataSource dataSource;
    private final String contactPhone;
    private final String workerLoginPhones;

    public MarketplaceDemoSeedRunner(
            DataSource dataSource,
            @Value("${sfs.marketplace.demo-seed.contact-phone:}") String contactPhone,
            @Value("${sfs.marketplace.demo-seed.worker-login-phones:}") String workerLoginPhones) {
        this.dataSource = dataSource;
        this.contactPhone = contactPhone;
        this.workerLoginPhones = workerLoginPhones;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Result result = seed();
            if (result.inserted() == 0 && result.contactsUpdated() == 0) {
                log.info("Marketplace demo seed: nothing to do (already loaded, or New Delhi reference data missing)");
            } else {
                log.info("Marketplace demo seed: loaded {} demo stores/workers, added the contact number to {} existing ones (contact number {})",
                        result.inserted(), result.contactsUpdated(),
                        result.contactConfigured() ? "configured" : "not set - Call/WhatsApp hidden");
            }
        } catch (Exception e) {
            log.error("Marketplace demo seed failed and was rolled back; the application continues", e);
        }
    }

    /** Runs the script in a single transaction and reports how many demo stores/workers it created or decorated. */
    public Result seed() throws SQLException {
        String phone = normalizePhone(contactPhone);
        if (StringUtils.hasText(contactPhone) && phone == null) {
            log.warn("Marketplace demo seed: SFS_DEMO_CONTACT_PHONE is not a valid phone number; contact buttons will be hidden");
        }
        List<String> loginPhones = workerLoginPhones(workerLoginPhones);
        for (int i = 0; i < WORKERS.size(); i++) {
            if (loginPhones.get(i) == null) {
                log.warn("Marketplace demo seed: no usable login number for demo worker {} (SFS_DEMO_WORKER_LOGIN_PHONES #{}); skipping that worker",
                        WORKERS.get(i), i + 1);
            }
        }
        try (Connection connection = dataSource.getConnection()) {
            boolean autoCommit = connection.getAutoCommit();
            connection.setAutoCommit(false);
            try {
                try (Statement st = connection.createStatement()) {
                    st.execute("CREATE TEMP TABLE demo_cfg (contact_phone text) ON COMMIT DROP");
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO demo_cfg (contact_phone) VALUES (?)")) {
                    ps.setString(1, phone);
                    ps.executeUpdate();
                }
                try (Statement st = connection.createStatement()) {
                    st.execute("CREATE TEMP TABLE demo_worker_phone (ord int PRIMARY KEY, phone text NOT NULL) ON COMMIT DROP");
                }
                try (PreparedStatement ps = connection.prepareStatement("INSERT INTO demo_worker_phone (ord, phone) VALUES (?, ?)")) {
                    for (int i = 0; i < loginPhones.size(); i++) {
                        if (loginPhones.get(i) == null) continue;
                        ps.setInt(1, i + 1);
                        ps.setString(2, loginPhones.get(i));
                        ps.addBatch();
                    }
                    ps.executeBatch();
                }
                ScriptUtils.executeSqlScript(connection,
                        new EncodedResource(new ClassPathResource(SCRIPT), StandardCharsets.UTF_8));
                int inserted;
                int contactsUpdated;
                try (Statement st = connection.createStatement();
                     ResultSet rs = st.executeQuery("SELECT (SELECT count(*) FROM fx), (SELECT n FROM seed_contacts)")) {
                    rs.next();
                    inserted = rs.getInt(1);
                    contactsUpdated = rs.getInt(2);
                }
                connection.commit();
                return new Result(inserted, contactsUpdated, phone != null);
            } catch (SQLException | RuntimeException e) {
                connection.rollback();
                throw e;
            } finally {
                connection.setAutoCommit(autoCommit);
            }
        }
    }

    /**
     * Accepts "8790012345", "+91 87900 12345", "08790012345" or any "+&lt;country&gt;&lt;number&gt;";
     * returns E.164 ("+918790012345") or null when blank or unrecognisable.
     */
    static String normalizePhone(String raw) {
        if (!StringUtils.hasText(raw)) return null;
        String trimmed = raw.trim();
        String digits = trimmed.replaceAll("\\D", "");
        if (trimmed.startsWith("+")) {
            return digits.length() >= 8 && digits.length() <= 15 && digits.charAt(0) != '0' ? "+" + digits : null;
        }
        if (digits.length() == 11 && digits.startsWith("0")) digits = digits.substring(1);
        if (digits.length() == 12 && digits.startsWith("91")) digits = digits.substring(2);
        return digits.length() == 10 && digits.charAt(0) >= '6' ? "+91" + digits : null;
    }

    /**
     * Comma-separated login numbers, one per entry of {@link #WORKERS}. Each must be an Indian mobile
     * (the users table only accepts +91[6-9]xxxxxxxxx); invalid, missing or repeated entries become null.
     */
    static List<String> workerLoginPhones(String raw) {
        String[] parts = StringUtils.hasText(raw) ? raw.split(",") : new String[0];
        Set<String> seen = new LinkedHashSet<>();
        List<String> out = new ArrayList<>();
        for (int i = 0; i < WORKERS.size(); i++) {
            String phone = i < parts.length ? normalizePhone(parts[i]) : null;
            out.add(phone != null && phone.matches("\\+91[6-9]\\d{9}") && seen.add(phone) ? phone : null);
        }
        return out;
    }

    public record Result(int inserted, int contactsUpdated, boolean contactConfigured) {
    }
}
