package com.brandPitara.sfs.repository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.brandPitara.sfs.entity.Otp;
import com.brandPitara.sfs.enums.OtpType;

public interface OtpRepository extends JpaRepository<Otp, Long> {
    Optional<Otp> findByCodeAndUser_IdAndTypeAndVerifiedIsFalseAndExpiresAtAfter(
        String code, Long userId, OtpType type, LocalDateTime now);
    
    List<Otp> findByUser_IdAndVerifiedIsFalseAndExpiresAtBefore(Long userId, LocalDateTime now);
}
