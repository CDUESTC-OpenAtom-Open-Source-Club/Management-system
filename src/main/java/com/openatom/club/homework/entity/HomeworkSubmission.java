package com.openatom.club.homework.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "homework_submissions", uniqueConstraints = {
    @UniqueConstraint(name = "uq_homework_submission", columnNames = {"homework_id", "member_id"})
})
@SQLRestriction("deleted_at IS NULL")
public class HomeworkSubmission {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "homework_id", nullable = false)
    private Long homeworkId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 20)
    private String status = "SUBMITTED";

    @Column(name = "submitted_at", nullable = false)
    private OffsetDateTime submittedAt;

    @Column(name = "review_comment", columnDefinition = "TEXT")
    private String reviewComment;

    @Column(name = "awarded_points", precision = 10, scale = 2)
    private BigDecimal awardedPoints;

    @Column(name = "reviewed_by_user_id")
    private Long reviewedByUserId;

    @Column(name = "reviewed_at")
    private OffsetDateTime reviewedAt;

    @Column(name = "point_record_id")
    private Long pointRecordId;

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
        if (submittedAt == null) submittedAt = OffsetDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
