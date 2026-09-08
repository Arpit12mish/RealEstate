package com.brandPitara.sfs.analytics.ingestion;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ParameterizedPreparedStatementSetter;

import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Production-hardening pass (Part C): proves the idempotency SQL contract
 * (ON CONFLICT (event_id, occurred_at) DO NOTHING, see V162) is actually issued, and
 * that insertBatch() reports the true inserted-row count rather than the raw batch
 * size once duplicates are silently skipped by the DB.
 */
class AnalyticsEventJdbcRepositoryTest {

    private AnalyticsEventRecord sampleRecord() {
        Instant now = Instant.now();
        return new AnalyticsEventRecord(
                UUID.randomUUID(), now, now, (short) 1, "SEARCH_SUBMITTED",
                UUID.randomUUID(), null, UUID.randomUUID(), "SEARCH", null, null,
                null, null, null, "ANDROID", "1.0.0", null
        );
    }

    @SuppressWarnings("unchecked")
    private JdbcTemplate mockJdbcTemplateReturning(int[][] batchResult) {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.batchUpdate(anyString(), anyList(), anyInt(), any(ParameterizedPreparedStatementSetter.class)))
                .thenReturn(batchResult);
        return jdbcTemplate;
    }

    @Test
    void insertBatchReturnsActualInsertedCountWhenSomeRowsAreSkippedAsDuplicates() {
        // 3 rows attempted: 2 inserted (1), 1 skipped by ON CONFLICT DO NOTHING (0)
        JdbcTemplate jdbcTemplate = mockJdbcTemplateReturning(new int[][]{{1, 0, 1}});
        AnalyticsEventJdbcRepository repository = new AnalyticsEventJdbcRepository(jdbcTemplate);

        int inserted = repository.insertBatch(List.of(sampleRecord(), sampleRecord(), sampleRecord()));

        assertThat(inserted).isEqualTo(2);
    }

    @Test
    void insertBatchOfAllDuplicatesReportsZeroInserted() {
        JdbcTemplate jdbcTemplate = mockJdbcTemplateReturning(new int[][]{{0, 0}});
        AnalyticsEventJdbcRepository repository = new AnalyticsEventJdbcRepository(jdbcTemplate);

        int inserted = repository.insertBatch(List.of(sampleRecord(), sampleRecord()));

        assertThat(inserted).isZero();
    }

    @Test
    void insertBatchFallsBackToBatchSizeWhenDriverReportsSuccessNoInfo() {
        JdbcTemplate jdbcTemplate = mockJdbcTemplateReturning(new int[][]{{1, Statement.SUCCESS_NO_INFO}});
        AnalyticsEventJdbcRepository repository = new AnalyticsEventJdbcRepository(jdbcTemplate);

        int inserted = repository.insertBatch(List.of(sampleRecord(), sampleRecord()));

        assertThat(inserted).isEqualTo(2);
    }

    @Test
    void insertBatchOfEmptyListNeverCallsJdbc() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        AnalyticsEventJdbcRepository repository = new AnalyticsEventJdbcRepository(jdbcTemplate);

        int inserted = repository.insertBatch(List.of());

        assertThat(inserted).isZero();
        verify(jdbcTemplate, never()).batchUpdate(anyString(), anyList(), anyInt(), any(ParameterizedPreparedStatementSetter.class));
    }

    @Test
    void insertSqlDeclaresOnConflictDoNothingOnEventIdAndOccurredAt() {
        JdbcTemplate jdbcTemplate = mockJdbcTemplateReturning(new int[][]{{1}});
        AnalyticsEventJdbcRepository repository = new AnalyticsEventJdbcRepository(jdbcTemplate);
        ArgumentCaptor<String> sqlCaptor = ArgumentCaptor.forClass(String.class);

        repository.insertBatch(List.of(sampleRecord()));

        verify(jdbcTemplate).batchUpdate(sqlCaptor.capture(), anyList(), anyInt(), any(ParameterizedPreparedStatementSetter.class));
        assertThat(sqlCaptor.getValue()).contains("ON CONFLICT (event_id, occurred_at) DO NOTHING");
    }
}
