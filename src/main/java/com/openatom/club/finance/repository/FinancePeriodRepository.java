package com.openatom.club.finance.repository;

import com.openatom.club.finance.entity.FinancePeriod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FinancePeriodRepository extends JpaRepository<FinancePeriod, Long> {
    List<FinancePeriod> findAllByFinanceYearOrderByFinanceMonthAsc(Integer year);
    List<FinancePeriod> findAllByOrderByFinanceYearDescFinanceMonthDesc();
    Optional<FinancePeriod> findByFinanceYearAndFinanceMonth(Integer year, Integer month);
}
