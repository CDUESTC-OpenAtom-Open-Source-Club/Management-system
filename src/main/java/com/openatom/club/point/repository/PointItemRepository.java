package com.openatom.club.point.repository;

import com.openatom.club.point.entity.PointItem;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface PointItemRepository extends JpaRepository<PointItem, Long> {
    List<PointItem> findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc();
    List<PointItem> findAllByDeletedAtIsNullAndEnabledTrueAndAllowMemberApplyTrueOrderBySortOrderAscIdAsc();
    Optional<PointItem> findByIdAndDeletedAtIsNull(Long id);

    long countByDeletedAtIsNull();
}
