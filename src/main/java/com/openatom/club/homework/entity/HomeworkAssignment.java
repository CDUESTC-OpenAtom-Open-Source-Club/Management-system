package com.openatom.club.homework.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "homework_assignments")
@SQLRestriction("deleted_at IS NULL")
public class HomeworkAssignment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "target_type", nullable = false, length = 20)
    private String targetType = "ALL";

    @Column(name = "target_department", length = 100)
    private String targetDepartment;

    @Column(name = "cohort_id")
    private Long cohortId;

    @Column(nullable = false)
    private OffsetDateTime deadline;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(name = "max_points", precision = 10, scale = 2)
    private BigDecimal maxPoints;

    @Column(name = "point_item_id")
    private Long pointItemId;

    @Column(name = "created_by_user_id")
    private Long createdByUserId;

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
