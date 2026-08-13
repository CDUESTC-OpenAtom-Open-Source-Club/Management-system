package com.openatom.club.point.entity;

import jakarta.persistence.*;
import lombok.Data;
import org.hibernate.annotations.SQLRestriction;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
@Entity
@Table(name = "point_items")
@SQLRestriction("deleted_at IS NULL")
public class PointItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_name", nullable = false, length = 200)
    private String itemName;

    @Column(name = "point_value", nullable = false)
    private BigDecimal pointValue = BigDecimal.ZERO;

    @Column(name = "item_type", length = 50)
    private String itemType = "其他";

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private Boolean enabled = true;

    @Column(name = "allow_member_apply", nullable = false)
    private Boolean allowMemberApply = true;

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
