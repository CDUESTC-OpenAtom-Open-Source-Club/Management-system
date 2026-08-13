package com.openatom.club.point.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class SearchPositionResult {
    private String keyword;
    private int matchCount;
    private int matchIndex;
    private Long memberId;
    private String name;
    private String studentNo;
    private Integer rankNo;
    private Integer pageNo;
    private Integer rowNoInPage;
    private BigDecimal totalScore;
}
