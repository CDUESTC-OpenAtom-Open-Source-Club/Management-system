package com.openatom.club.point.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "point_records")
@SQLRestriction("deleted_at IS NULL")
public class PointRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "point_item_id")
    private Long pointItemId;

    @Column(name = "application_id")
    private Long applicationId;

    @Column(nullable = false)
    private BigDecimal score;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(name = "source_type", length = 20, nullable = false)
    private String sourceType;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "occurred_at")
    private OffsetDateTime occurredAt;

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
        if (occurredAt == null) occurredAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
