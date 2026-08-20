package com.openatom.club.homework.service;

import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.exception.PermissionDeniedException;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.file.entity.FileRecord;
import com.openatom.club.file.service.FileStorageService;
import com.openatom.club.homework.dto.GradeSubmissionRequest;
import com.openatom.club.homework.dto.HomeworkSubmissionResponse;
import com.openatom.club.homework.entity.HomeworkAssignment;
import com.openatom.club.homework.entity.HomeworkSubmission;
import com.openatom.club.homework.entity.HomeworkSubmissionFile;
import com.openatom.club.homework.repository.HomeworkAssignmentRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionFileRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.PointItemTypes;
import com.openatom.club.point.entity.PointItem;
import com.openatom.club.point.entity.PointRecord;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import com.openatom.club.auth.entity.UserAccount;
import com.openatom.club.auth.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class HomeworkSubmissionService {
    private final HomeworkSubmissionRepository submissionRepository;
    private final HomeworkSubmissionFileRepository submissionFileRepository;
    private final HomeworkAssignmentRepository assignmentRepository;
    private final MemberRepository memberRepository;
    private final PointRecordRepository pointRecordRepository;
    private final PointItemRepository pointItemRepository;
    private final UserAccountRepository userAccountRepository;
    private final FileStorageService fileStorageService;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(
            "pdf", "doc", "docx", "zip", "rar", "7z",
            "jpg", "jpeg", "png", "gif", "bmp",
            "txt", "md", "pptx", "xlsx"
    );

    // ==================== 成员提交 ====================

    @Transactional
    public HomeworkSubmissionResponse submitHomework(Long homeworkId, String content,
                                                      List<MultipartFile> files) throws IOException {
        ActorContext actor = ActorHolder.get();
        Long memberId = actor.getMemberId();
        if (memberId == null) throw BizException.of("当前用户未绑定成员");

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BizException.of("成员不存在"));

        HomeworkAssignment assignment = assignmentRepository.findByIdAndDeletedAtIsNull(homeworkId)
                .orElseThrow(() -> BizException.of("作业不存在"));

        // 检查作业状态
        if (!"PUBLISHED".equals(assignment.getStatus())) {
            throw BizException.of("该作业尚未发布或已关闭");
        }

        // 检查成员是否在目标范围内
        if (!isMemberInTarget(member, assignment)) {
            throw BizException.of("您不在该作业的目标范围内");
        }

        // 检查届次归属：只能提交自己届次的作业
        if (!Objects.equals(assignment.getCohortId(), member.getCohortId())) {
            throw PermissionDeniedException.of("您不能提交其他届次的作业");
        }

        // 检查截止时间
        if (assignment.getDeadline() != null &&
                OffsetDateTime.now().isAfter(assignment.getDeadline())) {
            throw BizException.of("已超过作业截止时间");
        }

        // 查找已有提交
        Optional<HomeworkSubmission> existingOpt = submissionRepository
                .findByHomeworkIdAndMemberIdAndDeletedAtIsNull(homeworkId, memberId);

        HomeworkSubmission submission;
        boolean isUpdate;

        if (existingOpt.isPresent()) {
            submission = existingOpt.get();
            // 如果已批改，禁止修改
            if ("GRADED".equals(submission.getStatus())) {
                throw BizException.of("作业已批改，无法重新提交");
            }
            // 更新内容
            submission.setContent(content);
            submission.setSubmittedAt(OffsetDateTime.now());
            // 清除旧附件关联（软删除文件记录）
            List<HomeworkSubmissionFile> oldFiles = submissionFileRepository.findAllBySubmissionId(submission.getId());
            for (HomeworkSubmissionFile sf : oldFiles) {
                fileStorageService.softDeleteFileRecord(sf.getFileId());
            }
            submissionFileRepository.deleteAllBySubmissionId(submission.getId());
            isUpdate = true;
        } else {
            submission = new HomeworkSubmission();
            submission.setHomeworkId(homeworkId);
            submission.setMemberId(memberId);
            submission.setContent(content);
            submission.setStatus("SUBMITTED");
            submission.setSubmittedAt(OffsetDateTime.now());
            isUpdate = false;
        }

        HomeworkSubmission saved = submissionRepository.save(submission);

        // 保存新附件
        if (files != null) {
            for (MultipartFile file : files) {
                if (file.isEmpty()) continue;
                FileRecord fileRecord = fileStorageService.saveFile(file, "homework",
                        "homework/" + homeworkId, ALLOWED_FILE_EXTENSIONS);
                HomeworkSubmissionFile sf = new HomeworkSubmissionFile();
                sf.setSubmissionId(saved.getId());
                sf.setFileId(fileRecord.getId());
                submissionFileRepository.save(sf);
            }
        }

        String action = isUpdate ? "重新提交" : "提交";
        logService.log("homework_submission", isUpdate ? "RESUBMIT" : "SUBMIT",
                String.valueOf(saved.getId()),
                action + "作业《" + assignment.getTitle() + "》, 成员: " + member.getName());

        return enrichSubmissionResponse(saved, member, assignment);
    }

    // ==================== 获取自己的提交 ====================

    public HomeworkSubmissionResponse getMySubmission(Long homeworkId) {
        ActorContext actor = ActorHolder.get();
        Long memberId = actor.getMemberId();
        if (memberId == null) throw BizException.of("当前用户未绑定成员");

        HomeworkSubmission submission = submissionRepository
                .findByHomeworkIdAndMemberIdAndDeletedAtIsNull(homeworkId, memberId)
                .orElseThrow(() -> BizException.of("您尚未提交该作业"));

        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BizException.of("成员不存在"));
        HomeworkAssignment assignment = assignmentRepository.findByIdAndDeletedAtIsNull(homeworkId)
                .orElseThrow(() -> BizException.of("作业不存在"));

        return enrichSubmissionResponse(submission, member, assignment);
    }

    // ==================== 管理员查看提交列表 ====================

    public Page<HomeworkSubmissionResponse> listSubmissions(Long homeworkId, String status, int page, int size) {
        permissionChecker.requireManageHomework();

        HomeworkAssignment assignment = assignmentRepository.findByIdAndDeletedAtIsNull(homeworkId)
                .orElseThrow(() -> BizException.of("作业不存在"));

        // 检查部门权限
        enforceDepartmentScope(assignment);

        Page<HomeworkSubmission> submissionPage = submissionRepository
                .findByHomeworkIdWithStatus(homeworkId, status,
                        PageRequest.of(page - 1, size, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "submittedAt")));

        return submissionPage.map(s -> {
            Member member = memberRepository.findById(s.getMemberId()).orElse(null);
            // 部门过滤
            if (member != null) {
                if (!canAccessMember(assignment, member)) {
                    return null;
                }
            }
            return enrichSubmissionResponse(s, member, assignment);
        });
    }

    // ==================== 查看单个提交 ====================

    public HomeworkSubmissionResponse getSubmission(Long id) {
        permissionChecker.requireManageHomework();

        HomeworkSubmission submission = submissionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("提交不存在"));

        HomeworkAssignment assignment = assignmentRepository.findByIdAndDeletedAtIsNull(submission.getHomeworkId())
                .orElseThrow(() -> BizException.of("作业不存在"));
        enforceDepartmentScope(assignment);

        Member member = memberRepository.findById(submission.getMemberId()).orElse(null);
        if (member != null && !canAccessMember(assignment, member)) {
            throw PermissionDeniedException.of("您只能查看本部门成员的提交");
        }

        return enrichSubmissionResponse(submission, member, assignment);
    }

    // ==================== 批改 ====================

    @Transactional
    public HomeworkSubmissionResponse gradeSubmission(Long id, GradeSubmissionRequest req) {
        permissionChecker.requireManageHomework();

        HomeworkSubmission submission = submissionRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("提交不存在"));

        HomeworkAssignment assignment = assignmentRepository.findByIdAndDeletedAtIsNull(submission.getHomeworkId())
                .orElseThrow(() -> BizException.of("作业不存在"));
        enforceDepartmentScope(assignment);

        Member member = memberRepository.findById(submission.getMemberId())
                .orElseThrow(() -> BizException.of("成员不存在"));

        // 检查部门权限
        permissionChecker.requireReviewSubmission(member.getDepartment() != null ? member.getDepartment() : "");

        // 积分范围校验（后端兜底）：负数永远拒绝；有最大积分时再校验上限
        if (req.getPoints() == null) {
            throw BizException.of("积分不能为空");
        }
        if (req.getPoints().compareTo(BigDecimal.ZERO) < 0) {
            throw BizException.of("积分不能小于 0");
        }
        if (assignment.getMaxPoints() != null
                && req.getPoints().compareTo(assignment.getMaxPoints()) > 0) {
            throw BizException.of("积分不能超过最大积分 " + assignment.getMaxPoints());
        }

        ActorContext actor = ActorHolder.get();
        boolean isFirstGrade = submission.getPointRecordId() == null;

        // 创建或更新 point_record
        PointRecord pointRecord;
        if (isFirstGrade) {
            pointRecord = new PointRecord();
            pointRecord.setMemberId(submission.getMemberId());
            // 作业积分统一归入「开源学习」类型，不随作业创建时手选的积分项目
            pointRecord.setPointItemId(findOpenSourceLearningPointItemId());
            pointRecord.setScore(req.getPoints());
            pointRecord.setSourceType("HOMEWORK");
            pointRecord.setOperatorName(actor.getName());
            pointRecord.setReason("完成作业：《" + assignment.getTitle() + "》" +
                    (req.getComment() != null && !req.getComment().isEmpty() ? " - 批语: " + req.getComment() : ""));
            pointRecord = pointRecordRepository.save(pointRecord);
        } else {
            // 修改原有积分
            pointRecord = pointRecordRepository.findByIdAndDeletedAtIsNull(submission.getPointRecordId())
                    .orElseThrow(() -> BizException.of("关联的积分记录不存在"));
            pointRecord.setScore(req.getPoints());
            pointRecord.setOperatorName(actor.getName());
            pointRecord.setReason("完成作业：《" + assignment.getTitle() + "》" +
                    (req.getComment() != null && !req.getComment().isEmpty() ? " - 批语: " + req.getComment() : ""));
            pointRecord = pointRecordRepository.save(pointRecord);
        }

        // 更新 submission
        submission.setStatus("GRADED");
        submission.setAwardedPoints(req.getPoints());
        submission.setReviewComment(req.getComment());
        submission.setReviewedByUserId(actor.getUserId());
        submission.setReviewedAt(OffsetDateTime.now());
        submission.setPointRecordId(pointRecord.getId());
        HomeworkSubmission saved = submissionRepository.save(submission);

        String action = isFirstGrade ? "批改" : "修改批改结果";
        logService.log("homework_submission", isFirstGrade ? "GRADE" : "REGRADE",
                String.valueOf(saved.getId()),
                action + "作业《" + assignment.getTitle() + "》, 成员: " + member.getName() +
                        ", 积分: " + req.getPoints());

        return enrichSubmissionResponse(saved, member, assignment);
    }

    // ==================== 文件下载（带权限校验） ====================

    public void downloadSubmissionFile(Long submissionId, Long fileId, HttpServletResponse response) throws IOException {
        HomeworkSubmission submission = submissionRepository.findByIdAndDeletedAtIsNull(submissionId)
                .orElseThrow(() -> BizException.of("提交不存在"));

        // 权限检查：查看者是否是提交者本人，或者有管理权限
        ActorContext actor = ActorHolder.get();
        if (!submission.getMemberId().equals(actor.getMemberId())) {
            // 不是提交者本人，需要管理权限
            permissionChecker.requireManageHomework();
            HomeworkAssignment assignment = assignmentRepository.findByIdAndDeletedAtIsNull(submission.getHomeworkId())
                    .orElseThrow(() -> BizException.of("作业不存在"));
            enforceDepartmentScope(assignment);
            Member member = memberRepository.findById(submission.getMemberId()).orElse(null);
            if (member != null) {
                permissionChecker.requireDownloadHomeworkFile(
                        member.getDepartment() != null ? member.getDepartment() : "");
            }
        }

        // 验证文件属于该提交
        List<HomeworkSubmissionFile> submissionFiles = submissionFileRepository.findAllBySubmissionId(submissionId);
        boolean fileBelongs = submissionFiles.stream().anyMatch(sf -> sf.getFileId().equals(fileId));
        if (!fileBelongs) {
            throw BizException.of("该文件不属于此提交");
        }

        fileStorageService.downloadFile(fileId, response);
    }

    public void viewSubmissionFile(Long submissionId, Long fileId, HttpServletResponse response) throws IOException {
        HomeworkSubmission submission = submissionRepository.findByIdAndDeletedAtIsNull(submissionId)
                .orElseThrow(() -> BizException.of("提交不存在"));

        // 权限检查
        ActorContext actor = ActorHolder.get();
        if (!submission.getMemberId().equals(actor.getMemberId())) {
            permissionChecker.requireManageHomework();
            HomeworkAssignment assignment = assignmentRepository.findByIdAndDeletedAtIsNull(submission.getHomeworkId())
                    .orElseThrow(() -> BizException.of("作业不存在"));
            enforceDepartmentScope(assignment);
            Member member = memberRepository.findById(submission.getMemberId()).orElse(null);
            if (member != null) {
                permissionChecker.requireDownloadHomeworkFile(
                        member.getDepartment() != null ? member.getDepartment() : "");
            }
        }

        // 验证文件属于该提交
        List<HomeworkSubmissionFile> submissionFiles = submissionFileRepository.findAllBySubmissionId(submissionId);
        boolean fileBelongs = submissionFiles.stream().anyMatch(sf -> sf.getFileId().equals(fileId));
        if (!fileBelongs) {
            throw BizException.of("该文件不属于此提交");
        }

        fileStorageService.viewFile(fileId, response);
    }

    // ==================== 内部辅助方法 ====================

    /** 作业积分统一归入「开源学习」类型；无该类型项目时返回 null（按手动积分处理） */
    private Long findOpenSourceLearningPointItemId() {
        return pointItemRepository
                .findFirstByItemTypeAndDeletedAtIsNullOrderBySortOrderAscIdAsc(PointItemTypes.OPEN_SOURCE_LEARNING)
                .map(PointItem::getId)
                .orElse(null);
    }

    private boolean isMemberInTarget(Member member, HomeworkAssignment assignment) {
        if ("ALL".equals(assignment.getTargetType())) return true;
        if ("DEPARTMENT".equals(assignment.getTargetType())) {
            return member.getDepartment() != null &&
                    member.getDepartment().equals(assignment.getTargetDepartment());
        }
        return false;
    }

    /** 部长只能访问自己部门的作业 */
    private void enforceDepartmentScope(HomeworkAssignment assignment) {
        ActorContext actor = ActorHolder.get();
        if (actor.isFullAccess()) return;

        if ("部长".equals(actor.getPosition())) {
            String actorDept = actor.getDepartment();
            if ("ALL".equals(assignment.getTargetType())) {
                throw PermissionDeniedException.of("部长只能查看本部门作业");
            }
            if (!"DEPARTMENT".equals(assignment.getTargetType()) ||
                    !actorDept.equals(assignment.getTargetDepartment())) {
                throw PermissionDeniedException.of("部长只能查看本部门作业");
            }
        }
    }

    private boolean canAccessMember(HomeworkAssignment assignment, Member member) {
        if (member == null) return false;
        ActorContext actor = ActorHolder.get();
        if (actor.isFullAccess()) return true;
        if ("部长".equals(actor.getPosition())) {
            return actor.getDepartment() != null &&
                    actor.getDepartment().equals(member.getDepartment());
        }
        return false;
    }

    private HomeworkSubmissionResponse enrichSubmissionResponse(HomeworkSubmission s, Member member,
                                                                  HomeworkAssignment assignment) {
        HomeworkSubmissionResponse r = HomeworkSubmissionResponse.from(s);
        if (assignment != null) r.setHomeworkTitle(assignment.getTitle());
        if (member != null) {
            r.setMemberName(member.getName());
            r.setMemberStudentNo(member.getStudentNo());
            r.setMemberDepartment(member.getDepartment());
            r.setMemberPosition(member.getPosition());
        }
        // 获取审核人姓名
        if (s.getReviewedByUserId() != null) {
            userAccountRepository.findById(s.getReviewedByUserId()).ifPresent(u -> r.setReviewedByName(u.getUsername()));
        }
        // 获取附件信息
        List<HomeworkSubmissionFile> submissionFiles = submissionFileRepository.findAllBySubmissionId(s.getId());
        if (!submissionFiles.isEmpty()) {
            r.setFiles(submissionFiles.stream().map(sf -> {
                FileRecord fr = fileStorageService.getFileRecord(sf.getFileId());
                HomeworkSubmissionResponse.SubmissionFileInfo fi = new HomeworkSubmissionResponse.SubmissionFileInfo();
                fi.setId(sf.getId());
                fi.setFileId(sf.getFileId());
                if (fr != null) {
                    fi.setOriginalName(fr.getOriginalName());
                    fi.setFileSize(fr.getFileSize());
                    fi.setContentType(fr.getContentType());
                }
                return fi;
            }).filter(fi -> fi.getOriginalName() != null).collect(Collectors.toList()));
        } else {
            r.setFiles(List.of());
        }
        return r;
    }

}
