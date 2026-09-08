package com.brandPitara.sfs.company.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.CityEntity;
import com.brandPitara.sfs.company.dto.*;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "company_project")
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyProjectEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  @Column(nullable = false, length = 180)
  private String name;

  @Column(length = 220, unique = true)
  private String slug;

  @Column(name = "short_description", length = 300)
  private String shortDescription;

  @Column(columnDefinition = "text")
  private String description;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "city_id")
  private CityEntity city;

  @Column(name = "address_line", columnDefinition = "text")
  private String addressLine;

  @Column(name = "location_label", length = 180)
  private String locationLabel;

  @Column(name = "client_name", length = 180)
  private String clientName;

  @Column(name = "project_area", length = 100)
  private String projectArea;

  @Column(name = "detail3", length = 180)
  private String detail3;

  @Column(name = "tags", columnDefinition = "text")
  private String tags;

  @Column(name = "cover_media_url", columnDefinition = "text")
  private String coverMediaUrl;

  @Column(name = "cover_media_type", length = 20)
  private String coverMediaType; // IMAGE | VIDEO | etc

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "stats_json", columnDefinition = "jsonb", nullable = false)
  @Builder.Default
  private List<CompanyProjectStatDto> stats = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "budget_json", columnDefinition = "jsonb", nullable = false)
  @Builder.Default
  private CompanyProjectBudgetDto budget = new CompanyProjectBudgetDto();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "price_breakdown_json", columnDefinition = "jsonb", nullable = false)
  @Builder.Default
  private List<CompanyProjectPriceBreakdownItemDto> priceBreakdown = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "client_requirements_json", columnDefinition = "jsonb", nullable = false)
  @Builder.Default
  private List<CompanyProjectClientRequirementDto> clientRequirements = new ArrayList<>();

  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "design_materials_json", columnDefinition = "jsonb", nullable = false)
  @Builder.Default
  private CompanyProjectDesignMaterialsDto designMaterials = new CompanyProjectDesignMaterialsDto();

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean published = false;

  @Column(nullable = false)
  @Builder.Default
  private Integer priority = 0;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
