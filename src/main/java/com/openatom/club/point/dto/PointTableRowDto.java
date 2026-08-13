package com.openatom.club.point.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.util.Map;

@Data
public class PointTableRowDto {
    private Integer rankNo;
    private Long memberId;
    private String name;
    private String studentNo;
    private String phone;
    private String major;
    private String department;
    private String position;
    private Map<String, BigDecimal> scores;
    private BigDecimal totalScore;
}
