package com.openatom.club;

import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.file.service.FileStorageService;
import com.openatom.club.homework.dto.GradeSubmissionRequest;
import com.openatom.club.homework.entity.HomeworkAssignment;
import com.openatom.club.homework.entity.HomeworkSubmission;
import com.openatom.club.homework.repository.HomeworkAssignmentRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionFileRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
import com.openatom.club.homework.service.HomeworkSubmissionService;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

/**
 * 作业批改积分边界单元测试：负数（含无最大积分场景）与超过最大积分都必须被后端拒绝。
 */
@ExtendWith(MockitoExtension.class)
class HomeworkSubmissionGradeTest {

    @Mock HomeworkSubmissionRepository submissionRepository;
    @Mock HomeworkSubmissionFileRepository submissionFileRepository;
    @Mock HomeworkAssignmentRepository assignmentRepository;
    @Mock MemberRepository memberRepository;
    @Mock PointRecordRepository pointRecordRepository;
    @Mock PointItemRepository pointItemRepository;
    @Mock UserAccountRepository userAccountRepository;
    @Mock FileStorageService fileStorageService;
    @Mock PermissionChecker permissionChecker;
    @Mock OperationLogService logService;

    private HomeworkSubmissionService service;

    @BeforeEach
    void setUp() {
        service = new HomeworkSubmissionService(submissionRepository, submissionFileRepository,
                assignmentRepository, memberRepository, pointRecordRepository, pointItemRepository,
                userAccountRepository, fileStorageService, permissionChecker, logService);
        ActorContext actor = new ActorContext("林涛", "秘书处", "副会长");
        actor.setUserId(1L);
        actor.setMemberId(1L);
        ActorHolder.set(actor);
    }

    @AfterEach
    void clearActor() {
        ActorHolder.clear();
    }

    private HomeworkSubmission submission(Long id, Long homeworkId, Long memberId) {
        HomeworkSubmission s = new HomeworkSubmission();
        s.setId(id);
        s.setHomeworkId(homeworkId);
        s.setMemberId(memberId);
        s.setStatus("SUBMITTED");
        return s;
    }

    private HomeworkAssignment assignment(Long id, String maxPoints) {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(id);
        a.setTitle("作业" + id);
        a.setTargetType("ALL");
        a.setMaxPoints(maxPoints == null ? null : new BigDecimal(maxPoints));
        return a;
    }

    private GradeSubmissionRequest req(String points) {
        GradeSubmissionRequest r = new GradeSubmissionRequest();
        r.setPoints(new BigDecimal(points));
        return r;
    }

    private void stubBase(Long submissionId, Long homeworkId, Long memberId, HomeworkAssignment a) {
        HomeworkSubmission sub = submission(submissionId, homeworkId, memberId);
        Member m = new Member();
        m.setId(memberId);
        m.setName("张三");
        m.setDepartment("技术部");
        when(submissionRepository.findByIdAndDeletedAtIsNull(submissionId)).thenReturn(Optional.of(sub));
        when(assignmentRepository.findByIdAndDeletedAtIsNull(homeworkId)).thenReturn(Optional.of(a));
        when(memberRepository.findById(memberId)).thenReturn(Optional.of(m));
    }

    @Test
    void grade_negativePointsRejected_whenNoMaxPoints() {
        // 无最大积分时，负数仍必须被拒绝（修复此前 maxPoints=null 时负数漏网）
        stubBase(1L, 10L, 33L, assignment(10L, null));

        BizException ex = assertThrows(BizException.class,
                () -> service.gradeSubmission(1L, req("-10")));
        assertEquals("积分不能小于 0", ex.getMessage());
    }

    @Test
    void grade_pointsExceedMaxRejected() {
        stubBase(1L, 10L, 33L, assignment(10L, "5"));

        BizException ex = assertThrows(BizException.class,
                () -> service.gradeSubmission(1L, req("6")));
        assertEquals("积分不能超过最大积分 5", ex.getMessage());
    }
}
