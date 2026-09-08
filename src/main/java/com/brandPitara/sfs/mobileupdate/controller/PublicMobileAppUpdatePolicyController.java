package com.brandPitara.sfs.mobileupdate.controller;

import com.brandPitara.sfs.mobileupdate.MobilePlatform;
import com.brandPitara.sfs.mobileupdate.dto.MobileAppUpdatePolicyResponse;
import com.brandPitara.sfs.mobileupdate.service.MobileAppUpdatePolicyService;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Max;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/mobile-app/update-policy")
@RequiredArgsConstructor
@Validated
public class PublicMobileAppUpdatePolicyController {

    private final MobileAppUpdatePolicyService service;

    @GetMapping
    public ResponseEntity<MobileAppUpdatePolicyResponse> getPolicy(
            @RequestParam MobilePlatform platform,
            @RequestParam @PositiveOrZero @Max(9007199254740991L) Long currentBuild) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(service.evaluate(platform, currentBuild));
    }
}
