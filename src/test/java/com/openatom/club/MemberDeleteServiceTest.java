package com.openatom.club;

import com.openatom.club.auth.entity.UserAccount;
import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.exception.PermissionDeniedException;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.homework.entity.HomeworkSubmission;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.dto.BatchDeleteMembersResponse;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.member.service.MemberService;
import com.openatom.club.point.entity.PointApplication;
import com.openatom.club.point.entity.PointRecord;
import com.openatom.club.point.repository.PointApplicationRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.*;

/**
 * 成员删除生命周期单元测试（Mockito，不依赖 Spring 上下文 / 数据库）。
 * 覆盖：权限矩阵、本人保护、积分三来源清理、账号禁用、事务整批失败、去重、无账号成员。
 */
@ExtendWith(MockitoExtension.class)
class MemberDeleteServiceTest {

    @Mock MemberRepository memberRepository;
    @Mock CohortRepository cohortRepository;
    @Mock PointRecordRepository pointRecordRepository;
    @Mock PointApplicationRepository pointApplicationRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock HomeworkSubmissionRepository homeworkSubmissionRepository;
    @Mock OperationLogService logService;

    private final PermissionChecker permissionChecker = new PermissionChecker();
    private MemberService memberService;

    @BeforeEach
    void setUp() {
        memberService = new MemberService(
                memberRepository, cohortRepository, pointRecordRepository,
                pointApplicationRepository, userAccountRepository, homeworkSubmissionRepository,
                permissionChecker, logService);
        setActor("林涛", "秘书处", "副会长", 100L);
    }

    @AfterEach
    void clearActor() {
        ActorHolder.clear();
    }

    private void setActor(String name, String department, String position, Long memberId) {
        ActorContext actor = new ActorContext(name, department, position);
        actor.setUserId(memberId);
        actor.setMemberId(memberId);
        ActorHolder.set(actor);
    }

    private Member member(Long id, String name) {
        Member m = new Member();
        m.setId(id);
        m.setName(name);
        m.setStudentNo("S" + id);
        m.setPosition("社员");
        return m;
    }

    // ============ 权限矩阵 ============

    @Test
    void ministerCannotDelete() {
        setActor("张部长", "技术部", "部长", 200L);
        assertThrows(PermissionDeniedException.class,
                () -> memberService.batchDelete(List.of(1L)));
        verify(memberRepository, never()).findAllByIdInAndDeletedAtIsNull(anyList());
    }

    @Test
    void ordinaryMemberCannotDelete() {
        setActor("李四", "技术部", "社员", 300L);
        assertThrows(PermissionDeniedException.class, () -> memberService.delete(1L));
        verify(memberRepository, never()).findAllByIdInAndDeletedAtIsNull(anyList());
    }

    // ============ 本人保护 ============

    @Test
    void cannotDeleteSelf_batch() {
        // actor.memberId = 100L
        Member self = member(100L, "自己");
        when(memberRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(self));

        assertThrows(PermissionDeniedException.class,
                () -> memberService.batchDelete(List.of(100L)));

        verify(memberRepository, never()).saveAll(anyIterable());
        verify(pointRecordRepository, never()).saveAll(anyIterable());
    }

    @Test
    void cannotDeleteSelf_batchContainingSelf() {
        // [7, 100, 8] 包含本人 → 整批拒绝，7/8 也不删
        Member m7 = member(7L, "七");
        Member m100 = member(100L, "自己");
        Member m8 = member(8L, "八");
        when(memberRepository.findAllByIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(m7, m100, m8));

        assertThrows(PermissionDeniedException.class,
                () -> memberService.batchDelete(List.of(7L, 100L, 8L)));

        verify(memberRepository, never()).saveAll(anyIterable());
        assertNull(m7.getDeletedAt());
        assertNull(m8.getDeletedAt());
    }

    // ============ 校验：空 / 重复 / 无效 ============

    @Test
    void emptyIds_rejected() {
        assertThrows(BizException.class, () -> memberService.batchDelete(List.of()));
        verify(memberRepository, never()).findAllByIdInAndDeletedAtIsNull(anyList());
    }

    @Test
    void invalidId_wholeBatchFails() {
        Member a = member(1L, "张三");
        // 请求 [1, 999999]，但只查到 1 → 整批失败，不执行任何删除
        when(memberRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(a));

        assertThrows(BizException.class, () -> memberService.batchDelete(List.of(1L, 999999L)));

        verify(pointRecordRepository, never()).saveAll(anyIterable());
        verify(pointApplicationRepository, never()).saveAll(anyIterable());
        verify(userAccountRepository, never()).saveAll(anyIterable());
        verify(memberRepository, never()).saveAll(anyIterable());
        assertNull(a.getDeletedAt());
    }

    @Test
    void duplicateIds_deduped() {
        Member a = member(1L, "张三");
        when(memberRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(a));

        BatchDeleteMembersResponse result = memberService.batchDelete(List.of(1L, 1L));

        assertEquals(1, result.getDeletedMemberCount());
    }

    // ============ 核心：积分三来源清理 + 账号禁用 + 作业引用置空 ============

