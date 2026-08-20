package com.openatom.club.point.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PointDetailResponse {
    private Long id;
    private Long memberId;
    private Long pointItemId;
    private String pointItemName;
    private String pointItemType;
    private BigDecimal score;
    private String sourceType;
    private String sourceLabel;
    private String reason;
    private String operatorName;
    private OffsetDateTime occurredAt;
    private OffsetDateTime createdAt;
}
