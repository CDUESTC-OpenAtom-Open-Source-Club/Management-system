package com.openatom.club.cohort.repository;

import com.openatom.club.cohort.entity.Cohort;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CohortRepository extends JpaRepository<Cohort, Long> {
    Optional<Cohort> findByIdAndDeletedAtIsNull(Long id);

    Optional<Cohort> findByYearAndDeletedAtIsNull(Integer year);

    boolean existsByYearAndDeletedAtIsNull(Integer year);

    boolean existsByYearAndDeletedAtIsNullAndIdNot(Integer year, Long id);

    List<Cohort> findAllByDeletedAtIsNullOrderByYearDesc();

    List<Cohort> findAllByDeletedAtIsNullAndEnabledTrueOrderByYearDesc();
}
