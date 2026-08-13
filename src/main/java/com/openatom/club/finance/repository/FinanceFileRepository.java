package com.openatom.club.finance.repository;

import com.openatom.club.finance.entity.FinanceFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface FinanceFileRepository extends JpaRepository<FinanceFile, Long> {
    List<FinanceFile> findAllByPeriodIdAndDeletedAtIsNull(Long periodId);
    Optional<FinanceFile> findByPeriodIdAndFileTypeAndDeletedAtIsNull(Long periodId, String fileType);
    Optional<FinanceFile> findByIdAndDeletedAtIsNull(Long id);
}
