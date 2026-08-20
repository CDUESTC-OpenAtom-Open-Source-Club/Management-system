package com.openatom.club.point.repository;

import com.openatom.club.point.entity.PointRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public interface PointRecordRepository extends JpaRepository<PointRecord, Long> {
    List<PointRecord> findAllByMemberIdAndDeletedAtIsNull(Long memberId);

    List<PointRecord> findAllByMemberIdInAndDeletedAtIsNull(List<Long> memberIds);

    Page<PointRecord> findByMemberIdAndDeletedAtIsNull(Long memberId, Pageable pageable);

    Optional<PointRecord> findByIdAndDeletedAtIsNull(Long id);

    List<PointRecord> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    @Query("SELECT COALESCE(SUM(r.score), 0) FROM PointRecord r WHERE r.memberId = :memberId AND r.deletedAt IS NULL")
    BigDecimal sumScoreByMember(@Param("memberId") Long memberId);
}
