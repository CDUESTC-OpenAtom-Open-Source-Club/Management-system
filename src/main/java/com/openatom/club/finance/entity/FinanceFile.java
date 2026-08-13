package com.openatom.club.finance.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "finance_files")
public class FinanceFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "period_id", nullable = false)
    private Long periodId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "file_type", length = 20, nullable = false)
    private String fileType;

    @Column(columnDefinition = "TEXT")
    private String remark;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
