package com.brandPitara.sfs.publicreview.controller.publicapi;

import com.brandPitara.sfs.publicreview.dto.PublicReviewSignalResponse;
import com.brandPitara.sfs.publicreview.dto.SfsReviewResponse;
import com.brandPitara.sfs.publicreview.enums.PublicReviewTargetType;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectPublicReviewController {

    private final PublicReviewService publicReviewService;

    @GetMapping("/{projectId}/public-review-signal")
    public PublicReviewSignalResponse signal(@PathVariable Long projectId) {
        return publicReviewService.getPublicSignal(PublicReviewTargetType.PROJECT, projectId);
    }

    @GetMapping("/{projectId}/reviews")
    public List<SfsReviewResponse> listApprovedReviews(@PathVariable Long projectId) {
        return publicReviewService.listPublicApprovedReviews(projectId);
    }
}
