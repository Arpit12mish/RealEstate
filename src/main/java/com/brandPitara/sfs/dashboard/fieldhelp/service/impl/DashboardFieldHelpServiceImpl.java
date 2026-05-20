package com.brandPitara.sfs.dashboard.fieldhelp.service.impl;

import com.brandPitara.sfs.dashboard.auth.service.DashboardCurrentUserService;
import com.brandPitara.sfs.dashboard.common.enums.DashboardHelpModule;
import com.brandPitara.sfs.dashboard.fieldhelp.dto.DashboardFieldHelpResponse;
import com.brandPitara.sfs.dashboard.fieldhelp.dto.DashboardFieldHelpUpsertRequest;
import com.brandPitara.sfs.dashboard.fieldhelp.entity.DashboardFieldHelpEntity;
import com.brandPitara.sfs.dashboard.fieldhelp.repository.DashboardFieldHelpRepository;
import com.brandPitara.sfs.dashboard.fieldhelp.service.DashboardFieldHelpService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardFieldHelpServiceImpl implements DashboardFieldHelpService {

    private final DashboardFieldHelpRepository repository;
    private final DashboardCurrentUserService currentUserService;

    @Override
    @Transactional(readOnly = true)
    public List<DashboardFieldHelpResponse> listByModule(DashboardHelpModule module) {
        return repository.findByModuleAndActiveTrueOrderByDisplayOrderAscIdAsc(module)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardFieldHelpResponse getOne(DashboardHelpModule module, String fieldKey) {
        return repository.findByModuleAndFieldKey(module, fieldKey)
                .map(this::toResponse)
                .orElseThrow(() -> new EntityNotFoundException(
                        "Field help not found: " + module + "/" + fieldKey));
    }

    @Override
    @Transactional
    public DashboardFieldHelpResponse upsert(DashboardFieldHelpUpsertRequest request) {
        Long actorId = currentUserService.getCurrentUserOrThrow().getId();

        DashboardFieldHelpEntity entity = repository
                .findByModuleAndFieldKey(request.getModule(), request.getFieldKey())
                .orElseGet(() -> {
                    DashboardFieldHelpEntity e = new DashboardFieldHelpEntity();
                    e.setModule(request.getModule());
                    e.setFieldKey(request.getFieldKey());
                    e.setCreatedByDashboardUserId(actorId);
                    return e;
                });

        entity.setFieldLabel(request.getFieldLabel().trim());
        entity.setShortHelp(request.getShortHelp().trim());
        entity.setDetailedHelp(clean(request.getDetailedHelp()));
        entity.setWhyNeeded(clean(request.getWhyNeeded()));
        entity.setSourceHint(clean(request.getSourceHint()));
        entity.setExampleValue(clean(request.getExampleValue()));
        entity.setValidationHint(clean(request.getValidationHint()));
        entity.setUpdatedByDashboardUserId(actorId);

        if (request.getActive() != null) entity.setActive(request.getActive());
        if (request.getDisplayOrder() != null) entity.setDisplayOrder(request.getDisplayOrder());

        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void setActive(Long id, boolean active) {
        DashboardFieldHelpEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Field help not found: " + id));
        entity.setActive(active);
        entity.setUpdatedByDashboardUserId(currentUserService.getCurrentUserOrThrow().getId());
        repository.save(entity);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        DashboardFieldHelpEntity entity = repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Field help not found: " + id));
        entity.setActive(false);
        entity.setUpdatedByDashboardUserId(currentUserService.getCurrentUserOrThrow().getId());
        repository.save(entity);
    }

    private DashboardFieldHelpResponse toResponse(DashboardFieldHelpEntity e) {
        return DashboardFieldHelpResponse.builder()
                .id(e.getId())
                .module(e.getModule())
                .fieldKey(e.getFieldKey())
                .fieldLabel(e.getFieldLabel())
                .shortHelp(e.getShortHelp())
                .detailedHelp(e.getDetailedHelp())
                .whyNeeded(e.getWhyNeeded())
                .sourceHint(e.getSourceHint())
                .exampleValue(e.getExampleValue())
                .validationHint(e.getValidationHint())
                .active(e.getActive())
                .displayOrder(e.getDisplayOrder())
                .build();
    }

    private String clean(String s) {
        return StringUtils.hasText(s) ? s.trim() : null;
    }
}
