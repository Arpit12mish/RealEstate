package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.GuestSessionRequest;
import com.brandPitara.sfs.dto.GuestSessionResponse;
import com.brandPitara.sfs.service.GuestSessionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class GuestAuthController {

    private final GuestSessionService guestSessionService;

    @PostMapping("/guest/session")
    public ResponseEntity<GuestSessionResponse> createGuestSession(
            @Valid @RequestBody GuestSessionRequest request
    ) {
        return ResponseEntity.ok(guestSessionService.createOrReuseGuestSession(request));
    }
}