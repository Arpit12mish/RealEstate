package com.brandPitara.sfs.instagram.controller.publicapi;

import com.brandPitara.sfs.instagram.dto.PublicInstagramReelItemResponse;
import com.brandPitara.sfs.instagram.enums.InstagramReelCategory;
import com.brandPitara.sfs.instagram.service.InstagramReelService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/public/instagram-reels")
@RequiredArgsConstructor
public class InstagramReelPublicController {

    private final InstagramReelService instagramReelService;

    @GetMapping
    public List<PublicInstagramReelItemResponse> list(
        @RequestParam(defaultValue = "LATEST") InstagramReelCategory category,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return instagramReelService.publicList(category, page, size);
    }
}
