package com.brandPitara.sfs.publicreview.controller.admin;

import com.brandPitara.sfs.publicreview.dto.PublicReviewSampleResponse;
import com.brandPitara.sfs.publicreview.dto.UpdatePublicReviewDisplayStatusRequest;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/public-review-samples")
@RequiredArgsConstructor
public class AdminPublicReviewSampleController {

    private final PublicReviewService publicReviewService;

    @PatchMapping("/{sampleId}/display-status")
    @PreAuthorize("hasRole('ADMIN')")
    public PublicReviewSampleResponse updateDisplayStatus(
        @PathVariable Long sampleId,
        @Valid @RequestBody UpdatePublicReviewDisplayStatusRequest request
    ) {
        return publicReviewService.updateSampleDisplayStatus(sampleId, request);
    }
}
