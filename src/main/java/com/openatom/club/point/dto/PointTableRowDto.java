package com.openatom.club.point.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class PointTableRowDto {
    private Integer rankNo;
    private Long memberId;
    private String name;
    private String studentNo;

    private BigDecimal totalScore;

    // 按 PointItem.type 聚合的六大分类积分（顺序固定）
    private BigDecimal activityScore;
    private BigDecimal competitionScore;
    private BigDecimal openSourceLearningScore;
    private BigDecimal communityContributionScore;
    private BigDecimal speechHostingScore;
    private BigDecimal otherScore;
}
