package com.openatom.club.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class CurrentUserResponse {
    private Long userId;
    private String username;
    private Long memberId;
    private String name;
    private String studentNo;
    private String phone;
    private String major;
    private String department;
    private String position;
    private boolean fullAccess;
    private boolean profileCompleted;
    private boolean initialPasswordChanged;
}
