package com.openatom.club.point.dto;

import com.openatom.club.point.entity.PointApplication;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class PointApplicationResponse {
    private Long id;
    private Long memberId;
    private String memberName;
    private String studentNo;
    private Long pointItemId;
    private String itemName;
    private String status;
    private OffsetDateTime submittedAt;
    private OffsetDateTime reviewedAt;
    private String reviewedBy;
    private String reviewComment;
    private OffsetDateTime createdAt;

    public static PointApplicationResponse from(PointApplication a) {
        PointApplicationResponse dto = new PointApplicationResponse();
        dto.id = a.getId();
        dto.memberId = a.getMemberId();
        dto.pointItemId = a.getPointItemId();
        dto.status = a.getStatus();
        dto.submittedAt = a.getSubmittedAt();
        dto.reviewedAt = a.getReviewedAt();
        dto.reviewedBy = a.getReviewedBy();
        dto.reviewComment = a.getReviewComment();
        dto.createdAt = a.getCreatedAt();
        return dto;
    }
}
