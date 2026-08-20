package com.openatom.club;

import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.file.service.FileStorageService;
import com.openatom.club.homework.dto.HomeworkAssignmentResponse;
import com.openatom.club.homework.entity.HomeworkAssignment;
import com.openatom.club.homework.entity.HomeworkSubmission;
import com.openatom.club.homework.repository.HomeworkAssignmentFileRepository;
import com.openatom.club.homework.repository.HomeworkAssignmentRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
import com.openatom.club.homework.service.HomeworkService;
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

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * 「我的作业」列表的提交状态回归测试。
 * 覆盖：他人已提交时，未提交成员不应被误判为「已提交」。
 */
@ExtendWith(MockitoExtension.class)
class HomeworkServiceMyListTest {

    @Mock HomeworkAssignmentRepository assignmentRepository;
    @Mock HomeworkSubmissionRepository submissionRepository;
    @Mock HomeworkAssignmentFileRepository assignmentFileRepository;
    @Mock FileStorageService fileStorageService;
    @Mock MemberRepository memberRepository;
    @Mock PointItemRepository pointItemRepository;
    @Mock PointRecordRepository pointRecordRepository;
    @Mock CohortRepository cohortRepository;
    @Mock PermissionChecker permissionChecker;
    @Mock OperationLogService logService;

    private HomeworkService homeworkService;

    private Member member;
    private HomeworkAssignment assignment;

    @BeforeEach
    void setUp() {
        homeworkService = new HomeworkService(assignmentRepository, submissionRepository,
                assignmentFileRepository, memberRepository, pointItemRepository, pointRecordRepository,
                cohortRepository, permissionChecker, logService, fileStorageService);

        member = new Member();
        member.setId(10L);
        member.setName("测试成员");
        member.setStudentNo("S100");
        member.setDepartment("技术部");
        member.setPosition("社员");

        assignment = new HomeworkAssignment();
        assignment.setId(100L);
        assignment.setTitle("测试作业");
        assignment.setTargetType("ALL");
        assignment.setStatus("PUBLISHED");
        assignment.setCohortId(null);
        assignment.setCreatedAt(OffsetDateTime.now());

        ActorContext actor = new ActorContext("测试成员", "技术部", "社员");
        actor.setMemberId(10L);
        actor.setCohortId(null);
        ActorHolder.set(actor);

        when(memberRepository.findByIdAndDeletedAtIsNull(10L)).thenReturn(Optional.of(member));
        when(assignmentRepository.findAll()).thenReturn(List.of(assignment));
        // 模拟「其他人已提交」的全局计数：若无修正，submittedCount 会是 2、gradedCount 会是 1
        when(submissionRepository.countByHomeworkId(100L)).thenReturn(3L);
        when(submissionRepository.countByHomeworkIdAndStatus(100L, "SUBMITTED")).thenReturn(2L);
        when(submissionRepository.countByHomeworkIdAndStatus(100L, "GRADED")).thenReturn(1L);
    }

    @AfterEach
    void clearActor() {
        ActorHolder.clear();
    }

    private HomeworkAssignmentResponse first() {
        List<HomeworkAssignmentResponse> list = homeworkService.listMyHomework();
        assertEquals(1, list.size());
        return list.get(0);
    }

    @Test
    void noSubmission_seesZeroCounts_evenIfOthersSubmitted() {
        when(submissionRepository.findByHomeworkIdAndMemberIdAndDeletedAtIsNull(100L, 10L))
                .thenReturn(Optional.empty());

        HomeworkAssignmentResponse r = first();

        assertEquals(0, r.getSubmittedCount(), "未提交成员不应显示「已提交」");
        assertEquals(0, r.getGradedCount());
    }

    @Test
    void submittedButNotGraded_seesSubmitted() {
        HomeworkSubmission sub = new HomeworkSubmission();
        sub.setId(200L);
        sub.setHomeworkId(100L);
        sub.setMemberId(10L);
        sub.setStatus("SUBMITTED");
        when(submissionRepository.findByHomeworkIdAndMemberIdAndDeletedAtIsNull(100L, 10L))
                .thenReturn(Optional.of(sub));

        HomeworkAssignmentResponse r = first();

        assertEquals(1, r.getSubmittedCount());
        assertEquals(0, r.getGradedCount());
    }

    @Test
    void gradedSubmission_seesGraded() {
        HomeworkSubmission sub = new HomeworkSubmission();
        sub.setId(201L);
        sub.setHomeworkId(100L);
        sub.setMemberId(10L);
        sub.setStatus("GRADED");
        when(submissionRepository.findByHomeworkIdAndMemberIdAndDeletedAtIsNull(100L, 10L))
                .thenReturn(Optional.of(sub));

        HomeworkAssignmentResponse r = first();

        assertEquals(1, r.getSubmittedCount());
        assertEquals(1, r.getGradedCount());
    }
}
