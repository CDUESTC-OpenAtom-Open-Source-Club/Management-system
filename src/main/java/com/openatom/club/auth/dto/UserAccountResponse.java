package com.openatom.club.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
@Builder
public class UserAccountResponse {
    private Long id;
    private String username;
    private Long memberId;
    private String name;
    private String studentNo;
    private String phone;
    private String major;
    private String department;
    private String position;
    private Boolean enabled;
    private Boolean profileCompleted;
    private Boolean initialPasswordChanged;
    private OffsetDateTime lastLoginAt;
    private OffsetDateTime createdAt;
}
