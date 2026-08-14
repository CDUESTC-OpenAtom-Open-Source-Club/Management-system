package com.openatom.club.auth.dto;

import lombok.Data;

@Data
public class UpdateMyProfileRequest {
    private String name;
    private String studentNo;
    private String phone;
    private String major;
}
