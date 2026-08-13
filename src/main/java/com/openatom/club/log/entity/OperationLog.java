package com.openatom.club.log.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "operation_logs")
public class OperationLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "operator_name", length = 100)
    private String operatorName;

    @Column(name = "operator_department", length = 100)
    private String operatorDepartment;

    @Column(name = "operator_position", length = 50)
    private String operatorPosition;

    @Column(name = "module_name", length = 50)
    private String moduleName;

    @Column(name = "action_type", length = 50)
    private String actionType;

    @Column(name = "target_id", length = 100)
    private String targetId;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = OffsetDateTime.now();
    }
}
