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

    @Query("SELECT h FROM HomeworkAssignment h WHERE h.deletedAt IS NULL " +
           "AND (:status IS NULL OR h.status = :status) " +
           "ORDER BY h.createdAt DESC")
    Page<HomeworkAssignment> findAllWithFilters(@Param("status") String status, Pageable pageable);
}
