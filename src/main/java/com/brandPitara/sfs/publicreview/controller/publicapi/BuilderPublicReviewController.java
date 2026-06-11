package com.brandPitara.sfs.publicreview.controller.publicapi;

import com.brandPitara.sfs.publicreview.dto.PublicReviewSignalResponse;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/builders")
@RequiredArgsConstructor
public class BuilderPublicReviewController {

    private final PublicReviewService publicReviewService;

    @GetMapping("/{builderId}/public-review-signal")
    public PublicReviewSignalResponse signal(@PathVariable Long builderId) {
        return publicReviewService.getPublicSignal(PublicReviewTargetType.BUILDER, builderId);
    }
}
