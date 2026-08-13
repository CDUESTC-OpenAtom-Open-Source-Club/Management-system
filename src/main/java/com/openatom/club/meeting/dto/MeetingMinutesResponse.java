package com.openatom.club.meeting.dto;

import com.openatom.club.meeting.entity.MeetingMinutes;
import lombok.Data;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Data
public class MeetingMinutesResponse {
    private Long id;
    private String title;
    private LocalDate meetingDate;
    private Integer meetingYear;
    private Integer meetingMonth;
    private Long fileId;
    private String remark;
    private String createdBy;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static MeetingMinutesResponse from(MeetingMinutes m) {
        MeetingMinutesResponse dto = new MeetingMinutesResponse();
        dto.id = m.getId();
        dto.title = m.getTitle();
        dto.meetingDate = m.getMeetingDate();
        dto.meetingYear = m.getMeetingYear();
        dto.meetingMonth = m.getMeetingMonth();
        dto.fileId = m.getFileId();
        dto.remark = m.getRemark();
        dto.createdBy = m.getCreatedBy();
        dto.createdAt = m.getCreatedAt();
        dto.updatedAt = m.getUpdatedAt();
        return dto;
    }
}
