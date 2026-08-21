package com.openatom.club.member.service;

import com.openatom.club.auth.entity.UserAccount;
import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.cohort.entity.Cohort;
import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.exception.PermissionDeniedException;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.homework.entity.HomeworkSubmission;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.dto.BatchDeleteMembersResponse;
import com.openatom.club.member.dto.MemberRequest;
import com.openatom.club.member.dto.MemberResponse;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.entity.PointApplication;
import com.openatom.club.point.entity.PointRecord;
import com.openatom.club.point.repository.PointApplicationRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final CohortRepository cohortRepository;
    private final PointRecordRepository pointRecordRepository;
    private final PointApplicationRepository pointApplicationRepository;
    private final UserAccountRepository userAccountRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    /**
     * @param cohortId null=全部；-1=未分届；其他=指定届次
     */
    public PageResult<MemberResponse> list(String keyword, Long cohortId, int page, int size) {
        Page<Member> memberPage = memberRepository.searchMembers(
                cohortId,
                StringUtils.hasText(keyword) ? keyword : null,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "id"))
        );
        Map<Long, Integer> years = cohortYearMap();
        List<MemberResponse> list = memberPage.getContent().stream()
                .map(m -> MemberResponse.from(m, m.getCohortId() == null ? null : years.get(m.getCohortId())))
                .toList();
        return new PageResult<>(list, memberPage.getTotalElements(), page, size);
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest req) {
        permissionChecker.requireManage();
        Member member = memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("成员不存在"));
        if (!member.getStudentNo().equals(req.getStudentNo()) &&
                memberRepository.existsByStudentNoAndDeletedAtIsNullAndIdNot(req.getStudentNo(), id)) {
            throw BizException.of("学号已被其他成员使用: " + req.getStudentNo());
        }

        // 记录组织身份变更，供操作日志审计
        Long oldCohortId = member.getCohortId();
        String oldDepartment = member.getDepartment();
        String oldPosition = member.getPosition();

        // 基础字段：秘书处及以上可修改
        member.setName(req.getName());
        member.setStudentNo(req.getStudentNo());
        member.setPhone(req.getPhone());
        member.setMajor(req.getMajor());

        // 组织身份（届次/部门/职务）只能由 fullAccess 管理员修改
        if (req.getCohortId() != null && !req.getCohortId().equals(member.getCohortId())) {
            permissionChecker.requireAdmin();
            requireActiveCohort(req.getCohortId());
            member.setCohortId(req.getCohortId());
        }
        if (req.getDepartment() != null) {
            permissionChecker.requireAdmin();
            member.setDepartment(req.getDepartment());
        }
        if (StringUtils.hasText(req.getPosition())) {
            permissionChecker.requireAdmin();
            member.setPosition(req.getPosition());
        }

        Member saved = memberRepository.save(member);
        logService.log("member", "UPDATE", String.valueOf(id),
                "修改成员: " + saved.getName()
                        + "，届次 " + cohortLabel(oldCohortId) + "→" + cohortLabel(saved.getCohortId())
                        + "，部门 " + oldDepartment + "→" + saved.getDepartment()
                        + "，职务 " + oldPosition + "→" + saved.getPosition());
        return MemberResponse.from(saved, cohortYear(saved.getCohortId()));
    }

    /**
     * 删除成员（软删除），并同步清理该成员的积分业务数据、禁用关联账号。
     * 单删与批量删除共享同一套核心逻辑（见 {@link #deleteMembersInternal}）。
     */
    @Transactional
    public void delete(Long id) {
        if (id == null) {
            throw BizException.of("成员不存在");
        }
        deleteMembersInternal(List.of(id));
    }

    /**
     * 批量删除成员：全量校验 → 清理积分申请/积分记录/作业积分引用 → 禁用账号 → 软删除成员。
     * 整个批次处于同一事务，任一步失败即整体回滚。
     */
    @Transactional
    public BatchDeleteMembersResponse batchDelete(List<Long> memberIds) {
        return deleteMembersInternal(memberIds);
    }

    /**
     * 单删/批删共享的核心删除生命周期。
     * 顺序：
     * 1. 权限校验（fullAccess）
     * 2. 去重 + 非空校验
     * 3. 批量查询全部 Member，校验全部存在且未删除
     * 4. 校验不包含当前登录管理员本人
     * 5. 清除作业提交的 pointRecordId 引用（避免悬空引用）
     * 6. 软删除该成员全部 PointRecord（APPLICATION/MANUAL/HOMEWORK）
     * 7. 软删除该成员全部 PointApplication（PENDING/APPROVED/REJECTED）
     * 8. 禁用该成员关联的 UserAccount
     * 9. 软删除 Member
     * 10. 写 OperationLog
     */
    private BatchDeleteMembersResponse deleteMembersInternal(List<Long> rawMemberIds) {
        permissionChecker.requireFullAccess();
        ActorContext actor = permissionChecker.currentActor();

        // 去重 + 过滤 null
        List<Long> memberIds = rawMemberIds == null ? List.of() :
                rawMemberIds.stream().filter(Objects::nonNull).distinct().toList();
        if (memberIds.isEmpty()) {
            throw BizException.of("请选择要删除的成员");
        }

        // 批量查询全部有效成员（软删除的自动被 @SQLRestriction 过滤）
        List<Member> members = memberRepository.findAllByIdInAndDeletedAtIsNull(memberIds);
        if (members.size() != memberIds.size()) {
            throw BizException.of("存在无效成员，请刷新列表后重试");
        }

        // 禁止删除当前登录管理员本人（整批拒绝）
        if (actor.getMemberId() != null && memberIds.contains(actor.getMemberId())) {
            throw PermissionDeniedException.of("不能删除当前登录账号对应的成员");
        }

        OffsetDateTime now = OffsetDateTime.now();

        // 1) 清除作业提交对积分记录的引用（HOMEWORK 积分记录即将被软删除）
        List<HomeworkSubmission> submissions =
                homeworkSubmissionRepository.findAllByMemberIdInAndDeletedAtIsNull(memberIds);
        for (HomeworkSubmission s : submissions) {
            if (s.getPointRecordId() != null) {
                s.setPointRecordId(null);
            }
        }
        homeworkSubmissionRepository.saveAll(submissions);

        // 2) 软删除全部积分记录（三种来源 APPLICATION/MANUAL/HOMEWORK 全部处理）
        List<PointRecord> records =
                pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(memberIds);
        for (PointRecord r : records) {
            r.setDeletedAt(now);
        }
        pointRecordRepository.saveAll(records);

        // 3) 软删除全部积分申请（PENDING/APPROVED/REJECTED 全部处理）
        List<PointApplication> applications =
                pointApplicationRepository.findAllByMemberIdInAndDeletedAtIsNull(memberIds);
        for (PointApplication a : applications) {
            a.setDeletedAt(now);
        }
        pointApplicationRepository.saveAll(applications);

        // 4) 禁用关联账号（物理保留，审计可追溯；不存在账号则跳过）
        List<UserAccount> accounts =
                userAccountRepository.findAllByMemberIdInAndDeletedAtIsNull(memberIds);
        int disabledCount = 0;
        for (UserAccount u : accounts) {
            if (Boolean.TRUE.equals(u.getEnabled())) {
                u.setEnabled(false);
                disabledCount++;
            }
        }
        userAccountRepository.saveAll(accounts);

        // 5) 软删除成员本身
        for (Member m : members) {
            m.setDeletedAt(now);
        }
        memberRepository.saveAll(members);

        // 6) 操作日志（保留审计）
        logDeletion(members, applications.size(), records.size(), disabledCount);

        return new BatchDeleteMembersResponse(
                members.size(), applications.size(), records.size(), disabledCount);
    }

    private void logDeletion(List<Member> members, int appCount, int recCount, int disabledCount) {
        if (members.size() == 1) {
            Member m = members.get(0);
            logService.log("member", "DELETE", String.valueOf(m.getId()),
                    "删除成员：" + m.getName()
                            + "，届次：" + cohortLabel(m.getCohortId())
                            + "，部门：" + (m.getDepartment() == null ? "未填写" : m.getDepartment())
                            + "，职务：" + m.getPosition()
                            + "，删除积分申请：" + appCount + " 条"
                            + "，删除积分记录：" + recCount + " 条"
                            + "，禁用账号：" + (disabledCount > 0 ? "是" : "否"));
        } else {
            String names = members.stream().map(Member::getName).limit(10)
                    .collect(Collectors.joining("、"));
            if (members.size() > 10) {
                names += " 等";
            }
            logService.log("member", "DELETE", "批量:" + members.size() + "名",
                    "批量删除成员 " + members.size() + " 名（" + names + "）"
                            + "，删除积分申请 " + appCount + " 条"
                            + "，删除积分记录 " + recCount + " 条"
                            + "，禁用账号 " + disabledCount + " 个");
        }
    }

    public MemberResponse getById(Long id) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("成员不存在"));
        return MemberResponse.from(member, cohortYear(member.getCohortId()));
    }

    /** 批量给未分届成员设置届次（仅 fullAccess） */
    @Transactional
    public int batchSetCohort(List<Long> memberIds, Long cohortId) {
        permissionChecker.requireFullAccess();
        requireActiveCohort(cohortId);
        int updated = 0;
        for (Long id : memberIds) {
            if (id == null) continue;
            Member member = memberRepository.findByIdAndDeletedAtIsNull(id).orElse(null);
            if (member == null) continue;
            member.setCohortId(cohortId);
            memberRepository.save(member);
            updated++;
        }
        logService.log("member", "UPDATE", String.valueOf(cohortId),
                "批量设置 " + updated + " 名成员的届次为 " + cohortLabel(cohortId));
        return updated;
    }

    private void requireActiveCohort(Long cohortId) {
        Cohort cohort = cohortRepository.findByIdAndDeletedAtIsNull(cohortId)
                .orElseThrow(() -> BizException.of("届次不存在"));
        if (!Boolean.TRUE.equals(cohort.getEnabled())) {
            throw BizException.of("届次已停用，不能分配给成员");
        }
    }

    private Integer cohortYear(Long cohortId) {
        if (cohortId == null) return null;
        return cohortRepository.findByIdAndDeletedAtIsNull(cohortId).map(Cohort::getYear).orElse(null);
    }

    private String cohortLabel(Long cohortId) {
        Integer y = cohortYear(cohortId);
        return y == null ? "未分届" : y + "届";
    }

    private Map<Long, Integer> cohortYearMap() {
        return cohortRepository.findAllByDeletedAtIsNullOrderByYearDesc().stream()
                .collect(Collectors.toMap(Cohort::getId, Cohort::getYear));
    }
}
