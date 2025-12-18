package com.brandPitara.sfs.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BusinessEventRequest {

    @NotBlank
    private String eventType;   // CARD_VIEW, CALL_CLICK, ...

    @NotBlank
    private String source;      // SEARCH, CATEGORY_PAGE, BANNER

    @NotNull
    private Long cityId;

    @NotNull
    private Long categoryId;

    // optional: card position in list
    private Integer listingPosition;
}
