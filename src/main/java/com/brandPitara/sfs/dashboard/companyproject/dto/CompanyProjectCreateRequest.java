package com.brandPitara.sfs.dashboard.companyproject.dto;

import com.brandPitara.sfs.company.dto.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectCreateRequest {
  @NotNull private Long companyId;
  @NotBlank @Size(max = 180) private String name;
  @Size(max = 160) private String slug;
  @Size(max = 300) private String shortDescription;
  private String description;
  private Long cityId;
  private String addressLine;
  @Size(max = 180) private String locationLabel;
  @Size(max = 180) private String clientName;
  @Size(max = 100) private String projectArea;
  @Size(max = 180) private String detail3;
  private List<@Size(max = 80) String> tags;
  private String coverMediaUrl;
  @Size(max = 20) private String coverMediaType;
  @Valid @Size(max = 4) private List<CompanyProjectStatDto> stats;
  @Valid private CompanyProjectBudgetDto budget;
  @Valid @Size(max = 20) private List<CompanyProjectPriceBreakdownItemDto> priceBreakdown;
  @Valid @Size(max = 20) private List<CompanyProjectClientRequirementDto> clientRequirements;
  @Valid private CompanyProjectDesignMaterialsDto designMaterials;
  private Boolean active;
  private Boolean published;
  private Integer priority;
}
