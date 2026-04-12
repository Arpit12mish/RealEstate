package com.brandPitara.sfs.appcontent.controller;

import com.brandPitara.sfs.appcontent.dto.AppContentPageResponse;
import com.brandPitara.sfs.appcontent.dto.AppShareLinksResponse;
import com.brandPitara.sfs.appcontent.dto.ContactUsResponse;
import com.brandPitara.sfs.appcontent.dto.ProfileAppContentResponse;
import com.brandPitara.sfs.appcontent.service.AppContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app-content")
@RequiredArgsConstructor
public class AppContentController {

    private final AppContentService appContentService;

    @GetMapping("/pages/{slug}")
    public AppContentPageResponse getPage(@PathVariable String slug) {
        return appContentService.getPageBySlug(slug);
    }

    @GetMapping("/share-links")
    public AppShareLinksResponse getShareLinks() {
        return appContentService.getShareLinks();
    }

    @GetMapping("/contact-us")
    public ContactUsResponse getContactUs() {
        return appContentService.getContactUs();
    }

    @GetMapping("/profile")
    public ProfileAppContentResponse getProfileAppContent() {
        return appContentService.getProfileAppContent();
    }
}