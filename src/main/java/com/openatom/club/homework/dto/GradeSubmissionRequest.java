package com.openatom.club.homework.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
public class GradeSubmissionRequest {

    @NotNull(message = "积分不能为空")
    private BigDecimal points;

    private String comment;
}
