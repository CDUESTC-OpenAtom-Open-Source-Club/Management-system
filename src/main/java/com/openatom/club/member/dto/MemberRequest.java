package com.openatom.club.member.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class MemberRequest {
    @NotBlank(message = "姓名不能为空")
    private String name;

    @NotBlank(message = "学号不能为空")
    private String studentNo;

    private String phone;
    private String major;
    private String department;
    private String position;
    private Long cohortId;
}
