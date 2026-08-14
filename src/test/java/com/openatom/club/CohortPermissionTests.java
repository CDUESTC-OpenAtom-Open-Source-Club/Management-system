package com.openatom.club;

import com.openatom.club.auth.dto.CreateUserRequest;
import com.openatom.club.auth.dto.UpdateMyProfileRequest;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.homework.dto.HomeworkAssignmentRequest;
import com.openatom.club.member.dto.MemberRequest;
import com.openatom.club.point.dto.PointApplicationSubmitRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 届次重构相关的权限与 DTO 安全形状单元测试。
 * 纯单元测试，不依赖 Spring 上下文 / 数据库。
 */
class CohortPermissionTests {

    private final PermissionChecker checker = new PermissionChecker();

    @AfterEach
    void clearActor() {
        ActorHolder.clear();
    }

    private static Set<String> fieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
                .map(Field::getName)
                .collect(Collectors.toSet());
    }

    private void setActor(String name, String department, String position, Long cohortId, Integer cohortYear) {
        ActorContext actor = new ActorContext(name, department, position);
        actor.setUserId(1L);
        actor.setUsername("tester");
        actor.setMemberId(1L);
        actor.setCohortId(cohortId);
        actor.setCohortYear(cohortYear);
        ActorHolder.set(actor);
    }

    // ============ 组织身份 DTO 形状（杜绝普通成员自提权） ============

    @Test
    void profileDtoMustNotContainOrganizationFields() {
        Set<String> fields = fieldNames(UpdateMyProfileRequest.class);
        assertFalse(fields.contains("department"), "个人资料 DTO 不能包含 department");
        assertFalse(fields.contains("position"), "个人资料 DTO 不能包含 position");
        assertFalse(fields.contains("cohortId"), "个人资料 DTO 不能包含 cohortId");
        assertTrue(fields.contains("name"));
        assertTrue(fields.contains("studentNo"));
        assertTrue(fields.contains("phone"));
        assertTrue(fields.contains("major"));
    }

    @Test
    void pointApplicationSubmitDtoMustNotContainMemberId() {
        Set<String> fields = fieldNames(PointApplicationSubmitRequest.class);
        assertFalse(fields.contains("memberId"), "登记 DTO 不能包含客户端可指定的 memberId");
        assertTrue(fields.contains("pointItemIds"));
    }

    @Test
    void memberRequestContainsCohortIdForAdmin() {
        Set<String> fields = fieldNames(MemberRequest.class);
        assertTrue(fields.contains("cohortId"), "管理员成员编辑 DTO 应包含 cohortId");
    }

    @Test
    void createUserRequestContainsCohortId() {
        Set<String> fields = fieldNames(CreateUserRequest.class);
        assertTrue(fields.contains("cohortId"), "创建账号应包含 cohortId");
    }

    @Test
    void homeworkRequestContainsCohortId() {
        Set<String> fields = fieldNames(HomeworkAssignmentRequest.class);
        assertTrue(fields.contains("cohortId"), "作业 DTO 应包含 cohortId");
    }

    // ============ 作业权限矩阵（部长跨届本部门 / fullAccess 跨届跨部门） ============

    @Test
    void fullAccessCanReviewAnyDepartment() {
        setActor("会长", "会长室", "会长", null, null);
        assertTrue(checker.canManageHomework());
        assertTrue(checker.canReviewSubmission("技术部"));
        assertTrue(checker.canReviewSubmission("宣传部"));
    }

    @Test
    void ministerCanManageHomework() {
        setActor("张部长", "技术部", "部长", 1L, 2025);
        assertTrue(checker.isMinister());
        assertTrue(checker.canManageHomework());
    }

    @Test
    void ministerCanReviewOnlyOwnDepartment() {
        // 技术部部长：只要部门一致即可批改（届次不参与校验，天然支持跨届本部门）
        setActor("张部长", "技术部", "部长", 1L, 2025);
        assertTrue(checker.canReviewSubmission("技术部"));
        assertFalse(checker.canReviewSubmission("宣传部"));
    }

    @Test
    void ordinaryMemberCannotManageHomework() {
        setActor("李四", "技术部", "社员", 2L, 2026);
        assertFalse(checker.canManageHomework());
        assertFalse(checker.canReviewSubmission("技术部"));
    }

    // ============ ActorContext 届次字段 ============

    @Test
    void actorContextCarriesCohort() {
        setActor("李四", "技术部", "社员", 2L, 2026);
        ActorContext actor = ActorHolder.get();
        assertEquals(2L, actor.getCohortId());
        assertEquals(2026, actor.getCohortYear());
        assertFalse(actor.isFullAccess());
    }
}
