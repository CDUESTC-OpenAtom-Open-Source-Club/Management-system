package com.openatom.club.auth.dto;

import lombok.Data;

@Data
public class UpdateMyProfileRequest {
    private String name;
    private String studentNo;
    private String phone;
    private String major;
    // 部门/职务允许本人自改，但后端会校验「不得自设管理员身份」（秘书处/会长/副会长/部长）
    private String department;
    private String position;
}
