package com.openatom.club.homework.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class HomeworkAssignmentRequest {

    @NotBlank(message = "作业标题不能为空")
    private String title;

    private String description;

    @NotBlank(message = "目标范围不能为空")
    private String targetType; // ALL or DEPARTMENT

    private String targetDepartment;

    @NotNull(message = "截止时间不能为空")
    private OffsetDateTime deadline;

    private BigDecimal maxPoints;

    private Long pointItemId;
}
