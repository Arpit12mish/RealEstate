package com.brandPitara.sfs.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.brandPitara.sfs.entity.User;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    Boolean existsByPhoneNumber(String phoneNumber);
}
