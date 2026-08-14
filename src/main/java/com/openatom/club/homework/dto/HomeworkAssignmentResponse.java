package com.openatom.club.homework.dto;

import com.openatom.club.homework.entity.HomeworkAssignment;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class HomeworkAssignmentResponse {
    private Long id;
    private String title;
    private String description;
    private String targetType;
    private String targetDepartment;
    private Long cohortId;
    private Integer cohortYear;
    private OffsetDateTime deadline;
    private String status;
    private BigDecimal maxPoints;
    private Long pointItemId;
    private String pointItemName;
    private Long createdByUserId;
    private String createdByName;
    private long submissionCount;
    private long submittedCount;
    private long gradedCount;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static HomeworkAssignmentResponse from(HomeworkAssignment a) {
        HomeworkAssignmentResponse r = new HomeworkAssignmentResponse();
        r.id = a.getId();
        r.title = a.getTitle();
        r.description = a.getDescription();
        r.targetType = a.getTargetType();
        r.targetDepartment = a.getTargetDepartment();
        r.cohortId = a.getCohortId();
        r.deadline = a.getDeadline();
        r.status = a.getStatus();
        r.maxPoints = a.getMaxPoints();
        r.pointItemId = a.getPointItemId();
        r.createdByUserId = a.getCreatedByUserId();
        r.createdAt = a.getCreatedAt();
        r.updatedAt = a.getUpdatedAt();
        return r;
    }
}
