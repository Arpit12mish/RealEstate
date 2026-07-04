package com.brandPitara.sfs.appscreencontent.service;

import com.brandPitara.sfs.appscreencontent.dto.AppScreenContentRequest;
import com.brandPitara.sfs.appscreencontent.dto.AppScreenContentResponse;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenKey;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenPlacement;

import java.util.List;
import java.util.Optional;

public interface AppScreenContentService {
    AppScreenContentResponse create(AppScreenContentRequest request);
    AppScreenContentResponse update(Long id, AppScreenContentRequest request);
    AppScreenContentResponse setEnabled(Long id, boolean enabled);
    List<AppScreenContentResponse> list(AppScreenKey screenKey, AppScreenPlacement placement);
    Optional<AppScreenContentResponse> getActive(AppScreenKey screenKey, AppScreenPlacement placement);
}
