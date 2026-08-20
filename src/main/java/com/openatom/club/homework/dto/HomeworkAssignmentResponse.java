package com.openatom.club.homework.dto;

import com.openatom.club.homework.entity.HomeworkAssignment;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

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
    /** 所有有效提交总数（含已批改），即「提交人数」；批改不会使其减少 */
    private long submissionCount;
    /** 当前仍处于 SUBMITTED 状态的数量，即「待批改」；成员视角「我的作业」被覆盖为「本人是否已提交 0/1」 */
    private long submittedCount;
    /** 已批改（GRADED）数量 */
    private long gradedCount;
    private List<HomeworkAssignmentFileResponse> attachments = new ArrayList<>();
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
