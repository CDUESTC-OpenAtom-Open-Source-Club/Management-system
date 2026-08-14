package com.openatom.club.cohort.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CohortRequest {
    @NotNull(message = "届次年份不能为空")
    private Integer year;

    private Boolean enabled;
}
