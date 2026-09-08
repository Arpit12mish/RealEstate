package com.brandPitara.sfs.repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.security.identity.MobileAuthenticationUserSnapshot;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    Optional<User> findByPhoneNumber(String phoneNumber);
    List<User> findByPhoneNumberIn(Collection<String> phoneNumbers);
    Boolean existsByPhoneNumber(String phoneNumber);

    @Query("""
            select new com.brandPitara.sfs.security.identity.MobileAuthenticationUserSnapshot(
                u.id, u.phoneNumber, u.role, u.isVerified
            )
            from User u
            where u.id = :userId
            """)
    Optional<MobileAuthenticationUserSnapshot> findAuthenticationSnapshotById(@Param("userId") Long userId);
}
