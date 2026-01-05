package com.brandPitara.sfs.request.dto;

import jakarta.validation.constraints.*;

import java.util.List;

public record ServiceRequestCreateRequest(
        @NotNull Long cityId,
        @Size(max = 120) String locality,
        @NotBlank @Size(max = 12) String pincode,
        @Size(max = 2000) String notes,
        @NotEmpty List<@NotNull Long> categoryIds
) {}
