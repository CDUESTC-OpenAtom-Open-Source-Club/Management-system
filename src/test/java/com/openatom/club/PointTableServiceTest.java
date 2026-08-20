package com.openatom.club;

import com.openatom.club.common.exception.PermissionDeniedException;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.dto.PointDetailResponse;
import com.openatom.club.point.dto.PointTableResult;
import com.openatom.club.point.dto.PointTableRowDto;
import com.openatom.club.point.entity.PointItem;
import com.openatom.club.point.entity.PointRecord;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import com.openatom.club.point.service.PointTableService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

/**
 * 积分总表重构单元测试（Mockito，不依赖 Spring 上下文 / 数据库）。
 * 覆盖：按 PointItem.type 六类聚合、负积分、排名、届次隔离、明细权限、来源映射。
 */
@ExtendWith(MockitoExtension.class)
class PointTableServiceTest {

    @Mock PointItemRepository pointItemRepository;
    @Mock PointRecordRepository pointRecordRepository;
    @Mock MemberRepository memberRepository;

    private final PermissionChecker permissionChecker = new PermissionChecker();
    private PointTableService pointTableService;

    @BeforeEach
    void setUp() {
        pointTableService = new PointTableService(
                pointItemRepository, pointRecordRepository, memberRepository, permissionChecker);
        // 默认 fullAccess（副会长 + 秘书处）
        ActorContext actor = new ActorContext("林涛", "秘书处", "副会长");
        actor.setUserId(1L);
        actor.setMemberId(1L);
        ActorHolder.set(actor);
    }

    @AfterEach
    void clearActor() {
        ActorHolder.clear();
    }

    // ==================== 辅助构造 ====================

    private Member member(Long id, String name) {
        return member(id, name, 1L);
    }

    private Member member(Long id, String name, Long cohortId) {
        Member m = new Member();
        m.setId(id);
        m.setName(name);
        m.setStudentNo("S" + id);
        m.setCohortId(cohortId);
        return m;
    }

    private PointItem item(Long id, String name, String type) {
        PointItem p = new PointItem();
        p.setId(id);
        p.setItemName(name);
        p.setItemType(type);
        p.setPointValue(new BigDecimal("1"));
        return p;
    }

    private PointRecord rec(Long id, Long memberId, Long pointItemId, String score) {
        PointRecord r = new PointRecord();
        r.setId(id);
        r.setMemberId(memberId);
        r.setPointItemId(pointItemId);
        r.setScore(new BigDecimal(score));
        r.setSourceType("MANUAL");
        return r;
    }

    private void stubItems(PointItem... items) {
        when(pointItemRepository.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc())
                .thenReturn(List.of(items));
    }

    // ==================== 分类聚合 ====================

    @Test
    void table_aggregatesByItemType() {
        Member a = member(1L, "张三");
        PointItem act = item(1L, "志愿服务", "活动");
        PointItem comp = item(2L, "蓝桥杯", "比赛");
        PointItem os = item(3L, "Git学习", "开源学习");
        PointItem comm = item(4L, "仓库维护", "社区贡献");
        PointItem speech = item(5L, "年度总结主持", "演讲或主持");
        PointItem other = item(6L, "其他项目", "其他");

        when(memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(a));
        stubItems(act, comp, os, comm, speech, other);
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(
                rec(1L, 1L, act.getId(), "10"),
                rec(2L, 1L, act.getId(), "5"),
                rec(3L, 1L, comp.getId(), "20"),
                rec(4L, 1L, os.getId(), "8"),
                rec(5L, 1L, comm.getId(), "6"),
                rec(6L, 1L, speech.getId(), "4"),
                rec(7L, 1L, other.getId(), "2")));

        PointTableResult result = pointTableService.getTable(1, 50, null, 1L);

