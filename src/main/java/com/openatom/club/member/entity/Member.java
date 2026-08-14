package com.openatom.club.member.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "members")
@SQLRestriction("deleted_at IS NULL")
public class Member {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(name = "student_no", unique = true, length = 50)
    private String studentNo;

    @Column(length = 20)
    private String phone;

    @Column(length = 100)
    private String major;

    @Column(length = 100)
    private String department;

    @Column(length = 50, nullable = false)
    private String position = "社员";

    @Column(name = "cohort_id")
    private Long cohortId;

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
        if (position == null) position = "社员";
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = OffsetDateTime.now();
    }
}
