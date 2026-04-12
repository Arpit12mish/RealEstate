package com.brandPitara.sfs.entity;

import com.brandPitara.sfs.enums.FavoriteTargetType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.OffsetDateTime;

@Entity
@Table(
    name = "user_favorite",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_user_favorite_user_target",
            columnNames = {"user_id", "target_type", "target_id"}
        )
    },
    indexes = {
        @Index(name = "idx_user_favorite_user_id", columnList = "user_id"),
        @Index(name = "idx_user_favorite_target", columnList = "target_type,target_id")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFavoriteEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private FavoriteTargetType targetType;

    @Column(name = "target_id", nullable = false)
    private Long targetId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
