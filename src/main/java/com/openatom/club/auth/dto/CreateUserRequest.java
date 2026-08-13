package com.openatom.club.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateUserRequest {
    @NotBlank(message = "用户名不能为空")
    private String username;

    @NotBlank(message = "初始密码不能为空")
    @Size(min = 6, message = "初始密码至少 6 位")
    private String initialPassword;

    private Boolean enabled = true;
}
