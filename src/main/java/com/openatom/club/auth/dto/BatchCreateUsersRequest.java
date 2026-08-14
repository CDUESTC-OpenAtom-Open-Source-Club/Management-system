package com.openatom.club.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class BatchCreateUsersRequest {
    @NotNull(message = "届次不能为空")
    private Long cohortId;

    @NotEmpty(message = "accounts 不能为空")
    @Valid
    private List<BatchCreateAccountItem> accounts;
}
