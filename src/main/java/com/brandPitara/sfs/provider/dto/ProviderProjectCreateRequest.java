package com.brandPitara.sfs.provider.dto;

import com.brandPitara.sfs.provider.enums.MediaType;
import com.brandPitara.sfs.provider.enums.ProjectVisibility;
import jakarta.validation.constraints.*;

import java.util.List;

public record ProviderProjectCreateRequest(
    @NotBlank @Size(max = 120) String title,
    @Size(max = 4000) String description,

    @NotNull Long categoryId,
    Long cityId,
    @Size(max = 120) String locality,

    @Min(0) Integer budgetMin,
    @Min(0) Integer budgetMax,

    @NotNull ProjectVisibility visibility,

    @NotEmpty List<ProjectMediaRequest> media
) {
  public record ProjectMediaRequest(
      @NotNull MediaType mediaType,
      @NotBlank String url,
      String thumbnailUrl,
      @Min(0) int sortOrder
  ) {}
}
