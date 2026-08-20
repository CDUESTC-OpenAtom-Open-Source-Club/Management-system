package com.openatom.club;

import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.file.service.FileStorageService;
import com.openatom.club.homework.dto.HomeworkAssignmentResponse;
import com.openatom.club.homework.entity.HomeworkAssignment;
import com.openatom.club.homework.repository.HomeworkAssignmentFileRepository;
import com.openatom.club.homework.repository.HomeworkAssignmentRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
import com.openatom.club.homework.service.HomeworkService;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/**
 * 「提交/已批」统计语义回归测试。
 * submissionCount = 所有有效提交（含已批改），批改不减少；
 * submittedCount = 仅 SUBMITTED（待批改）；gradedCount = GRADED。
 */
@ExtendWith(MockitoExtension.class)
class HomeworkServiceCountSemanticsTest {

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

    @BeforeEach
    void setUp() {
        homeworkService = new HomeworkService(assignmentRepository, submissionRepository,
                assignmentFileRepository, memberRepository, pointItemRepository, pointRecordRepository,
                cohortRepository, permissionChecker, logService, fileStorageService);
        ActorContext actor = new ActorContext("林涛", "秘书处", "副会长");
        actor.setUserId(1L);
        actor.setMemberId(1L);
        actor.setCohortId(6L);
        ActorHolder.set(actor);
    }

    @AfterEach
    void clearActor() {
        ActorHolder.clear();
    }

    private HomeworkAssignment assignment() {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(1L);
        a.setTitle("测试作业");
        a.setTargetType("ALL");
        a.setStatus("PUBLISHED");
        return a;
    }

    private HomeworkAssignmentResponse getDetail(long total, long submitted, long graded) {
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(assignment()));
        when(submissionRepository.countByHomeworkId(1L)).thenReturn(total);
        when(submissionRepository.countByHomeworkIdAndStatus(1L, "SUBMITTED")).thenReturn(submitted);
        when(submissionRepository.countByHomeworkIdAndStatus(1L, "GRADED")).thenReturn(graded);
        return homeworkService.getAssignment(1L);
    }

    @Test
    void singleSubmissionGraded_submissionCountStaysOne() {
        // 场景2：1 人提交后批改，submissionCount 绝不能变成 0
        HomeworkAssignmentResponse r = getDetail(1L, 0L, 1L);
        assertEquals(1L, r.getSubmissionCount());
        assertEquals(0L, r.getSubmittedCount());
        assertEquals(1L, r.getGradedCount());
    }

    @Test
    void mixedStatuses_submissionCountIsTotal() {
        // 场景3：3 人提交，其中 1 人已批改
        HomeworkAssignmentResponse r = getDetail(3L, 2L, 1L);
        assertEquals(3L, r.getSubmissionCount());
        assertEquals(2L, r.getSubmittedCount());
        assertEquals(1L, r.getGradedCount());
    }

    @Test
    void allGraded_submissionCountIsTotal() {
        // 场景4：3 人全部批改
        HomeworkAssignmentResponse r = getDetail(3L, 0L, 3L);
        assertEquals(3L, r.getSubmissionCount());
        assertEquals(0L, r.getSubmittedCount());
        assertEquals(3L, r.getGradedCount());
    }
}
