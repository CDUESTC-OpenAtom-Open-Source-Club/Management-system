package com.openatom.club.homework.repository;

import com.openatom.club.homework.entity.HomeworkAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface HomeworkAssignmentRepository extends JpaRepository<HomeworkAssignment, Long> {

    Optional<HomeworkAssignment> findByIdAndDeletedAtIsNull(Long id);

    long countByCohortId(Long cohortId);

    @Query("SELECT h FROM HomeworkAssignment h WHERE h.deletedAt IS NULL " +
           "AND (:status IS NULL OR h.status = :status) " +
           "AND (:cohortId IS NULL OR h.cohortId = :cohortId) " +
           "AND (:department IS NULL OR h.targetDepartment = :department) " +
           "ORDER BY h.createdAt DESC")
    Page<HomeworkAssignment> findAllWithFilters(@Param("status") String status,
                                                 @Param("cohortId") Long cohortId,
                                                 @Param("department") String department,
                                                 Pageable pageable);
}
