package com.brandPitara.sfs.provider.dto;

import com.brandPitara.sfs.provider.enums.ProviderType;
import jakarta.validation.constraints.*;

import java.util.List;

public record ProviderProfileUpsertRequest(
    @NotNull ProviderType providerType,

    @NotBlank @Size(max = 120) String displayName,
    @Size(max = 180) String headline,
    @Size(max = 2000) String bio,

    @NotNull Long primaryCategoryId,
    @Min(0) @Max(60) Integer experienceYears,

    // brand-only optional
    @Size(max = 150) String businessName,
    @Size(max = 30) String gstNumber,

    @NotEmpty List<ServiceAreaRequest> serviceAreas
) {
  public record ServiceAreaRequest(
      @NotNull Long cityId,
      @Size(max = 120) String locality,
      @Size(max = 12) String pincode,
      Double latitude,
      Double longitude
  ) {}
}
