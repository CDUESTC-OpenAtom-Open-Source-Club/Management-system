package com.openatom.club.finance.dto;

import lombok.Data;
import java.util.List;

@Data
public class FinanceMonthDetailResponse {
    private FinancePeriodResponse period;
    private FinanceFileResponse report;
    private List<FinanceFileResponse> vouchers;
}
