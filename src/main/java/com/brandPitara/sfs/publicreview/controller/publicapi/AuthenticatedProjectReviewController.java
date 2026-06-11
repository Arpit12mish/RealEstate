package com.brandPitara.sfs.publicreview.controller.publicapi;

import com.brandPitara.sfs.entity.User;
import com.brandPitara.sfs.publicreview.dto.MySubmittedReviewResponse;
import com.brandPitara.sfs.publicreview.dto.PublicAuthenticatedReviewCreateRequest;
import com.brandPitara.sfs.publicreview.dto.SfsReviewResponse;
import com.brandPitara.sfs.publicreview.service.PublicReviewService;
import com.brandPitara.sfs.security.CurrentUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AuthenticatedProjectReviewController {

    private final PublicReviewService publicReviewService;
    private final CurrentUserService currentUserService;

    @PostMapping("/api/projects/{projectId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("!hasRole('GUEST')")
    public SfsReviewResponse submitReview(
            @PathVariable Long projectId,
            @Valid @RequestBody PublicAuthenticatedReviewCreateRequest request
    ) {
        User currentUser = currentUserService.requireUser();
        return publicReviewService.submitAuthenticatedProjectReview(projectId, request, currentUser);
    }

    @GetMapping("/api/profile/submitted-reviews")
    public List<MySubmittedReviewResponse> listMySubmittedReviews() {
        User currentUser = currentUserService.requireUser();
        return publicReviewService.listMySubmittedReviews(currentUser);
    }
}
