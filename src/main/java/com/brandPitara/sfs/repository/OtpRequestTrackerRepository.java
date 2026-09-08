package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.OtpRequestTracker;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OtpRequestTrackerRepository extends JpaRepository<OtpRequestTracker, Long> {
    Optional<OtpRequestTracker> findByPhoneNumber(String phoneNumber);

    /**
     * Locks the tracker row for the duration of the caller's transaction so concurrent
     * verify attempts for the same phone number serialize instead of losing updates to
     * failedVerifyCountInWindow (see TwilioOtpServiceImpl#verifyOtp).
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM OtpRequestTracker t WHERE t.phoneNumber = :phoneNumber")
    Optional<OtpRequestTracker> findByPhoneNumberForUpdate(@Param("phoneNumber") String phoneNumber);

    /**
     * Atomically creates the row used as the per-phone lock. PostgreSQL serializes
     * concurrent inserts on the unique phone key, avoiding a check-then-insert race.
     */
    @Modifying
    @Query(value = """
            INSERT INTO otp_request_tracker (
                phone_number, send_count_in_window, failed_verify_count_in_window
            ) VALUES (:phoneNumber, 0, 0)
            ON CONFLICT (phone_number) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("phoneNumber") String phoneNumber);
}
