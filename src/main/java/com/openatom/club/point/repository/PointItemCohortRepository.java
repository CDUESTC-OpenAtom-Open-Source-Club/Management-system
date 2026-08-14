package com.openatom.club.point.repository;

import com.openatom.club.point.entity.PointItemCohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PointItemCohortRepository extends JpaRepository<PointItemCohort, Long> {
    List<PointItemCohort> findByPointItemId(Long pointItemId);

    List<PointItemCohort> findByCohortId(Long cohortId);

    long countByCohortId(Long cohortId);

    void deleteByPointItemId(Long pointItemId);
}
