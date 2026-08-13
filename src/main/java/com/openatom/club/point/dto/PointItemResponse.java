package com.openatom.club.point.dto;

import com.openatom.club.point.entity.PointItem;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PointItemResponse {
    private Long id;
    private String itemName;
    private BigDecimal pointValue;
    private String itemType;
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean allowMemberApply;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static PointItemResponse from(PointItem p) {
        PointItemResponse dto = new PointItemResponse();
        dto.id = p.getId();
        dto.itemName = p.getItemName();
        dto.pointValue = p.getPointValue();
        dto.itemType = p.getItemType();
        dto.description = p.getDescription();
        dto.sortOrder = p.getSortOrder();
        dto.enabled = p.getEnabled();
        dto.allowMemberApply = p.getAllowMemberApply();
        dto.createdBy = p.getCreatedBy();
        dto.createdAt = p.getCreatedAt();
        dto.updatedAt = p.getUpdatedAt();
        return dto;
    }
}
