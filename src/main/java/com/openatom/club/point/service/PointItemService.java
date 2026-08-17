package com.openatom.club.point.service;

import com.openatom.club.cohort.dto.CohortResponse;
import com.openatom.club.cohort.entity.Cohort;
import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.point.PointItemTypes;
import com.openatom.club.point.dto.PointItemRequest;
import com.openatom.club.point.dto.PointItemResponse;
import com.openatom.club.point.entity.PointItem;
import com.openatom.club.point.entity.PointItemCohort;
import com.openatom.club.point.repository.PointItemCohortRepository;
import com.openatom.club.point.repository.PointItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointItemService {
    private final PointItemRepository pointItemRepository;
    private final PointItemCohortRepository pointItemCohortRepository;
    private final CohortRepository cohortRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    public List<PointItemResponse> list() {
        Map<Long, Integer> years = cohortYearMap();
        return pointItemRepository.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc()
                .stream().map(i -> toResponse(i, years)).toList();
    }

    public List<PointItemResponse> applyOptions() {
        Long cohortId = ActorHolder.get().getCohortId();
        Map<Long, Integer> years = cohortYearMap();
        return pointItemRepository.findAllByDeletedAtIsNullAndEnabledTrueAndAllowMemberApplyTrueOrderBySortOrderAscIdAsc()
                .stream()
                .filter(i -> isApplicable(i, cohortId))
                .map(i -> toResponse(i, years)).toList();
    }

    @Transactional
    public PointItemResponse create(PointItemRequest req) {
        permissionChecker.requireManage();
        PointItem item = new PointItem();
        item.setItemName(req.getItemName());
        item.setPointValue(req.getPointValue());
        item.setItemType(normalizeItemType(req.getItemType()));
        item.setDescription(req.getDescription());
        item.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        item.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        item.setAllowMemberApply(req.getAllowMemberApply() != null ? req.getAllowMemberApply() : true);
        item.setCreatedBy(ActorHolder.get().getName());
        item.setUpdatedBy(ActorHolder.get().getName());
        PointItem saved = pointItemRepository.save(item);
        saveCohortLinks(saved.getId(), req.getCohortIds());
        logService.log("point_item", "CREATE", String.valueOf(saved.getId()),
                "新增积分项目: " + saved.getItemName());
        return toResponse(saved, cohortYearMap());
    }

    @Transactional
    public PointItemResponse update(Long id, PointItemRequest req) {
        permissionChecker.requireManage();
        PointItem item = pointItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("积分项目不存在"));
        item.setItemName(req.getItemName());
        item.setPointValue(req.getPointValue());
        if (StringUtils.hasText(req.getItemType())) item.setItemType(normalizeItemType(req.getItemType()));
        if (req.getDescription() != null) item.setDescription(req.getDescription());
        if (req.getSortOrder() != null) item.setSortOrder(req.getSortOrder());
        if (req.getEnabled() != null) item.setEnabled(req.getEnabled());
        if (req.getAllowMemberApply() != null) item.setAllowMemberApply(req.getAllowMemberApply());
        item.setUpdatedBy(ActorHolder.get().getName());
        PointItem saved = pointItemRepository.save(item);
        if (req.getCohortIds() != null) {
            saveCohortLinks(saved.getId(), req.getCohortIds());
        }
        logService.log("point_item", "UPDATE", String.valueOf(id),
                "修改积分项目: " + saved.getItemName());
        return toResponse(saved, cohortYearMap());
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.requireManage();
        PointItem item = pointItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("积分项目不存在"));
        item.setDeletedAt(OffsetDateTime.now());
        pointItemRepository.save(item);
        pointItemCohortRepository.deleteByPointItemId(id);
        logService.log("point_item", "DELETE", String.valueOf(id),
                "删除积分项目: " + item.getItemName());
    }

    private String normalizeItemType(String type) {
        String t = type == null ? "" : type.trim();
        if (t.isEmpty()) {
            return PointItemTypes.OTHER;
        }
        if (!PointItemTypes.isValid(t)) {
            throw BizException.of("非法的积分项目类型: " + t);
        }
        return t;
    }

    private boolean isApplicable(PointItem item, Long cohortId) {
        List<PointItemCohort> links = pointItemCohortRepository.findByPointItemId(item.getId());
        if (links.isEmpty()) return true; // 全局适用
        return cohortId != null && links.stream().anyMatch(l -> l.getCohortId().equals(cohortId));
    }

    private void saveCohortLinks(Long pointItemId, List<Long> cohortIds) {
        pointItemCohortRepository.deleteByPointItemId(pointItemId);
        if (cohortIds != null) {
            for (Long cohortId : cohortIds) {
                PointItemCohort link = new PointItemCohort();
                link.setPointItemId(pointItemId);
                link.setCohortId(cohortId);
                pointItemCohortRepository.save(link);
            }
        }
    }

    private PointItemResponse toResponse(PointItem item, Map<Long, Integer> years) {
        PointItemResponse dto = PointItemResponse.from(item);
        List<CohortResponse> cohorts = pointItemCohortRepository.findByPointItemId(item.getId()).stream().map(l -> {
            CohortResponse c = new CohortResponse();
            c.setId(l.getCohortId());
            c.setYear(years.get(l.getCohortId()));
            c.setEnabled(true);
            return c;
        }).toList();
        dto.setCohorts(cohorts);
        return dto;
    }

    private Map<Long, Integer> cohortYearMap() {
        return cohortRepository.findAllByDeletedAtIsNullOrderByYearDesc().stream()
                .collect(Collectors.toMap(Cohort::getId, Cohort::getYear));
    }
}
