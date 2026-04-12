package com.brandPitara.sfs.appcontent.service;

import com.brandPitara.sfs.appcontent.dto.AppContentPageResponse;
import com.brandPitara.sfs.appcontent.dto.AppShareLinksResponse;
import com.brandPitara.sfs.appcontent.dto.ContactUsResponse;
import com.brandPitara.sfs.appcontent.dto.ProfileAppContentResponse;

public interface AppContentService {
    AppContentPageResponse getPageBySlug(String slug);
    AppShareLinksResponse getShareLinks();
    ContactUsResponse getContactUs();
    ProfileAppContentResponse getProfileAppContent();
}