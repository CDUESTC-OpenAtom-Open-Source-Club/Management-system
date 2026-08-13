package com.openatom.club.homework.dto;

import com.openatom.club.homework.entity.HomeworkSubmission;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Data
public class HomeworkSubmissionResponse {
    private Long id;
    private Long homeworkId;
    private String homeworkTitle;
    private Long memberId;
    private String memberName;
    private String memberStudentNo;
    private String memberDepartment;
    private String memberPosition;
    private String content;
    private String status;
    private OffsetDateTime submittedAt;
    private String reviewComment;
    private BigDecimal awardedPoints;
    private Long reviewedByUserId;
    private String reviewedByName;
    private OffsetDateTime reviewedAt;
    private Long pointRecordId;
    private List<SubmissionFileInfo> files;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    @Data
    public static class SubmissionFileInfo {
        private Long id;
        private Long fileId;
        private String originalName;
        private Long fileSize;
        private String contentType;
    }

    public static HomeworkSubmissionResponse from(HomeworkSubmission s) {
        HomeworkSubmissionResponse r = new HomeworkSubmissionResponse();
        r.id = s.getId();
        r.homeworkId = s.getHomeworkId();
        r.memberId = s.getMemberId();
        r.content = s.getContent();
        r.status = s.getStatus();
        r.submittedAt = s.getSubmittedAt();
        r.reviewComment = s.getReviewComment();
        r.awardedPoints = s.getAwardedPoints();
        r.reviewedByUserId = s.getReviewedByUserId();
        r.reviewedAt = s.getReviewedAt();
        r.pointRecordId = s.getPointRecordId();
        r.createdAt = s.getCreatedAt();
        r.updatedAt = s.getUpdatedAt();
        return r;
    }
}
