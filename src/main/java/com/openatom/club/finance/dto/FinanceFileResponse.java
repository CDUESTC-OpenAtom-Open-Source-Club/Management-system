package com.openatom.club.finance.dto;

import com.openatom.club.finance.entity.FinanceFile;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class FinanceFileResponse {
    private Long id;
    private Long periodId;
    private Long fileId;
    private String fileType;
    private String remark;
    private String createdBy;
    private OffsetDateTime createdAt;
    private String originalName;
    private Long fileSize;
    private String contentType;

    public static FinanceFileResponse from(FinanceFile f) {
        FinanceFileResponse dto = new FinanceFileResponse();
        dto.id = f.getId();
        dto.periodId = f.getPeriodId();
        dto.fileId = f.getFileId();
        dto.fileType = f.getFileType();
        dto.remark = f.getRemark();
        dto.createdBy = f.getCreatedBy();
        dto.createdAt = f.getCreatedAt();
        return dto;
    }
}
