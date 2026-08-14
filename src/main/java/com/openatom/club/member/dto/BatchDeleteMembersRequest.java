package com.openatom.club.member.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class BatchDeleteMembersRequest {

    @NotEmpty(message = "请选择要删除的成员")
    private List<Long> memberIds;
}
