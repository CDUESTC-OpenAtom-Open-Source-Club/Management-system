package com.openatom.club.homework.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

/**
 * 作业发布附件关系表：管理员/部长发布作业时上传的资料附件。
 * 与 {@link HomeworkSubmissionFile}（成员提交附件）严格区分。
 * 采用软删除（deleted_at），删除附件 = 软删除关系 + 软删除 files 记录。
 */
@Data
@Entity
@Table(name = "homework_assignment_files")
public class HomeworkAssignmentFile {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "assignment_id", nullable = false)
    private Long assignmentId;

    @Column(name = "file_id", nullable = false)
    private Long fileId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "deleted_at")
    private OffsetDateTime deletedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
