package com.brandPitara.sfs.service;

import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.dto.PageResponse;

public interface FavoriteService {

    void addFavorite(Long businessId);

    void removeFavorite(Long businessId);

    boolean isFavorite(Long businessId);

    PageResponse<BusinessResponse> listFavorites(int page, int size);

    void toggleFavorite(Long businessId);

    long getFavoriteCount(Long businessId);


}
