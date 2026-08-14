package com.openatom.club.point.dto;

import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class PointApplicationSubmitRequest {
    @NotEmpty(message = "请至少选择一个积分项目")
    private List<Long> pointItemIds;
}
