package com.brandPitara.sfs.cms.workflow.entity;

import com.brandPitara.sfs.cms.content.entity.ContentPostEntity;
import com.brandPitara.sfs.cms.workflow.domain.ContentWorkflowAction;
import com.brandPitara.sfs.dashboard.user.entity.DashboardUserEntity;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Immutable;

import java.time.OffsetDateTime;

@Entity
@Immutable
@Table(name = "content_review_activity")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ContentReviewActivityEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "content_post_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_review_activity_post"))
    private ContentPostEntity contentPost;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "revision_id", foreignKey = @ForeignKey(name = "fk_content_review_activity_revision"))
    private ContentPostRevisionEntity revision;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ContentWorkflowAction action;

    @Column(length = 4000)
    private String comment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "actor_dashboard_user_id", nullable = false,
            foreignKey = @ForeignKey(name = "fk_content_review_activity_actor"))
    private DashboardUserEntity actor;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;
}
