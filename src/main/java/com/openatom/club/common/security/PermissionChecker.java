package com.openatom.club.common.security;

import com.openatom.club.common.exception.PermissionDeniedException;
import org.springframework.stereotype.Component;

/**
 * 权限检查工具 V2：所有权限统一基于 isFullAccess()
 */
@Component
public class PermissionChecker {

    /** fullAccess = 会长 | 副会长 | 秘书处成员 */
    public void requireFullAccess() {
        ActorContext actor = ActorHolder.get();
        if (!actor.isFullAccess()) {
            throw PermissionDeniedException.of("该操作需要会长、副会长或秘书处成员权限");
        }
    }

    /** 兼容旧调用 */
    public void requireManage() {
        requireFullAccess();
    }

    /** 兼容旧调用 */
    public void requireAdmin() {
        requireFullAccess();
    }

    /** 财务权限 = fullAccess */
    public void requireFinanceAccess() {
        ActorContext actor = ActorHolder.get();
        if (!actor.isFullAccess()) {
            throw PermissionDeniedException.of("财务台账只有秘书处成员、会长、副会长可以访问");
        }
    }

    /** 操作日志权限 = fullAccess */
    public void requireLogAccess() {
        ActorContext actor = ActorHolder.get();
        if (!actor.isFullAccess()) {
            throw PermissionDeniedException.of("操作日志只有秘书处成员、会长、副会长可以查看");
        }
    }

    /** 账号管理权限 = fullAccess */
    public void requireUserManage() {
        ActorContext actor = ActorHolder.get();
        if (!actor.isFullAccess()) {
            throw PermissionDeniedException.of("账号管理只有秘书处成员、会长、副会长可以操作");
        }
    }

    public ActorContext currentActor() {
        return ActorHolder.get();
    }

    // ======================== 作业模块权限 ========================

    /** 部长：position == "部长" */
    public boolean isMinister() {
        ActorContext actor = ActorHolder.get();
        return "部长".equals(actor.getPosition());
    }

    /** 是否可以管理作业（查看批改列表、发布作业等）：fullAccess 或 部长 */
    public boolean canManageHomework() {
        ActorContext actor = ActorHolder.get();
        return actor.isFullAccess() || "部长".equals(actor.getPosition());
    }

    /** 要求作业管理权限 */
    public void requireManageHomework() {
        if (!canManageHomework()) {
            throw PermissionDeniedException.of("该操作需要会长、副会长、秘书处成员或部长权限");
        }
    }

    /** 是否可以批改作业：fullAccess 或 部长 */
    public boolean canReviewHomework() {
        return canManageHomework();
    }

    /** 是否可以批改特定成员的提交 */
    public boolean canReviewSubmission(String submissionMemberDepartment) {
        ActorContext actor = ActorHolder.get();
        // fullAccess 可以批改全部
        if (actor.isFullAccess()) return true;
        // 部长只能批改本部门
        if ("部长".equals(actor.getPosition())) {
            String actorDept = actor.getDepartment();
            return actorDept != null && actorDept.equals(submissionMemberDepartment);
        }
        return false;
    }

    /** 要求批改特定提交的权限 */
    public void requireReviewSubmission(String submissionMemberDepartment) {
        if (!canReviewSubmission(submissionMemberDepartment)) {
            throw PermissionDeniedException.of("您只能批改本部门成员的作业");
        }
    }

    /** 是否可以下载作业附件 */
    public boolean canDownloadHomeworkFile(String submissionMemberDepartment) {
        return canReviewSubmission(submissionMemberDepartment);
    }

    /** 要求下载作业附件权限 */
    public void requireDownloadHomeworkFile(String submissionMemberDepartment) {
        if (!canDownloadHomeworkFile(submissionMemberDepartment)) {
            throw PermissionDeniedException.of("您只能下载本部门成员的作业附件");
        }
    }
}
