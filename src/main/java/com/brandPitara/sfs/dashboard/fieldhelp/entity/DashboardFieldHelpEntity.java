package com.brandPitara.sfs.dashboard.fieldhelp.entity;

import com.brandPitara.sfs.dashboard.common.enums.DashboardHelpModule;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(
        name = "dashboard_field_help",
        indexes = {
                @Index(name = "idx_dashboard_field_help_module", columnList = "module"),
                @Index(name = "idx_dashboard_field_help_active", columnList = "active")
        },
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uq_dashboard_field_help_module_field",
                        columnNames = {"module", "field_key"}
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardFieldHelpEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 100)
    private DashboardHelpModule module;

    @Column(name = "field_key", nullable = false, length = 150)
    private String fieldKey;

    @Column(name = "field_label", nullable = false, length = 180)
    private String fieldLabel;

    @Column(name = "short_help", nullable = false, columnDefinition = "text")
    private String shortHelp;

    @Column(name = "detailed_help", columnDefinition = "text")
    private String detailedHelp;

    @Column(name = "why_needed", columnDefinition = "text")
    private String whyNeeded;

    @Column(name = "source_hint", columnDefinition = "text")
    private String sourceHint;

    @Column(name = "example_value", columnDefinition = "text")
    private String exampleValue;

    @Column(name = "validation_hint", columnDefinition = "text")
    private String validationHint;

    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "display_order", nullable = false)
    @Builder.Default
    private Integer displayOrder = 0;

    @Column(name = "created_by_dashboard_user_id")
    private Long createdByDashboardUserId;

    @Column(name = "updated_by_dashboard_user_id")
    private Long updatedByDashboardUserId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.active == null) this.active = true;
        if (this.displayOrder == null) this.displayOrder = 0;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
