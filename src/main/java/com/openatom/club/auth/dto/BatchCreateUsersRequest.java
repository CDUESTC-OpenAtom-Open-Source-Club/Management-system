package com.openatom.club.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchCreateUsersRequest {
    @NotEmpty(message = "accounts 不能为空")
    @Valid
    private List<BatchCreateAccountItem> accounts;
}
