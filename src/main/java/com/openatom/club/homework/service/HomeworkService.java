package com.openatom.club.homework.service;

import com.openatom.club.cohort.entity.Cohort;
import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.exception.PermissionDeniedException;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.file.entity.FileRecord;
import com.openatom.club.file.service.FileStorageService;
import com.openatom.club.homework.dto.HomeworkAssignmentFileResponse;
import com.openatom.club.homework.dto.HomeworkAssignmentRequest;
import com.openatom.club.homework.dto.HomeworkAssignmentResponse;
import com.openatom.club.homework.entity.HomeworkAssignment;
import com.openatom.club.homework.entity.HomeworkAssignmentFile;
import com.openatom.club.homework.entity.HomeworkSubmission;
import com.openatom.club.homework.repository.HomeworkAssignmentFileRepository;
import com.openatom.club.homework.repository.HomeworkAssignmentRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.entity.PointItem;
import com.openatom.club.point.entity.PointRecord;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkService {
    private final HomeworkAssignmentRepository assignmentRepository;
    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkAssignmentFileRepository assignmentFileRepository;
    private final MemberRepository memberRepository;
    private final PointItemRepository pointItemRepository;
    private final PointRecordRepository pointRecordRepository;
    private final CohortRepository cohortRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;
    private final FileStorageService fileStorageService;

    /** 作业发布附件允许的文件类型（管理员/部长发布的资料） */
    private static final Set<String> ASSIGNMENT_FILE_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
            "txt", "md", "zip", "rar", "7z",
            "jpg", "jpeg", "png", "gif", "bmp"
    );

    // ==================== 管理端列表 ====================

    /**
     * @param department 部长固定为本部门；fullAccess 可选
     */
    public Page<HomeworkAssignmentResponse> listAssignments(String status, Long cohortId, String department, int page, int size) {
        permissionChecker.requireManageHomework();
        ActorContext actor = ActorHolder.get();
        String deptFilter = department;
        if (!actor.isFullAccess() && "部长".equals(actor.getPosition())) {
            deptFilter = actor.getDepartment(); // 部长固定本部门
        }
        Page<HomeworkAssignment> assignmentPage = assignmentRepository.findAllWithFilters(status, cohortId, deptFilter,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        return assignmentPage.map(this::toResponse);
    }

    // ==================== 成员端：查看需要完成的作业 ====================

    public List<HomeworkAssignmentResponse> listMyHomework() {
        ActorContext actor = ActorHolder.get();
        Long memberId = actor.getMemberId();
        if (memberId == null) throw BizException.of("当前用户未绑定成员");
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BizException.of("成员不存在"));
        String dept = member.getDepartment();
        Long cohortId = actor.getCohortId();

        List<HomeworkAssignment> all = assignmentRepository.findAll();
        List<HomeworkAssignment> relevant = all.stream()
                .filter(a -> "PUBLISHED".equals(a.getStatus()) || "CLOSED".equals(a.getStatus()))
                .filter(a -> Objects.equals(a.getCohortId(), cohortId))
                .filter(a -> "ALL".equals(a.getTargetType()) ||
                        (dept != null && dept.equals(a.getTargetDepartment())))
                .sorted((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .toList();

        return relevant.stream().map(a -> {
            HomeworkAssignmentResponse r = toResponse(a);
            HomeworkSubmission sub = submissionRepository
                    .findByHomeworkIdAndMemberIdAndDeletedAtIsNull(a.getId(), memberId).orElse(null);
            // 成员视角：submittedCount/gradedCount 表示「本人」提交状态（0/1）。
            // 必须覆盖 toResponse 的全局计数，否则他人提交会让未提交成员被误判为「已提交」。
            r.setSubmittedCount(sub != null ? 1 : 0);
            r.setGradedCount(sub != null && "GRADED".equals(sub.getStatus()) ? 1 : 0);
            return r;
        }).toList();
    }

    // ==================== 详情（补严权限） ====================

    public HomeworkAssignmentResponse getAssignment(Long id) {
        HomeworkAssignment a = assignmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("作业不存在"));
        checkAssignmentViewPermission(a);
        HomeworkAssignmentResponse r = toResponse(a);
        enrichAttachments(r, id);
        return r;
    }

    private void checkAssignmentViewPermission(HomeworkAssignment a) {
        ActorContext actor = ActorHolder.get();
        if (actor.isFullAccess()) return;
        // 部长：本部门管理访问（不限制届次）
        if ("部长".equals(actor.getPosition()) && isMinisterDeptScope(a)) return;
        // 个人视角：非草稿 + 同届 + 符合部门范围
        if ("DRAFT".equals(a.getStatus())) {
            throw PermissionDeniedException.of("您不能查看草稿作业");
        }
        if (!Objects.equals(a.getCohortId(), actor.getCohortId())) {
            throw PermissionDeniedException.of("您不能查看其他届次的作业");
        }
        if (!isMemberInTargetForActor(a, actor)) {
            throw PermissionDeniedException.of("您不在该作业的目标范围内");
        }
    }

    private boolean isMinisterDeptScope(HomeworkAssignment a) {
        ActorContext actor = ActorHolder.get();
        return "DEPARTMENT".equals(a.getTargetType()) &&
                actor.getDepartment() != null &&
                actor.getDepartment().equals(a.getTargetDepartment());
    }

    private boolean isMemberInTargetForActor(HomeworkAssignment a, ActorContext actor) {
        if ("ALL".equals(a.getTargetType())) return true;
        if ("DEPARTMENT".equals(a.getTargetType())) {
            return actor.getDepartment() != null && actor.getDepartment().equals(a.getTargetDepartment());
        }
        return false;
    }

    // ==================== 创建 ====================

    @Transactional
    public HomeworkAssignmentResponse createAssignment(HomeworkAssignmentRequest req) {
        permissionChecker.requireManageHomework();
        validateTargetScope(req);
        requireActiveCohort(req.getCohortId());

        HomeworkAssignment a = new HomeworkAssignment();
        a.setTitle(req.getTitle());
        a.setDescription(req.getDescription());
        a.setTargetType(req.getTargetType());
        a.setTargetDepartment(req.getTargetDepartment());
        a.setCohortId(req.getCohortId());
        a.setDeadline(req.getDeadline());
        a.setMaxPoints(req.getMaxPoints());
        a.setPointItemId(req.getPointItemId());
        a.setStatus("DRAFT");
        a.setCreatedByUserId(ActorHolder.get().getUserId());

        if (req.getPointItemId() != null) {
            pointItemRepository.findByIdAndDeletedAtIsNull(req.getPointItemId())
                    .orElseThrow(() -> BizException.of("积分项目不存在"));
        }

        HomeworkAssignment saved = assignmentRepository.save(a);
        logService.log("homework", "CREATE", String.valueOf(saved.getId()),
                "创建作业: " + saved.getTitle() + "（" + cohortLabel(saved.getCohortId()) + "）");
        return toResponse(saved);
    }

    // ==================== 编辑 ====================

    @Transactional
    public HomeworkAssignmentResponse updateAssignment(Long id, HomeworkAssignmentRequest req) {
        permissionChecker.requireManageHomework();
        HomeworkAssignment a = assignmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("作业不存在"));
        validateOwnership(a);
        validateTargetScope(req);
        requireActiveCohort(req.getCohortId());

        a.setTitle(req.getTitle());
        a.setDescription(req.getDescription());
        a.setTargetType(req.getTargetType());
        a.setTargetDepartment(req.getTargetDepartment());
        a.setCohortId(req.getCohortId());
        a.setDeadline(req.getDeadline());
        a.setMaxPoints(req.getMaxPoints());
        if (req.getPointItemId() != null) {
            pointItemRepository.findByIdAndDeletedAtIsNull(req.getPointItemId())
                    .orElseThrow(() -> BizException.of("积分项目不存在"));
        }
        a.setPointItemId(req.getPointItemId());

        HomeworkAssignment saved = assignmentRepository.save(a);
        logService.log("homework", "UPDATE", String.valueOf(saved.getId()),
                "修改作业: " + saved.getTitle());
        return toResponse(saved);
    }

    // ==================== 发布 ====================

    @Transactional
    public HomeworkAssignmentResponse publishAssignment(Long id) {
        permissionChecker.requireManageHomework();
        HomeworkAssignment a = assignmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("作业不存在"));
        validateOwnership(a);
        if (!"DRAFT".equals(a.getStatus())) {
            throw BizException.of("只有草稿状态的作业可以发布");
        }
        a.setStatus("PUBLISHED");
        HomeworkAssignment saved = assignmentRepository.save(a);
        logService.log("homework", "PUBLISH", String.valueOf(saved.getId()),
                "发布作业: " + saved.getTitle() + "（" + cohortLabel(saved.getCohortId()) + "）");
        return toResponse(saved);
    }

    // ==================== 关闭 ====================

    @Transactional
    public HomeworkAssignmentResponse closeAssignment(Long id) {
        permissionChecker.requireManageHomework();
        HomeworkAssignment a = assignmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("作业不存在"));
        validateOwnership(a);
        if (!"PUBLISHED".equals(a.getStatus())) {
            throw BizException.of("只有已发布的作业可以关闭");
        }
        a.setStatus("CLOSED");
        HomeworkAssignment saved = assignmentRepository.save(a);
        logService.log("homework", "CLOSE", String.valueOf(saved.getId()),
                "关闭作业: " + saved.getTitle());
        return toResponse(saved);
    }

    // ==================== 删除 ====================

    @Transactional
    public void deleteAssignment(Long id) {
        permissionChecker.requireManageHomework();
        HomeworkAssignment a = assignmentRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("作业不存在"));
        validateOwnership(a);
        deleteAssignmentsInternal(List.of(a));
    }

    /**
     * 批量删除作业：全量校验（存在性 + 所有权）后统一级联删除，任一失败整体回滚。
     */
    @Transactional
    public int deleteAssignments(List<Long> ids) {
        permissionChecker.requireManageHomework();
        List<Long> distinctIds = ids == null ? List.of()
                : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw BizException.of("请选择要删除的作业");
        }
        List<HomeworkAssignment> assignments = assignmentRepository
                .findAllByIdInAndDeletedAtIsNull(distinctIds);
        if (assignments.size() != distinctIds.size()) {
            throw BizException.of("存在无效作业，请刷新后重试");
        }
        // 全量校验：所有权（任一不满足整体失败）
        for (HomeworkAssignment a : assignments) {
            validateOwnership(a);
        }
        deleteAssignmentsInternal(assignments);
        return assignments.size();
    }

    /**
     * 级联删除：软删除作业 + 其批改产生的积分记录（HOMEWORK），并清空提交的 point_record_id。
     */
    private void deleteAssignmentsInternal(List<HomeworkAssignment> assignments) {
        List<Long> homeworkIds = assignments.stream().map(HomeworkAssignment::getId).toList();
        OffsetDateTime now = OffsetDateTime.now();

        // 1) 级联清理：软删除批改积分记录 + 清空提交的积分引用
        List<HomeworkSubmission> submissions = submissionRepository
                .findAllByHomeworkIdInAndDeletedAtIsNull(homeworkIds);
        List<Long> pointRecordIds = submissions.stream()
                .map(HomeworkSubmission::getPointRecordId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        int deletedPointCount = 0;
        if (!pointRecordIds.isEmpty()) {
            List<PointRecord> records = pointRecordRepository.findAllByIdInAndDeletedAtIsNull(pointRecordIds);
            for (PointRecord r : records) {
                r.setDeletedAt(now);
            }
            pointRecordRepository.saveAll(records);
            deletedPointCount = records.size();
        }
        for (HomeworkSubmission s : submissions) {
            if (s.getPointRecordId() != null) {
                s.setPointRecordId(null);
            }
        }
        submissionRepository.saveAll(submissions);

        // 2) 级联软删除发布附件关系 + 文件记录
        List<HomeworkAssignmentFile> assignmentFiles = assignmentFileRepository
                .findAllByAssignmentIdInAndDeletedAtIsNull(homeworkIds);
        for (HomeworkAssignmentFile af : assignmentFiles) {
            af.setDeletedAt(now);
            fileStorageService.softDeleteFileRecord(af.getFileId());
        }
        assignmentFileRepository.saveAll(assignmentFiles);

        // 3) 软删除作业
        for (HomeworkAssignment a : assignments) {
            a.setDeletedAt(now);
        }
        assignmentRepository.saveAll(assignments);

        // 4) 操作日志
        if (assignments.size() == 1) {
            HomeworkAssignment a = assignments.get(0);
            logService.log("homework", "DELETE", String.valueOf(a.getId()),
                    "删除作业: " + a.getTitle() + "，级联删除积分记录 " + deletedPointCount + " 条");
        } else {
            String titles = assignments.stream().map(HomeworkAssignment::getTitle)
                    .limit(10).collect(Collectors.joining("、"));
            if (assignments.size() > 10) {
                titles += " 等";
            }
            logService.log("homework", "DELETE", "批量:" + assignments.size() + "个",
                    "批量删除作业 " + assignments.size() + " 个（" + titles + "），级联删除积分记录 " + deletedPointCount + " 条");
        }
    }

    // ==================== 作业发布附件 ====================

    /** 列出作业发布附件：成员（非草稿+同届+范围内）与管理员/部长均可查看 */
    public List<HomeworkAssignmentFileResponse> listAssignmentFiles(Long assignmentId) {
        HomeworkAssignment a = assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId)
                .orElseThrow(() -> BizException.of("作业不存在"));
        checkAssignmentViewPermission(a);
        return assignmentFileRepository.findAllByAssignmentIdAndDeletedAtIsNull(assignmentId)
                .stream().map(this::toFileResponse).toList();
    }

    /** 上传作业发布附件：fullAccess 或部长（本部门，届次自由） */
    @Transactional
    public List<HomeworkAssignmentFileResponse> uploadAssignmentFiles(Long assignmentId,
                                                                       List<MultipartFile> files) throws IOException {
        permissionChecker.requireManageHomework();
        HomeworkAssignment a = assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId)
                .orElseThrow(() -> BizException.of("作业不存在"));
        validateOwnership(a);

        List<HomeworkAssignmentFileResponse> results = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                FileRecord record = fileStorageService.saveFile(file, "homework-assignment",
                        "homework-assignment/" + assignmentId, ASSIGNMENT_FILE_EXTENSIONS);
                HomeworkAssignmentFile af = new HomeworkAssignmentFile();
                af.setAssignmentId(assignmentId);
                af.setFileId(record.getId());
                assignmentFileRepository.save(af);
                results.add(toFileResponse(af));
            }
        }
        if (!results.isEmpty()) {
            logService.log("homework", "UPLOAD", String.valueOf(assignmentId),
                    "上传作业附件 " + results.size() + " 个: " + a.getTitle());
        }
        return results;
    }

    /** 下载作业发布附件：先校验作业访问权限，再校验 fileId 归属，禁止通过 fileId 绕过权限 */
    public void downloadAssignmentFile(Long assignmentId, Long fileId, HttpServletResponse response) throws IOException {
        HomeworkAssignment a = assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId)
                .orElseThrow(() -> BizException.of("作业不存在"));
        checkAssignmentViewPermission(a);
        HomeworkAssignmentFile af = assignmentFileRepository
                .findByAssignmentIdAndFileIdAndDeletedAtIsNull(assignmentId, fileId)
                .orElseThrow(() -> BizException.of("附件不存在"));
        fileStorageService.downloadFile(af.getFileId(), response);
    }

    /** 删除作业发布附件：fullAccess 或部长（本部门，届次自由） */
    @Transactional
    public void deleteAssignmentFile(Long assignmentId, Long fileId) {
        permissionChecker.requireManageHomework();
        HomeworkAssignment a = assignmentRepository.findByIdAndDeletedAtIsNull(assignmentId)
                .orElseThrow(() -> BizException.of("作业不存在"));
        validateOwnership(a);
        HomeworkAssignmentFile af = assignmentFileRepository
                .findByAssignmentIdAndFileIdAndDeletedAtIsNull(assignmentId, fileId)
                .orElseThrow(() -> BizException.of("附件不存在"));
        af.setDeletedAt(OffsetDateTime.now());
        assignmentFileRepository.save(af);
        fileStorageService.softDeleteFileRecord(af.getFileId());
        logService.log("homework", "DELETE", String.valueOf(af.getId()),
                "删除作业附件: " + a.getTitle());
    }

    private HomeworkAssignmentFileResponse toFileResponse(HomeworkAssignmentFile af) {
        HomeworkAssignmentFileResponse dto = new HomeworkAssignmentFileResponse();
        dto.setId(af.getId());
        dto.setFileId(af.getFileId());
        dto.setCreatedAt(af.getCreatedAt());
        FileRecord fr = fileStorageService.getFileRecord(af.getFileId());
        if (fr != null) {
            dto.setOriginalName(fr.getOriginalName());
            dto.setFileSize(fr.getFileSize());
            dto.setContentType(fr.getContentType());
        }
        return dto;
    }

    private void enrichAttachments(HomeworkAssignmentResponse r, Long assignmentId) {
        r.setAttachments(assignmentFileRepository.findAllByAssignmentIdAndDeletedAtIsNull(assignmentId)
                .stream().map(this::toFileResponse).toList());
    }

    // ==================== 内部辅助 ====================

    private HomeworkAssignmentResponse toResponse(HomeworkAssignment a) {
        HomeworkAssignmentResponse r = HomeworkAssignmentResponse.from(a);
        r.setCohortYear(a.getCohortId() == null ? null : cohortYear(a.getCohortId()));
        if (a.getPointItemId() != null) {
            pointItemRepository.findById(a.getPointItemId()).ifPresent(pi -> r.setPointItemName(pi.getItemName()));
        }
        r.setCreatedByName(ActorHolder.get().getName());
        // submissionCount = 所有有效提交（含已批改），批改不减少；
        // submittedCount = 仅 SUBMITTED 状态（待批改）；gradedCount = GRADED。
        r.setSubmissionCount(submissionRepository.countByHomeworkId(a.getId()));
        r.setSubmittedCount(submissionRepository.countByHomeworkIdAndStatus(a.getId(), "SUBMITTED"));
        r.setGradedCount(submissionRepository.countByHomeworkIdAndStatus(a.getId(), "GRADED"));
        return r;
    }

    /** 验证部长不能创建 ALL 或其它部门作业（届次自由） */
    private void validateTargetScope(HomeworkAssignmentRequest req) {
        ActorContext actor = ActorHolder.get();
        if (actor.isFullAccess()) return;

        if ("部长".equals(actor.getPosition())) {
            if ("ALL".equals(req.getTargetType())) {
                throw PermissionDeniedException.of("部长不能发布面向全体成员的作业");
            }
            String actorDept = actor.getDepartment();
            if (!"DEPARTMENT".equals(req.getTargetType()) || req.getTargetDepartment() == null) {
                throw PermissionDeniedException.of("部长只能发布面向本部门的作业");
            }
            if (!req.getTargetDepartment().equals(actorDept)) {
                throw PermissionDeniedException.of("部长只能发布面向本部门的作业");
            }
        }
    }

    /** 验证创建者是否可以管理此作业（部长不能管理其他部门作业） */
    private void validateOwnership(HomeworkAssignment a) {
        ActorContext actor = ActorHolder.get();
        if (actor.isFullAccess()) return;

        if ("部长".equals(actor.getPosition())) {
            if ("ALL".equals(a.getTargetType())) {
                throw PermissionDeniedException.of("您只能管理本部门的作业");
            }
            if (!"DEPARTMENT".equals(a.getTargetType()) ||
                    !actor.getDepartment().equals(a.getTargetDepartment())) {
                throw PermissionDeniedException.of("您只能管理本部门的作业");
            }
        }
    }

    private void requireActiveCohort(Long cohortId) {
        Cohort cohort = cohortRepository.findByIdAndDeletedAtIsNull(cohortId)
                .orElseThrow(() -> BizException.of("届次不存在"));
        if (!Boolean.TRUE.equals(cohort.getEnabled())) {
            throw BizException.of("届次已停用，不能创建作业");
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
}
