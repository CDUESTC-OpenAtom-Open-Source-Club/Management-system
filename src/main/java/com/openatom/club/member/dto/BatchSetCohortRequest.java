package com.openatom.club.member.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchSetCohortRequest {
    @NotEmpty(message = "请选择成员")
    private List<Long> memberIds;

    @NotNull(message = "请选择届次")
    private Long cohortId;
}
