package com.openatom.club.log.dto;

import com.openatom.club.log.entity.OperationLog;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class OperationLogResponse {
    private Long id;
    private String operatorName;
    private String operatorDepartment;
    private String operatorPosition;
    private String moduleName;
    private String actionType;
    private String targetId;
    private String description;
    private OffsetDateTime createdAt;

    public static OperationLogResponse from(OperationLog log) {
        OperationLogResponse dto = new OperationLogResponse();
        dto.id = log.getId();
        dto.operatorName = log.getOperatorName();
        dto.operatorDepartment = log.getOperatorDepartment();
        dto.operatorPosition = log.getOperatorPosition();
        dto.moduleName = log.getModuleName();
        dto.actionType = log.getActionType();
        dto.targetId = log.getTargetId();
        dto.description = log.getDescription();
        dto.createdAt = log.getCreatedAt();
        return dto;
    }
}
