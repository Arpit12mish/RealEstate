package com.brandPitara.sfs.provider.controller;

import com.brandPitara.sfs.provider.dto.PresignProviderMediaRequest;
import com.brandPitara.sfs.provider.dto.PresignProviderMediaResponse;
import com.brandPitara.sfs.provider.dto.ProviderMediaListResponse;
import com.brandPitara.sfs.provider.dto.ProviderMediaResponse;
import com.brandPitara.sfs.provider.service.ProviderMediaPresignService;
import com.brandPitara.sfs.provider.service.ProviderMediaService;
import com.brandPitara.sfs.security.CurrentUserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/providers/me/media")
@RequiredArgsConstructor
public class ProviderMediaController {

    private final ProviderMediaService providerMediaService;
    private final CurrentUserService currentUserService;
    private final ProviderMediaPresignService providerMediaPresignService;

    /**
     * Client flow:
     * 1) Call /presign -> returns uploadUrl + publicUrl + storageKey
     * 2) Upload file to uploadUrl
     * 3) Call /profile-photo or /gallery with (url=publicUrl, storageKey)
     */
    public record UploadMediaRequest(
            @NotBlank String url,
            String thumbnailUrl,
            @NotBlank String storageKey,
            @Min(0) Integer sortOrder
    ) {}

    @GetMapping
    @PreAuthorize("hasAnyRole('WORKER','BRAND')")
    public ProviderMediaListResponse getMyMedia() {
        return providerMediaService.getMyMedia(currentUserService.requireUserId());
    }

    @PostMapping("/profile-photo")
    @PreAuthorize("hasAnyRole('WORKER','BRAND')")
    public ProviderMediaResponse upsertProfilePhoto(@Valid @RequestBody UploadMediaRequest req) {
        return providerMediaService.upsertMyProfilePhoto(
                currentUserService.requireUserId(),
                req.url(),
                req.thumbnailUrl(),
                req.storageKey()
        );
    }

    @PostMapping("/gallery")
    @PreAuthorize("hasAnyRole('WORKER','BRAND')")
    public ProviderMediaResponse addGallery(@Valid @RequestBody UploadMediaRequest req) {
        return providerMediaService.addMyGalleryImage(
                currentUserService.requireUserId(),
                req.url(),
                req.thumbnailUrl(),
                req.storageKey(),
                req.sortOrder() == null ? 0 : req.sortOrder()
        );
    }

    @DeleteMapping("/{mediaId}")
    @PreAuthorize("hasAnyRole('WORKER','BRAND')")
    public void deleteMedia(@PathVariable Long mediaId) {
        providerMediaService.deleteMyMedia(currentUserService.requireUserId(), mediaId);
    }

    @PostMapping("/presign")
    @PreAuthorize("hasAnyRole('WORKER','BRAND')")
    public PresignProviderMediaResponse presign(@Valid @RequestBody PresignProviderMediaRequest req) {
        return providerMediaPresignService.createPresignedUpload(
                currentUserService.requireUserId(),
                req
        );
    }
}
