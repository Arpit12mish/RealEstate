package com.brandPitara.sfs.dashboard.fieldhelp.repository;

import com.brandPitara.sfs.dashboard.common.enums.DashboardHelpModule;
import com.brandPitara.sfs.dashboard.fieldhelp.entity.DashboardFieldHelpEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DashboardFieldHelpRepository extends JpaRepository<DashboardFieldHelpEntity, Long> {

    List<DashboardFieldHelpEntity> findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(DashboardHelpModule module);

    Optional<DashboardFieldHelpEntity> findByModuleAndFieldKey(DashboardHelpModule module, String fieldKey);

    boolean existsByModuleAndFieldKey(DashboardHelpModule module, String fieldKey);
}
