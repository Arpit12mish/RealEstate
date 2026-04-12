package com.brandPitara.sfs.appcontent.repository;

import com.brandPitara.sfs.appcontent.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
    Optional<AppSetting> findBySettingKeyAndActiveTrue(String settingKey);
    List<AppSetting> findBySettingKeyInAndActiveTrue(List<String> keys);
}