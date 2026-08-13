package com.openatom.club.log.repository;

import com.openatom.club.log.entity.OperationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OperationLogRepository extends JpaRepository<OperationLog, Long> {
    @Query("SELECT l FROM OperationLog l WHERE " +
           "(:moduleName IS NULL OR l.moduleName = :moduleName) AND " +
           "(:actionType IS NULL OR l.actionType = :actionType) AND " +
           "(:keyword IS NULL OR l.operatorName LIKE %:keyword% OR l.description LIKE %:keyword%)")
    Page<OperationLog> search(@Param("moduleName") String moduleName,
                               @Param("actionType") String actionType,
                               @Param("keyword") String keyword,
                               Pageable pageable);

    @Query("SELECT l FROM OperationLog l ORDER BY l.createdAt DESC")
    List<OperationLog> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
