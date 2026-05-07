package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.OtpRequestTracker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRequestTrackerRepository extends JpaRepository<OtpRequestTracker, Long> {
    Optional<OtpRequestTracker> findByPhoneNumber(String phoneNumber);
}