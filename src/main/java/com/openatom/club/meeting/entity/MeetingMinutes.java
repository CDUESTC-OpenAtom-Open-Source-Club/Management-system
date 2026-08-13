package com.openatom.club.meeting.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "meeting_minutes")
@SQLRestriction("deleted_at IS NULL")
public class MeetingMinutes {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(name = "meeting_date", nullable = false)
    private LocalDate meetingDate;

    @Column(name = "meeting_year", nullable = false)
    private Integer meetingYear;

    @Column(name = "meeting_month", nullable = false)
    private Integer meetingMonth;

    @Column(name = "file_id")
    private Long fileId;

    @Column(columnDefinition = "TEXT")
    private String remark;

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
