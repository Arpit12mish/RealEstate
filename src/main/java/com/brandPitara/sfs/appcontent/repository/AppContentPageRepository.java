package com.brandPitara.sfs.appcontent.repository;

import com.brandPitara.sfs.appcontent.entity.AppContentPage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AppContentPageRepository extends JpaRepository<AppContentPage, Long> {
    Optional<AppContentPage> findBySlugAndActiveTrue(String slug);
}