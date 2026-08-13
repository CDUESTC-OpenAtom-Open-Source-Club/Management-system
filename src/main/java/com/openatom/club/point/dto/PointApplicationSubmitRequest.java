package com.openatom.club.point.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class PointApplicationSubmitRequest {
    @NotNull(message = "成员ID不能为空")
    private Long memberId;

    @NotEmpty(message = "请至少选择一个积分项目")
    private List<Long> pointItemIds;
}
