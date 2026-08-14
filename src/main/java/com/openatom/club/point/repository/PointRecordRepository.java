package com.openatom.club.point.repository;

import com.openatom.club.point.entity.PointRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PointRecordRepository extends JpaRepository<PointRecord, Long> {
    List<PointRecord> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    List<PointRecord> findAllByMemberIdInAndDeletedAtIsNull(List<Long> memberIds);

    Optional<PointRecord> findByIdAndDeletedAtIsNull(Long id);

    @Query("SELECT r.memberId, COALESCE(SUM(r.score), 0) FROM PointRecord r WHERE r.deletedAt IS NULL GROUP BY r.memberId")
    List<Object[]> sumScoreGroupByMember();

    @Query("SELECT r.memberId, r.pointItemId, COALESCE(SUM(r.score), 0) FROM PointRecord r WHERE r.deletedAt IS NULL AND r.pointItemId IS NOT NULL GROUP BY r.memberId, r.pointItemId")
    List<Object[]> sumScoreGroupByMemberAndItem();

    @Query("SELECT COALESCE(SUM(r.score), 0) FROM PointRecord r WHERE r.memberId = :memberId AND r.deletedAt IS NULL")
    BigDecimal sumScoreByMember(@Param("memberId") Long memberId);
}