    @Test
    void batchDelete_cleansAllPointSourcesAndDisablesAccount() {
        Member a = member(1L, "张三");
        Member b = member(2L, "李四");

        PointRecord recApp = new PointRecord();
        recApp.setId(10L); recApp.setMemberId(1L); recApp.setSourceType("APPLICATION"); recApp.setScore(new BigDecimal("10"));
        PointRecord recManual = new PointRecord();
        recManual.setId(11L); recManual.setMemberId(1L); recManual.setSourceType("MANUAL"); recManual.setScore(new BigDecimal("5"));
        PointRecord recHomework = new PointRecord();
        recHomework.setId(12L); recHomework.setMemberId(1L); recHomework.setSourceType("HOMEWORK"); recHomework.setScore(new BigDecimal("8"));

        PointApplication appPending = new PointApplication();
        appPending.setId(20L); appPending.setMemberId(1L); appPending.setStatus("PENDING");
        PointApplication appApproved = new PointApplication();
        appApproved.setId(21L); appApproved.setMemberId(1L); appApproved.setStatus("APPROVED");
        PointApplication appRejected = new PointApplication();
        appRejected.setId(22L); appRejected.setMemberId(1L); appRejected.setStatus("REJECTED");

        UserAccount account = new UserAccount();
        account.setId(30L); account.setMemberId(1L); account.setEnabled(true);

        HomeworkSubmission submission = new HomeworkSubmission();
        submission.setId(40L); submission.setMemberId(1L); submission.setPointRecordId(12L);

        when(memberRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(a, b));
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(recApp, recManual, recHomework));
        when(pointApplicationRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(appPending, appApproved, appRejected));
        when(userAccountRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(account));
        when(homeworkSubmissionRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(submission));

        BatchDeleteMembersResponse result = memberService.batchDelete(List.of(1L, 2L));

        // 计数正确
        assertEquals(2, result.getDeletedMemberCount());
        assertEquals(3, result.getDeletedPointApplicationCount());
        assertEquals(3, result.getDeletedPointRecordCount());
        assertEquals(1, result.getDisabledAccountCount());

        // 三种来源的积分记录全部软删除
        assertNotNull(recApp.getDeletedAt(), "APPLICATION 积分应删除");
        assertNotNull(recManual.getDeletedAt(), "MANUAL 积分应删除");
        assertNotNull(recHomework.getDeletedAt(), "HOMEWORK 积分应删除");

        // 三种状态的申请全部软删除
        assertNotNull(appPending.getDeletedAt());
        assertNotNull(appApproved.getDeletedAt());
        assertNotNull(appRejected.getDeletedAt());

        // 账号禁用
        assertFalse(account.getEnabled());

        // 作业提交的积分引用置空（避免悬空引用）
        assertNull(submission.getPointRecordId());

        // 成员软删除
        assertNotNull(a.getDeletedAt());
        assertNotNull(b.getDeletedAt());
    }

    @Test
    void singleDelete_sharesSameLifecycle() {
        Member a = member(1L, "张三");
        PointRecord rec = new PointRecord();
        rec.setId(10L); rec.setMemberId(1L); rec.setSourceType("MANUAL");
        PointApplication app = new PointApplication();
        app.setId(20L); app.setMemberId(1L); app.setStatus("PENDING");
        UserAccount account = new UserAccount();
        account.setId(30L); account.setMemberId(1L); account.setEnabled(true);

        when(memberRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(a));
        when(pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(rec));
        when(pointApplicationRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(app));
        when(userAccountRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(account));
        when(homeworkSubmissionRepository.findAllByMemberIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of());

        memberService.delete(1L);

        assertNotNull(a.getDeletedAt());
        assertNotNull(rec.getDeletedAt());
        assertNotNull(app.getDeletedAt());
        assertFalse(account.getEnabled());
    }

    // ============ 边界：无账号成员可删除 ============

    @Test
    void memberWithoutAccount_deletesFine() {
        Member a = member(5L, "无账号成员");
        when(memberRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(a));
        // 其余仓库默认返回空集合（Mockito 对集合返回类型默认返回空集合）

        BatchDeleteMembersResponse result = memberService.batchDelete(List.of(5L));

        assertEquals(1, result.getDeletedMemberCount());
        assertEquals(0, result.getDisabledAccountCount());
        assertNotNull(a.getDeletedAt());
    }

    // ============ DTO 形状 ============

    @Test
    void batchDeleteRequestDtoOnlyCarriesMemberIds() {
        Set<String> fields = Arrays.stream(
                        com.openatom.club.member.dto.BatchDeleteMembersRequest.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertTrue(fields.contains("memberIds"));
        assertEquals(1, fields.size(), "批量删除请求 DTO 只应包含 memberIds");
    }

    @Test
    void batchDeleteResponseCarriesCounts() {
        Set<String> fields = Arrays.stream(
                        com.openatom.club.member.dto.BatchDeleteMembersResponse.class.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
        assertTrue(fields.contains("deletedMemberCount"));
        assertTrue(fields.contains("deletedPointApplicationCount"));
        assertTrue(fields.contains("deletedPointRecordCount"));
        assertTrue(fields.contains("disabledAccountCount"));
    }
}
