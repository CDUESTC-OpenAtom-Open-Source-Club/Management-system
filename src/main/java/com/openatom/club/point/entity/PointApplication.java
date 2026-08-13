package com.openatom.club.point.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "point_applications")
@SQLRestriction("deleted_at IS NULL")
public class PointApplication {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "point_item_id", nullable = false)
    private Long pointItemId;

    @Column(length = 20, nullable = false)
    private String status = "PENDING";

    @Column(name = "submitted_at")
    private OffsetDateTime submittedAt;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

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
        submittedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
