package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.OtpRequestTracker;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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
}
