package com.openatom.club;

import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.file.service.FileStorageService;
import com.openatom.club.homework.entity.HomeworkAssignment;
import com.openatom.club.homework.entity.HomeworkSubmission;
import com.openatom.club.homework.repository.HomeworkAssignmentFileRepository;
import com.openatom.club.homework.repository.HomeworkAssignmentRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
import com.openatom.club.homework.service.HomeworkService;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.entity.PointRecord;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 作业批量删除单元测试（Mockito，不依赖 Spring 上下文 / 数据库）。
 * 覆盖：批量软删除、有批改记录时级联删除积分、空列表拒绝。
 */
@ExtendWith(MockitoExtension.class)
class HomeworkServiceBatchDeleteTest {

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
        ActorHolder.set(actor);
    }

    @AfterEach
    void clearActor() {
        ActorHolder.clear();
    }

    private HomeworkAssignment assignment(Long id, String title) {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(id);
        a.setTitle(title);
        a.setTargetType("ALL");
        a.setStatus("PUBLISHED");
        return a;
    }

    @Test
    void batchDelete_softDeletesAll() {
        HomeworkAssignment a1 = assignment(1L, "A1");
        HomeworkAssignment a2 = assignment(2L, "A2");
        when(assignmentRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(a1, a2));

        int count = homeworkService.deleteAssignments(List.of(1L, 2L));

        assertEquals(2, count);
        assertNotNull(a1.getDeletedAt());
        assertNotNull(a2.getDeletedAt());
        verify(assignmentRepository).saveAll(anyList());
    }

    @Test
    void batchDelete_withGradedSubmission_cascadesPointRecord() {
        HomeworkAssignment a1 = assignment(1L, "A1");
        HomeworkSubmission sub = new HomeworkSubmission();
        sub.setId(10L);
        sub.setHomeworkId(1L);
        sub.setMemberId(33L);
        sub.setStatus("GRADED");
        sub.setPointRecordId(99L);
        PointRecord record = new PointRecord();
        record.setId(99L);
        record.setMemberId(33L);
        record.setSourceType("HOMEWORK");
        record.setScore(new BigDecimal("5"));

        when(assignmentRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(a1));
        when(submissionRepository.findAllByHomeworkIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(sub));
        when(pointRecordRepository.findAllByIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(record));

        int count = homeworkService.deleteAssignments(List.of(1L));

        assertEquals(1, count);
        assertNotNull(a1.getDeletedAt(), "作业应被软删除");
        assertNotNull(record.getDeletedAt(), "批改积分应被级联软删除");
        assertNull(sub.getPointRecordId(), "提交的 point_record_id 应被清空");
    }

    @Test
    void batchDelete_emptyIds_rejected() {
        assertThrows(BizException.class, () -> homeworkService.deleteAssignments(List.of()));
        verify(assignmentRepository, never()).findAllByIdInAndDeletedAtIsNull(anyList());
    }
}
