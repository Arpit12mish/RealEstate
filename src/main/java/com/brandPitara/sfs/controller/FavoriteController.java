package com.brandPitara.sfs.controller;

import com.brandPitara.sfs.dto.BusinessResponse;
import com.brandPitara.sfs.dto.PageResponse;
import com.brandPitara.sfs.service.FavoriteService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteService favoriteService;

    // ❤️ Add
    @PostMapping("/{businessId}")
    public ResponseEntity<?> add(@PathVariable Long businessId) {
        favoriteService.addFavorite(businessId);
        return ResponseEntity.ok(Map.of("status", "FAVORITED"));
    }

    // 💔 Remove
    @DeleteMapping("/{businessId}")
    public ResponseEntity<?> remove(@PathVariable Long businessId) {
        favoriteService.removeFavorite(businessId);
        return ResponseEntity.ok(Map.of("status", "UNFAVORITED"));
    }

    // ✅ Exists (useful for heart icon)
    @GetMapping("/exists/{businessId}")
    public ResponseEntity<?> exists(@PathVariable Long businessId) {
        boolean fav = favoriteService.isFavorite(businessId);
        return ResponseEntity.ok(Map.of("favorite", fav));
    }

    // 📃 List (paged)
    @GetMapping
    public ResponseEntity<PageResponse<BusinessResponse>> list(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(50) int size
    ) {
        return ResponseEntity.ok(favoriteService.listFavorites(page, size));
    }

    @PostMapping("/{businessId}/toggle")
    public ResponseEntity<?> toggle(@PathVariable Long businessId) {
        favoriteService.toggleFavorite(businessId);

        boolean isFav = favoriteService.isFavorite(businessId);
        long count = favoriteService.getFavoriteCount(businessId); // add method OR use repository directly

        return ResponseEntity.ok(Map.of(
                "status", isFav ? "FAVORITED" : "UNFAVORITED",
                "isFavorite", isFav,
                "favoriteCount", count
        ));
    }

}
