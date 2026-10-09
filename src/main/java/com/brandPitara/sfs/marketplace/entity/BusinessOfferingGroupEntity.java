package com.brandPitara.sfs.marketplace.entity;

import com.brandPitara.sfs.entity.BaseEntity;
import com.brandPitara.sfs.entity.BusinessEntity;
import com.brandPitara.sfs.marketplace.enums.BusinessOfferingType;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "business_offering_group")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class BusinessOfferingGroupEntity extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "business_id", nullable = false)
    private BusinessEntity business;

    @Enumerated(EnumType.STRING)
    @Column(name = "offering_type", nullable = false, length = 20)
    private BusinessOfferingType offeringType;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private int sortOrder = 0;

    @OneToMany(mappedBy = "group", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("sortOrder ASC, id ASC")
    @Builder.Default
    private List<BusinessOfferingItemEntity> items = new ArrayList<>();
}
