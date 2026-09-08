package com.brandPitara.sfs.publiccontent.controller;

import com.brandPitara.sfs.cms.content.domain.ContentType;
import com.brandPitara.sfs.publiccontent.dto.PublicCategoryListResponse;
import com.brandPitara.sfs.publiccontent.dto.PublicContentDetailResponse;
import com.brandPitara.sfs.publiccontent.dto.PublicContentPageResponse;
import com.brandPitara.sfs.publiccontent.service.PublicCategoryService;
import com.brandPitara.sfs.publiccontent.service.PublicContentResult;
import com.brandPitara.sfs.publiccontent.service.PublicContentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/content")
@RequiredArgsConstructor
public class PublicContentController {
    public static final String CACHE_CONTROL =
            "public, max-age=0, s-maxage=60, stale-while-revalidate=60, stale-if-error=300";

    private final PublicContentService service;
    private final PublicCategoryService categoryService;

    /**
     * A literal segment always wins over {slug} in Spring's path matching
     * regardless of declaration order, so this cannot fall through to
     * detail(slug="categories") - see PublicContentControllerTest's
     * route-collision regression test, which asserts the {slug} service
     * method is never invoked for this path.
     */
    @GetMapping("/categories")
    public ResponseEntity<PublicCategoryListResponse> categories() {
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                .body(categoryService.list());
    }

    @GetMapping("/{slug}")
    public ResponseEntity<PublicContentDetailResponse> detail(
            @PathVariable String slug,
            @RequestHeader(value = HttpHeaders.IF_NONE_MATCH, required = false) String ifNoneMatch
    ) {
        PublicContentResult result = service.getBySlug(slug, ifNoneMatch);
        if (result.notModified()) {
            return ResponseEntity.status(304)
                    .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                    .eTag(result.etag())
                    .build();
        }
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                .eTag(result.etag())
                .body(result.body());
    }

    @GetMapping
    public ResponseEntity<PublicContentPageResponse> list(
            @RequestParam(required = false) ContentType contentType,
            @RequestParam(required = false) String categorySlug,
            @RequestParam(required = false) String author,
            @RequestParam(required = false) String tag,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PublicContentPageResponse response = categorySlug == null && author == null && tag == null && q == null
                ? service.list(contentType, page, size)
                : service.list(contentType, categorySlug, author, tag, q, page, size);
        return ResponseEntity.ok()
                .header(HttpHeaders.CACHE_CONTROL, CACHE_CONTROL)
                .body(response);
    }
}
