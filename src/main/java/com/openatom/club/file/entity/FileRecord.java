package com.openatom.club.file.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "files")
public class FileRecord {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "original_name", nullable = false, length = 500)
    private String originalName;

    @Column(name = "stored_name", nullable = false, length = 500)
    private String storedName;

    @Column(name = "file_path", nullable = false, columnDefinition = "TEXT")
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "content_type", length = 200)
    private String contentType;

    @Column(name = "file_hash", length = 64)
    private String fileHash;

    @Column(name = "module_name", nullable = false, length = 50)
    private String moduleName;

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
