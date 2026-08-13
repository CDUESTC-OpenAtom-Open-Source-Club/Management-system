package com.openatom.club.finance.dto;

import com.openatom.club.finance.entity.FinancePeriod;
import lombok.Data;
import java.time.OffsetDateTime;

@Data
public class FinancePeriodResponse {
    private Long id;
    private Integer financeYear;
    private Integer financeMonth;
    private String remark;
    private OffsetDateTime createdAt;

    public static FinancePeriodResponse from(FinancePeriod p) {
        FinancePeriodResponse dto = new FinancePeriodResponse();
        dto.id = p.getId();
        dto.financeYear = p.getFinanceYear();
        dto.financeMonth = p.getFinanceMonth();
        dto.remark = p.getRemark();
        dto.createdAt = p.getCreatedAt();
        return dto;
    }
}
