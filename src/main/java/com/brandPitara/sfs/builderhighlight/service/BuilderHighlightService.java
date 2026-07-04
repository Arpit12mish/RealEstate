package com.brandPitara.sfs.builderhighlight.service;

import com.brandPitara.sfs.builderhighlight.dto.request.BuilderHighlightItemRequest;
import com.brandPitara.sfs.builderhighlight.dto.request.BuilderHighlightStatusUpdateRequest;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightItemResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightPublicItemResponse;
import com.brandPitara.sfs.builderhighlight.dto.response.BuilderHighlightsScreenResponse;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightStatus;
import com.brandPitara.sfs.builderhighlight.enums.BuilderHighlightType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface BuilderHighlightService {

    BuilderHighlightsScreenResponse publicGetScreen(Long builderId);

    Page<BuilderHighlightPublicItemResponse> publicListItems(Long builderId, BuilderHighlightType type, Pageable pageable);

    BuilderHighlightPublicItemResponse publicGetItem(Long builderId, Long itemId);

    BuilderHighlightItemResponse create(Long builderId, BuilderHighlightItemRequest request);

    BuilderHighlightItemResponse update(Long builderId, Long itemId, BuilderHighlightItemRequest request);

    Page<BuilderHighlightItemResponse> dashboardList(
        Long builderId,
        BuilderHighlightType type,
        BuilderHighlightStatus status,
        Pageable pageable
    );

    BuilderHighlightItemResponse dashboardGet(Long builderId, Long itemId);

    void softDelete(Long builderId, Long itemId);

    BuilderHighlightItemResponse updateStatus(Long builderId, Long itemId, BuilderHighlightStatusUpdateRequest request);

    BuilderHighlightItemResponse setPublished(Long builderId, Long itemId, boolean value);
}
