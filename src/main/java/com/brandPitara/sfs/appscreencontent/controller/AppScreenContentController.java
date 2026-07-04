package com.brandPitara.sfs.appscreencontent.controller;

import com.brandPitara.sfs.appscreencontent.dto.AppScreenContentResponse;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenKey;
import com.brandPitara.sfs.appscreencontent.enums.AppScreenPlacement;
import com.brandPitara.sfs.appscreencontent.service.AppScreenContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/app/screen-content")
@RequiredArgsConstructor
public class AppScreenContentController {

    private final AppScreenContentService service;

    @GetMapping
    public ResponseEntity<AppScreenContentResponse> getActive(
            @RequestParam("screen") AppScreenKey screenKey,
            @RequestParam("placement") AppScreenPlacement placement
    ) {
        return service.getActive(screenKey, placement)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }
}
