package com.openatom.club.homework.dto;

import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class HomeworkAssignmentFileResponse {
    /** 关系表主键 */
    private Long id;
    /** files 表主键（下载/删除接口用 fileId） */
    private Long fileId;
    private String originalName;
    private Long fileSize;
    private String contentType;
    private OffsetDateTime createdAt;
}
