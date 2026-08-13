package com.openatom.club.point.service;

import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.point.dto.PointItemRequest;
import com.openatom.club.point.dto.PointItemResponse;
import com.openatom.club.point.entity.PointItem;
import com.openatom.club.point.repository.PointItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointItemService {
    private final PointItemRepository pointItemRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    public List<PointItemResponse> list() {
        return pointItemRepository.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc()
                .stream().map(PointItemResponse::from).toList();
    }

    public List<PointItemResponse> applyOptions() {
        return pointItemRepository.findAllByDeletedAtIsNullAndEnabledTrueAndAllowMemberApplyTrueOrderBySortOrderAscIdAsc()
                .stream().map(PointItemResponse::from).toList();
    }

    @Transactional
    public PointItemResponse create(PointItemRequest req) {
        permissionChecker.requireManage();
        PointItem item = new PointItem();
        item.setItemName(req.getItemName());
        item.setPointValue(req.getPointValue());
        item.setItemType(StringUtils.hasText(req.getItemType()) ? req.getItemType() : "其他");
        item.setDescription(req.getDescription());
        item.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        item.setEnabled(req.getEnabled() != null ? req.getEnabled() : true);
        item.setAllowMemberApply(req.getAllowMemberApply() != null ? req.getAllowMemberApply() : true);
        item.setCreatedBy(ActorHolder.get().getName());
        item.setUpdatedBy(ActorHolder.get().getName());
        PointItem saved = pointItemRepository.save(item);
        logService.log("point_item", "CREATE", String.valueOf(saved.getId()),
                "新增积分项目: " + saved.getItemName());
        return PointItemResponse.from(saved);
    }

    @Transactional
    public PointItemResponse update(Long id, PointItemRequest req) {
        permissionChecker.requireManage();
        PointItem item = pointItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("积分项目不存在"));
        item.setItemName(req.getItemName());
        item.setPointValue(req.getPointValue());
        if (StringUtils.hasText(req.getItemType())) item.setItemType(req.getItemType());
        if (req.getDescription() != null) item.setDescription(req.getDescription());
        if (req.getSortOrder() != null) item.setSortOrder(req.getSortOrder());
        if (req.getEnabled() != null) item.setEnabled(req.getEnabled());
        if (req.getAllowMemberApply() != null) item.setAllowMemberApply(req.getAllowMemberApply());
        item.setUpdatedBy(ActorHolder.get().getName());
        PointItem saved = pointItemRepository.save(item);
        logService.log("point_item", "UPDATE", String.valueOf(id),
                "修改积分项目: " + saved.getItemName());
        return PointItemResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.requireManage();
        PointItem item = pointItemRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("积分项目不存在"));
        item.setDeletedAt(OffsetDateTime.now());
        pointItemRepository.save(item);
        logService.log("point_item", "DELETE", String.valueOf(id),
                "删除积分项目: " + item.getItemName());
    }
}
