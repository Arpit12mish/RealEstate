package com.brandPitara.sfs.company.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "company_pricing_plan")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyPricingPlanEntity extends BaseEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "company_id", nullable = false)
  private CompanyEntity company;

  // SUBSCRIPTION | PROJECT_BASED
  @Column(name = "pricing_type", nullable = false, length = 20)
  private String pricingType;

  @Column(name = "plan_name", nullable = false, length = 100)
  private String planName;

  @Column(name = "price_amount", precision = 12, scale = 2)
  private BigDecimal priceAmount;

  @Column(nullable = false, length = 10)
  @Builder.Default
  private String currency = "INR";

  @Column(name = "billing_unit", length = 30)
  private String billingUnit;

  @Column(columnDefinition = "text")
  private String description;

  // JSON array of strings, e.g. ["3D Design", "Site Visits"]. The DB column is a
  // real `jsonb` type (see V133), so this requires @JdbcTypeCode(SqlTypes.JSON) -
  // without it Hibernate binds a plain varchar parameter and Postgres rejects the
  // implicit varchar->jsonb assignment (PSQLException, surfaced as a 500).
  @JdbcTypeCode(SqlTypes.JSON)
  @Column(name = "features_json", columnDefinition = "jsonb", nullable = false)
  @Builder.Default
  private List<String> features = new ArrayList<>();

  @Column(name = "sort_order", nullable = false)
  @Builder.Default
  private Integer sortOrder = 0;

  @Column(name = "public_visible", nullable = false)
  @Builder.Default
  private Boolean publicVisible = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean active = true;

  @Column(nullable = false)
  @Builder.Default
  private Boolean deleted = false;
}
