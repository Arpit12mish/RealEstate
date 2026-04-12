package com.brandPitara.sfs.appcontent.entity;

import com.brandPitara.sfs.appcontent.enums.AppContentType;
import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;

@Entity
@Table(name = "app_content_page")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppContentPage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slug", nullable = false, unique = true, length = 100)
    private String slug;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private AppContentType contentType;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @PrePersist
    public void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
        if (this.contentType == null) {
            this.contentType = AppContentType.PLAIN_TEXT;
        }
    }

    @PreUpdate
    public void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}