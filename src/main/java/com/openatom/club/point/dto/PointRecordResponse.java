package com.openatom.club.point.dto;

import com.openatom.club.point.entity.PointRecord;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PointRecordResponse {
    private Long id;
    private Long memberId;
    private Long pointItemId;
    private Long applicationId;
    private BigDecimal score;
    private String reason;
    private String sourceType;
    private String operatorName;
    private OffsetDateTime occurredAt;
    private OffsetDateTime createdAt;

    public static PointRecordResponse from(PointRecord r) {
        PointRecordResponse dto = new PointRecordResponse();
        dto.id = r.getId();
        dto.memberId = r.getMemberId();
        dto.pointItemId = r.getPointItemId();
        dto.applicationId = r.getApplicationId();
        dto.score = r.getScore();
        dto.reason = r.getReason();
        dto.sourceType = r.getSourceType();
        dto.operatorName = r.getOperatorName();
        dto.occurredAt = r.getOccurredAt();
        dto.createdAt = r.getCreatedAt();
        return dto;
    }
}