        assertEquals(1, result.getTotal());
        PointTableRowDto row = result.getRows().get(0);
        assertDecimal("15", row.getActivityScore());
        assertDecimal("20", row.getCompetitionScore());
        assertDecimal("8", row.getOpenSourceLearningScore());
        assertDecimal("6", row.getCommunityContributionScore());
        assertDecimal("4", row.getSpeechHostingScore());
        assertDecimal("2", row.getOtherScore());
        assertDecimal("55", row.getTotalScore());
    }

    @Test
    void table_sumsNegativeScores() {
        Member a = member(1L, "张三");
        PointItem act = item(1L, "志愿服务", "活动");

        when(memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(a));
        stubItems(act);
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(
                rec(1L, 1L, act.getId(), "10"),
                rec(2L, 1L, act.getId(), "-3")));

        PointTableResult result = pointTableService.getTable(1, 50, null, 1L);

        PointTableRowDto row = result.getRows().get(0);
        assertDecimal("7", row.getActivityScore());
        assertDecimal("7", row.getTotalScore());
    }

    // ==================== 排名 ====================

    @Test
    void table_ranksByTotalDesc() {
        Member a = member(1L, "A");
        Member b = member(2L, "B");
        Member c = member(3L, "C");
        PointItem act = item(1L, "活动项目", "活动");

        when(memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(1L))
                .thenReturn(List.of(a, b, c));
        stubItems(act);
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(
                rec(1L, a.getId(), act.getId(), "50"),
                rec(2L, b.getId(), act.getId(), "120"),
                rec(3L, c.getId(), act.getId(), "80")));

        PointTableResult result = pointTableService.getTable(1, 50, null, 1L);

        List<PointTableRowDto> rows = result.getRows();
        assertEquals("B", rows.get(0).getName());
        assertEquals(1, rows.get(0).getRankNo());
        assertEquals("C", rows.get(1).getName());
        assertEquals("A", rows.get(2).getName());
        assertDecimal("120", rows.get(0).getTotalScore());
        assertDecimal("80", rows.get(1).getTotalScore());
        assertDecimal("50", rows.get(2).getTotalScore());
    }

    @Test
    void table_tieBreakByNameAsc() {
        Member a = member(1L, "李四");
        Member b = member(2L, "张三");
        PointItem act = item(1L, "活动项目", "活动");

        when(memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(1L))
                .thenReturn(List.of(a, b));
        stubItems(act);
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(
                rec(1L, a.getId(), act.getId(), "50"),
                rec(2L, b.getId(), act.getId(), "50")));

        PointTableResult result = pointTableService.getTable(1, 50, null, 1L);

        assertEquals("张三", result.getRows().get(0).getName());
        assertEquals("李四", result.getRows().get(1).getName());
    }

    // ==================== 届次隔离 ====================

    @Test
    void table_cohortIsolation() {
        Member a2025 = member(1L, "A", 5L);
        Member b2026 = member(2L, "B", 6L);
        PointItem act = item(1L, "活动项目", "活动");

        when(memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(5L)).thenReturn(List.of(a2025));
        when(memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(6L)).thenReturn(List.of(b2026));
        stubItems(act);
        List<PointRecord> all = List.of(
                rec(1L, 1L, act.getId(), "100"),
                rec(2L, 2L, act.getId(), "80"));
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenAnswer(inv -> {
            List<Long> ids = inv.getArgument(0);
            return all.stream().filter(r -> ids.contains(r.getMemberId())).toList();
        });

        PointTableResult r2026 = pointTableService.getTable(1, 50, null, 6L);
        assertEquals(1, r2026.getTotal());
        assertEquals("B", r2026.getRows().get(0).getName());

        PointTableResult r2025 = pointTableService.getTable(1, 50, null, 5L);
        assertEquals(1, r2025.getTotal());
        assertEquals("A", r2025.getRows().get(0).getName());
    }

    // ==================== 异常数据兼容 ====================

    @Test
    void table_nullPointItemId_countsAsOther() {
        Member a = member(1L, "张三");
        when(memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(a));
        stubItems(); // 无任何项目
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(
                rec(1L, 1L, null, "5")));

        PointTableResult result = pointTableService.getTable(1, 50, null, 1L);

        PointTableRowDto row = result.getRows().get(0);
        assertDecimal("5", row.getOtherScore());
        assertDecimal("5", row.getTotalScore());
        assertDecimal("0", row.getActivityScore());
    }

    @Test
    void table_unknownType_countsAsOther() {
        Member a = member(1L, "张三");
        PointItem legacy = item(1L, "历史会议", "会议"); // 非法类型
        when(memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(1L)).thenReturn(List.of(a));
        stubItems(legacy);
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(
                rec(1L, 1L, legacy.getId(), "5")));

        PointTableResult result = pointTableService.getTable(1, 50, null, 1L);

        assertDecimal("5", result.getRows().get(0).getOtherScore());
    }

    @Test
    void table_zeroScoreMember_included() {
        Member a = member(1L, "A");
        Member b = member(2L, "B");
        PointItem act = item(1L, "活动项目", "活动");
        when(memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(1L))
                .thenReturn(List.of(a, b));
        stubItems(act);
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(
                rec(1L, a.getId(), act.getId(), "10")));

        PointTableResult result = pointTableService.getTable(1, 50, null, 1L);

        assertEquals(2, result.getTotal());
        PointTableRowDto bRow = result.getRows().stream()
                .filter(r -> r.getMemberId().equals(2L)).findFirst().orElseThrow();
        assertDecimal("0", bRow.getTotalScore());
        assertDecimal("0", bRow.getOtherScore());
    }

    // ==================== 明细权限 ====================

    @Test
    void detail_requiresFullAccess_normalMemberForbidden() {
        ActorContext actor = new ActorContext("普通", "技术部", "社员");
        actor.setUserId(2L);
        actor.setMemberId(2L);
        ActorHolder.set(actor);

        assertThrows(PermissionDeniedException.class,
                () -> pointTableService.listMemberDetails(1L, 1, 10));
    }

    @Test
    void detail_requiresFullAccess_ministerForbidden() {
        ActorContext actor = new ActorContext("部长", "技术部", "部长");
        actor.setUserId(2L);
        actor.setMemberId(2L);
        ActorHolder.set(actor);

        assertThrows(PermissionDeniedException.class,
                () -> pointTableService.listMemberDetails(1L, 1, 10));
    }

    @Test
    void detail_fullAccess_success() {
        Member a = member(1L, "张三");
        PointItem item = item(1L, "蓝桥杯", "比赛");
        PointRecord r = rec(1L, 1L, item.getId(), "20");
        r.setSourceType("APPLICATION");
        r.setReason("一等奖");

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));
        when(pointRecordRepository.findByMemberIdAndDeletedAtIsNull(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(r), PageRequest.of(0, 10), 1));
        when(pointItemRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(item));

        PageResult<PointDetailResponse> result = pointTableService.listMemberDetails(1L, 1, 10);

        assertEquals(1, result.getTotal());
        PointDetailResponse d = result.getList().get(0);
        assertEquals("蓝桥杯", d.getPointItemName());
        assertEquals("比赛", d.getPointItemType());
        assertEquals("活动登记", d.getSourceLabel());
        assertEquals("一等奖", d.getReason());
    }

    @Test
    void detail_mapsAllSources() {
        Member a = member(1L, "张三");
        PointItem item = item(1L, "Git实践", "开源学习");
        PointRecord app = rec(1L, 1L, item.getId(), "20");
        app.setSourceType("APPLICATION");
        PointRecord manual = rec(2L, 1L, item.getId(), "5");
        manual.setSourceType("MANUAL");
        PointRecord hw = rec(3L, 1L, item.getId(), "8");
        hw.setSourceType("HOMEWORK");

        when(memberRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));
        when(pointRecordRepository.findByMemberIdAndDeletedAtIsNull(eq(1L), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(app, manual, hw), PageRequest.of(0, 10), 3));
        when(pointItemRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(item));

        PageResult<PointDetailResponse> result = pointTableService.listMemberDetails(1L, 1, 10);

        assertEquals(3, result.getTotal());
        assertEquals("活动登记", result.getList().get(0).getSourceLabel());
        assertEquals("手工调整", result.getList().get(1).getSourceLabel());
        assertEquals("作业", result.getList().get(2).getSourceLabel());
        assertTrue(result.getList().stream().allMatch(d -> "Git实践".equals(d.getPointItemName())));
        assertTrue(result.getList().stream().allMatch(d -> "开源学习".equals(d.getPointItemType())));
    }

    // ==================== 工具 ====================

    private void assertDecimal(String expected, BigDecimal actual) {
        assertNotNull(actual, "分数不应为 null");
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "期望 " + expected + " 实际 " + actual);
    }
}
