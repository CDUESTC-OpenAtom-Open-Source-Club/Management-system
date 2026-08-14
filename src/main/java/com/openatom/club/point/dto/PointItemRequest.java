package com.openatom.club.point.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.util.List;

@Data
public class PointItemRequest {
    @NotBlank(message = "积分项目名称不能为空")
    private String itemName;

    @NotNull(message = "积分分値不能为空")
    private BigDecimal pointValue;

    private String itemType;
    private String description;
    private Integer sortOrder;
    private Boolean enabled;
    private Boolean allowMemberApply;

    /** 适用届次（空 = 全局适用） */
    private List<Long> cohortIds;
}
