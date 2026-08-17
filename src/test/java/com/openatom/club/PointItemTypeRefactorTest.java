package com.openatom.club;

import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.point.PointItemTypes;
import com.openatom.club.point.dto.PointItemRequest;
import com.openatom.club.point.dto.PointItemResponse;
import com.openatom.club.point.entity.PointItem;
import com.openatom.club.point.repository.PointItemCohortRepository;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.service.PointItemService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 积分项目类型重构测试：
 * - 统一合法类型常量（顺序 + 合法性）
 * - PointItemService 新增/编辑时拒绝旧类型与非法类型
 */
@ExtendWith(MockitoExtension.class)
class PointItemTypeRefactorTest {

    @Mock PointItemRepository pointItemRepository;
    @Mock PointItemCohortRepository pointItemCohortRepository;
    @Mock CohortRepository cohortRepository;
    @Mock OperationLogService logService;

    private final PermissionChecker permissionChecker = new PermissionChecker();
    private PointItemService pointItemService;

    @BeforeEach
    void setUp() {
        pointItemService = new PointItemService(
                pointItemRepository, pointItemCohortRepository, cohortRepository,
                permissionChecker, logService);
        ActorContext actor = new ActorContext("林涛", "秘书处", "副会长");
        actor.setUserId(1L);
        actor.setMemberId(1L);
        ActorHolder.set(actor);
    }

    @AfterEach
    void clearActor() {
        ActorHolder.clear();
    }

    private PointItemRequest req(String type) {
        PointItemRequest r = new PointItemRequest();
        r.setItemName("测试项目");
        r.setPointValue(new BigDecimal("10"));
        r.setItemType(type);
        r.setEnabled(true);
        r.setAllowMemberApply(true);
        r.setSortOrder(0);
        r.setCohortIds(null);
        return r;
    }

    // ============ 统一常量 ============

    @Test
    void allTypes_inStrictOrder() {
        assertEquals(
                List.of("活动", "比赛", "开源学习", "社区贡献", "演讲或主持", "其他"),
                PointItemTypes.ALL);
    }

    @Test
    void validTypes_accepted() {
        for (String t : PointItemTypes.ALL) {
            assertTrue(PointItemTypes.isValid(t), t + " 应为合法类型");
        }
    }

    @Test
    void legacyTypes_rejected() {
        assertFalse(PointItemTypes.isValid("会议"));
        assertFalse(PointItemTypes.isValid("任务"));
    }

    @Test
    void unknownAndBlank_rejected() {
        assertFalse(PointItemTypes.isValid("UNKNOWN"));
        assertFalse(PointItemTypes.isValid(null));
        assertFalse(PointItemTypes.isValid(""));
        assertFalse(PointItemTypes.isValid("  "));
    }

    // ============ 新增 / 编辑 ============

    @Test
    void create_withNewType_succeeds() {
        when(pointItemRepository.save(any(PointItem.class))).thenAnswer(inv -> {
            PointItem p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        PointItemResponse resp = pointItemService.create(req("开源学习"));

        assertEquals("开源学习", resp.getItemType());
        verify(pointItemRepository).save(any(PointItem.class));
    }

    @Test
    void update_withNewType_succeeds() {
        PointItem existing = new PointItem();
        existing.setId(5L);
        existing.setItemName("旧名称");
        existing.setPointValue(new BigDecimal("5"));
        existing.setItemType("活动");
        when(pointItemRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(existing));
        when(pointItemRepository.save(any(PointItem.class))).thenAnswer(inv -> inv.getArgument(0));

        PointItemResponse resp = pointItemService.update(5L, req("社区贡献"));

        assertEquals("社区贡献", resp.getItemType());
    }

    @Test
    void create_withLegacyType_rejected() {
        assertThrows(BizException.class, () -> pointItemService.create(req("会议")));
        verify(pointItemRepository, never()).save(any(PointItem.class));
    }

    @Test
    void update_withLegacyType_rejected() {
        PointItem existing = new PointItem();
        existing.setId(5L);
        existing.setItemType("活动");
        when(pointItemRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(existing));

        assertThrows(BizException.class, () -> pointItemService.update(5L, req("任务")));
        verify(pointItemRepository, never()).save(any(PointItem.class));
    }

    @Test
    void create_withUnknownType_rejected() {
        assertThrows(BizException.class, () -> pointItemService.create(req("UNKNOWN")));
        verify(pointItemRepository, never()).save(any(PointItem.class));
    }

    @Test
    void create_withBlankType_defaultsToOther() {
        when(pointItemRepository.save(any(PointItem.class))).thenAnswer(inv -> {
            PointItem p = inv.getArgument(0);
            p.setId(2L);
            return p;
        });

        PointItemResponse resp = pointItemService.create(req(null));

        assertEquals("其他", resp.getItemType());
    }
}
