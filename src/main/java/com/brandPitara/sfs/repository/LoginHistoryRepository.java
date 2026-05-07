package com.brandPitara.sfs.repository;

import com.brandPitara.sfs.entity.LoginHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface LoginHistoryRepository extends JpaRepository<LoginHistory, Long> {

    @Modifying
    @Query("UPDATE LoginHistory lh SET lh.user = null WHERE lh.user.id = :userId")
    void nullifyUserById(Long userId);
}
