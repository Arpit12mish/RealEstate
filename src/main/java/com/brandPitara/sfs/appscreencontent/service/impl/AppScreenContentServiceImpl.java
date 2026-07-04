package com.brandPitara.sfs.appscreencontent.service.impl;

import com.brandPitara.sfs.appscreencontent.dto.AppScreenContentRequest;
import com.brandPitara.sfs.appscreencontent.dto.AppScreenContentResponse;
import com.brandPitara.sfs.appscreencontent.entity.AppScreenContentEntity;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenKey;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenMediaType;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenPlacement;
import com.brandPitara.sfs.appscreencontent.repository.AppScreenContentRepository;
import com.brandPitara.sfs.appscreencontent.service.AppScreenContentService;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppScreenContentServiceImpl implements AppScreenContentService {

    private static final String DEFAULT_BACKGROUND_COLOR = "#000000";

    private final AppScreenContentRepository repository;

    @Override
    public AppScreenContentResponse create(AppScreenContentRequest request) {
        validate(request);

        AppScreenContentEntity entity = AppScreenContentEntity.builder()
                .screenKey(request.screenKey())
                .placement(request.placement())
                .mediaType(request.mediaType())
                .mediaUrl(request.mediaUrl().trim())
                .enabled(request.enabled() == null || request.enabled())
                .backgroundColor(defaultString(request.backgroundColor(), DEFAULT_BACKGROUND_COLOR))
                .aspectRatio(request.aspectRatio())
                .startAt(request.startAt())
                .endAt(request.endAt())
                .minAppVersion(blankToNull(request.minAppVersion()))
                .sortOrder(request.sortOrder() == null ? 0 : request.sortOrder())
                .build();

        return AppScreenContentResponse.from(repository.save(entity));
    }

    @Override
    public AppScreenContentResponse update(Long id, AppScreenContentRequest request) {
        validate(request);

        AppScreenContentEntity entity = findById(id);
        entity.setScreenKey(request.screenKey());
        entity.setPlacement(request.placement());
        entity.setMediaType(request.mediaType());
        entity.setMediaUrl(request.mediaUrl().trim());
        entity.setEnabled(request.enabled() == null || request.enabled());
        entity.setBackgroundColor(defaultString(request.backgroundColor(), DEFAULT_BACKGROUND_COLOR));
        entity.setAspectRatio(request.aspectRatio());
        entity.setStartAt(request.startAt());
        entity.setEndAt(request.endAt());
        entity.setMinAppVersion(blankToNull(request.minAppVersion()));
        entity.setSortOrder(request.sortOrder() == null ? 0 : request.sortOrder());

        return AppScreenContentResponse.from(repository.save(entity));
    }

    @Override
    public AppScreenContentResponse setEnabled(Long id, boolean enabled) {
        AppScreenContentEntity entity = findById(id);
        entity.setEnabled(enabled);
        return AppScreenContentResponse.from(repository.save(entity));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AppScreenContentResponse> list(AppScreenKey screenKey, AppScreenPlacement placement) {
        List<AppScreenContentEntity> entities = placement == null
                ? repository.findByScreenKeyOrderByPlacementAscSortOrderAscIdDesc(screenKey)
                : repository.findByScreenKeyAndPlacementOrderBySortOrderAscIdDesc(screenKey, placement);

        return entities.stream()
                .map(AppScreenContentResponse::from)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AppScreenContentResponse> getActive(AppScreenKey screenKey, AppScreenPlacement placement) {
        return repository.findActiveForPlacement(screenKey, placement, OffsetDateTime.now())
                .stream()
                .findFirst()
                .map(AppScreenContentResponse::from);
    }

    private AppScreenContentEntity findById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("App screen content not found: " + id));
    }

    private void validate(AppScreenContentRequest request) {
        if (request.endAt() != null && request.startAt() != null && request.endAt().isBefore(request.startAt())) {
            throw new IllegalArgumentException("endAt cannot be before startAt");
        }

        String mediaUrl = request.mediaUrl() == null ? "" : request.mediaUrl().trim().toLowerCase();
        if (request.mediaType() == AppScreenMediaType.LOTTIE_JSON && !mediaUrl.matches(".*\\.json(\\?.*)?$")) {
            throw new IllegalArgumentException("LOTTIE_JSON mediaUrl must point to a .json file");
        }
        if (request.mediaType() == AppScreenMediaType.VIDEO && !mediaUrl.matches(".*\\.(mp4|mov|m4v|webm)(\\?.*)?$")) {
            throw new IllegalArgumentException("VIDEO mediaUrl must point to a supported video file");
        }
    }

    private String defaultString(String value, String defaultValue) {
        String cleaned = blankToNull(value);
        return cleaned == null ? defaultValue : cleaned;
    }

    private String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
