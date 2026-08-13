package com.openatom.club.member.dto;

import com.openatom.club.member.entity.Member;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class MemberResponse {
    private Long id;
    private String name;
    private String studentNo;
    private String phone;
    private String major;
    private String department;
    private String position;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;

    public static MemberResponse from(Member m) {
        MemberResponse dto = new MemberResponse();
        dto.id = m.getId();
        dto.name = m.getName();
        dto.studentNo = m.getStudentNo();
        dto.phone = m.getPhone();
        dto.major = m.getMajor();
        dto.department = m.getDepartment();
        dto.position = m.getPosition();
        dto.createdAt = m.getCreatedAt();
        dto.updatedAt = m.getUpdatedAt();
        return dto;
    }
}
