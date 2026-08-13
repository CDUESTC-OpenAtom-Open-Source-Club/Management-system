package com.openatom.club.archive.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "archive_links")
@SQLRestriction("deleted_at IS NULL")
public class ArchiveLink {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "archive_year", nullable = false)
    private Integer archiveYear;

    @Column(name = "archive_type", length = 50)
    private String archiveType;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String url;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
        updatedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
