package com.openatom.club.point.repository;

import com.openatom.club.point.entity.PointApplication;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface PointApplicationRepository extends JpaRepository<PointApplication, Long> {
    @Query("SELECT a FROM PointApplication a WHERE a.deletedAt IS NULL AND a.memberId = :memberId AND " +
           "a.pointItemId = :pointItemId AND a.status IN ('PENDING', 'APPROVED')")
    List<PointApplication> findActiveByMemberAndItem(@Param("memberId") Long memberId,
                                                      @Param("pointItemId") Long pointItemId);

    @Query("SELECT a FROM PointApplication a WHERE a.deletedAt IS NULL AND " +
           "(:status IS NULL OR a.status = :status) AND " +
           "(:keyword IS NULL OR EXISTS (SELECT m FROM com.openatom.club.member.entity.Member m WHERE m.id = a.memberId AND (m.name LIKE %:keyword% OR m.studentNo LIKE %:keyword%)) OR " +
           "EXISTS (SELECT p FROM com.openatom.club.point.entity.PointItem p WHERE p.id = a.pointItemId AND p.itemName LIKE %:keyword%))")
    Page<PointApplication> searchApplications(@Param("status") String status,
                                               @Param("keyword") String keyword,
                                               Pageable pageable);

    List<PointApplication> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    Optional<PointApplication> findByIdAndDeletedAtIsNull(Long id);

    long countByStatusAndDeletedAtIsNull(String status);

    long countByCreatedAtBetweenAndDeletedAtIsNull(java.time.OffsetDateTime start, java.time.OffsetDateTime end);
}
