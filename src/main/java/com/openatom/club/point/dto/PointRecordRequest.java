package com.openatom.club.point.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Data
public class PointRecordRequest {
    private Long pointItemId;

    @NotNull(message = "分値不能为空")
    private BigDecimal score;

    @NotBlank(message = "积分原因不能为空")
    private String reason;

    private OffsetDateTime occurredAt;
}
