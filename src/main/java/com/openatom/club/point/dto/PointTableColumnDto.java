package com.openatom.club.point.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class PointTableColumnDto {
    private Long pointItemId;
    private String itemName;
    private BigDecimal pointValue;
}
