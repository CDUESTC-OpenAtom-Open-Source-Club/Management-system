package com.openatom.club.point.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectRequest {
    @NotBlank(message = "驳回意见不能为空")
    private String reviewComment;
}
