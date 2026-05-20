package com.brandPitara.sfs.dashboard.fieldhelp.service;

import com.brandPitara.sfs.dashboard.common.enums.DashboardHelpModule;
import com.brandPitara.sfs.dashboard.fieldhelp.dto.DashboardFieldHelpResponse;
import com.brandPitara.sfs.dashboard.fieldhelp.dto.DashboardFieldHelpUpsertRequest;

import java.util.List;

public interface DashboardFieldHelpService {

    List<DashboardFieldHelpResponse> listByModule(DashboardHelpModule module);

    DashboardFieldHelpResponse getOne(DashboardHelpModule module, String fieldKey);

    DashboardFieldHelpResponse upsert(DashboardFieldHelpUpsertRequest request);

    void setActive(Long id, boolean active);

    void delete(Long id);
}
