package com.openatom.club;

import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.exception.PermissionDeniedException;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.file.entity.FileRecord;
import com.openatom.club.file.service.FileStorageService;
import com.openatom.club.homework.dto.HomeworkAssignmentFileResponse;
import com.openatom.club.homework.entity.HomeworkAssignment;
import com.openatom.club.homework.entity.HomeworkAssignmentFile;
import com.openatom.club.homework.repository.HomeworkAssignmentFileRepository;
import com.openatom.club.homework.repository.HomeworkAssignmentRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
import com.openatom.club.homework.service.HomeworkService;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.multipart.MultipartFile;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 作业发布附件（homework_assignment_files）单元测试。
 * 覆盖：成员下载权限（同届/部门/草稿/跨届跨部门）、部长跨届本部门、fullAccess 跨届跨部门、
 * 删除权限、附件删除、作业删除级联、空附件。
 */
@ExtendWith(MockitoExtension.class)
class HomeworkServiceAttachmentTest {

    @Mock HomeworkAssignmentRepository assignmentRepository;
    @Mock HomeworkSubmissionRepository submissionRepository;
    @Mock HomeworkAssignmentFileRepository assignmentFileRepository;
    @Mock FileStorageService fileStorageService;
    @Mock MemberRepository memberRepository;
    @Mock PointItemRepository pointItemRepository;
    @Mock PointRecordRepository pointRecordRepository;
    @Mock CohortRepository cohortRepository;
    @Mock OperationLogService logService;

    // 使用真实 PermissionChecker，让 requireManageHomework() 真正按 ActorHolder 生效
    private final PermissionChecker permissionChecker = new PermissionChecker();

    private HomeworkService homeworkService;

    @BeforeEach
    void setUp() {
        homeworkService = new HomeworkService(assignmentRepository, submissionRepository,
                assignmentFileRepository, memberRepository, pointItemRepository, pointRecordRepository,
                cohortRepository, permissionChecker, logService, fileStorageService);
    }

    @AfterEach
    void clearActor() {
        ActorHolder.clear();
    }

    // ==================== 辅助 ====================

    private HomeworkAssignment assignment(Long id, String status, Long cohortId,
                                          String targetType, String targetDept) {
        HomeworkAssignment a = new HomeworkAssignment();
        a.setId(id);
        a.setTitle("作业" + id);
        a.setStatus(status);
        a.setCohortId(cohortId);
        a.setTargetType(targetType);
        a.setTargetDepartment(targetDept);
        return a;
    }

    private HomeworkAssignmentFile af(Long relationId, Long assignmentId, Long fileId) {
        HomeworkAssignmentFile f = new HomeworkAssignmentFile();
        f.setId(relationId);
        f.setAssignmentId(assignmentId);
        f.setFileId(fileId);
        f.setCreatedAt(OffsetDateTime.now());
        return f;
    }

    private void setActor(String name, String dept, String position, Long memberId, Long cohortId) {
        ActorContext actor = new ActorContext(name, dept, position);
        actor.setUserId(memberId);
        actor.setMemberId(memberId);
        actor.setCohortId(cohortId);
        ActorHolder.set(actor);
    }

    // ==================== 成员下载权限 ====================

    @Test
    void member_downloadOwnCohortInRange_success() throws Exception {
        setActor("张三", "技术部", "社员", 2L, 5L);
        HomeworkAssignment a = assignment(1L, "PUBLISHED", 5L, "ALL", null);
        HomeworkAssignmentFile f = af(10L, 1L, 100L);
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));
        when(assignmentFileRepository.findByAssignmentIdAndFileIdAndDeletedAtIsNull(1L, 100L))
                .thenReturn(Optional.of(f));
        HttpServletResponse resp = mock(HttpServletResponse.class);

        assertDoesNotThrow(() -> homeworkService.downloadAssignmentFile(1L, 100L, resp));
        verify(fileStorageService).downloadFile(100L, resp);
    }

    @Test
    void member_downloadOtherCohort_forbidden() throws Exception {
        setActor("张三", "技术部", "社员", 2L, 6L); // 2026
        HomeworkAssignment a = assignment(1L, "PUBLISHED", 5L, "ALL", null); // 2025
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));

        assertThrows(PermissionDeniedException.class,
                () -> homeworkService.downloadAssignmentFile(1L, 100L, mock(HttpServletResponse.class)));
        verify(fileStorageService, never()).downloadFile(anyLong(), any());
    }

    @Test
    void member_downloadOtherDept_forbidden() throws Exception {
        setActor("张三", "宣策部", "社员", 2L, 5L);
        HomeworkAssignment a = assignment(1L, "PUBLISHED", 5L, "DEPARTMENT", "技术部");
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));

        assertThrows(PermissionDeniedException.class,
                () -> homeworkService.downloadAssignmentFile(1L, 100L, mock(HttpServletResponse.class)));
        verify(fileStorageService, never()).downloadFile(anyLong(), any());
    }

    @Test
    void member_downloadDraft_forbidden() throws Exception {
        setActor("张三", "技术部", "社员", 2L, 5L);
        HomeworkAssignment a = assignment(1L, "DRAFT", 5L, "ALL", null);
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));

        assertThrows(PermissionDeniedException.class,
                () -> homeworkService.downloadAssignmentFile(1L, 100L, mock(HttpServletResponse.class)));
        verify(fileStorageService, never()).downloadFile(anyLong(), any());
    }

    @Test
    void member_delete_forbidden() {
        setActor("张三", "技术部", "社员", 2L, 5L);

        assertThrows(PermissionDeniedException.class,
                () -> homeworkService.deleteAssignmentFile(1L, 100L));
        verify(assignmentFileRepository, never()).save(any());
    }

    // ==================== 部长：届次自由、部门固定本部门 ====================

    @Test
    void minister_uploadOtherCohortSameDept_success() throws Exception {
        setActor("部长", "技术部", "部长", 3L, 6L); // 2026
        HomeworkAssignment a = assignment(1L, "DRAFT", 5L, "DEPARTMENT", "技术部"); // 2025 本部门
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));

        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        FileRecord record = new FileRecord();
        record.setId(100L);
        record.setOriginalName("实验说明.pdf");
        when(fileStorageService.saveFile(any(), eq("homework-assignment"), eq("homework-assignment/1"), any()))
                .thenReturn(record);
        when(fileStorageService.getFileRecord(100L)).thenReturn(record);

        List<HomeworkAssignmentFileResponse> result = homeworkService.uploadAssignmentFiles(1L, List.of(file));

        assertEquals(1, result.size());
        assertEquals("实验说明.pdf", result.get(0).getOriginalName());
    }

    @Test
    void minister_downloadOtherCohortSameDept_success() throws Exception {
        setActor("部长", "技术部", "部长", 3L, 6L); // 2026
        HomeworkAssignment a = assignment(1L, "PUBLISHED", 5L, "DEPARTMENT", "技术部"); // 2025 本部门
        HomeworkAssignmentFile f = af(10L, 1L, 100L);
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));
        when(assignmentFileRepository.findByAssignmentIdAndFileIdAndDeletedAtIsNull(1L, 100L))
                .thenReturn(Optional.of(f));
        HttpServletResponse resp = mock(HttpServletResponse.class);

        assertDoesNotThrow(() -> homeworkService.downloadAssignmentFile(1L, 100L, resp));
        verify(fileStorageService).downloadFile(100L, resp);
    }

    @Test
    void minister_deleteOtherDept_forbidden() {
        setActor("部长", "技术部", "部长", 3L, 5L);
        HomeworkAssignment a = assignment(1L, "DRAFT", 5L, "DEPARTMENT", "宣策部");
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));

        assertThrows(PermissionDeniedException.class,
                () -> homeworkService.deleteAssignmentFile(1L, 100L));
        verify(assignmentFileRepository, never()).save(any());
    }

    // ==================== fullAccess：跨届跨部门 ====================

    @Test
    void fullAccess_downloadCrossCohortDept_success() throws Exception {
        setActor("林涛", "秘书处", "副会长", 1L, 6L);
        HomeworkAssignment a = assignment(1L, "DRAFT", 5L, "DEPARTMENT", "宣策部");
        HomeworkAssignmentFile f = af(10L, 1L, 100L);
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));
        when(assignmentFileRepository.findByAssignmentIdAndFileIdAndDeletedAtIsNull(1L, 100L))
                .thenReturn(Optional.of(f));
        HttpServletResponse resp = mock(HttpServletResponse.class);

        assertDoesNotThrow(() -> homeworkService.downloadAssignmentFile(1L, 100L, resp));
        verify(fileStorageService).downloadFile(100L, resp);
    }

    // ==================== 删除 ====================

    @Test
    void deleteFile_softDeletesRelationAndFileRecord() {
        setActor("林涛", "秘书处", "副会长", 1L, 6L);
        HomeworkAssignment a = assignment(1L, "DRAFT", 5L, "ALL", null);
        HomeworkAssignmentFile f = af(10L, 1L, 100L);
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));
        when(assignmentFileRepository.findByAssignmentIdAndFileIdAndDeletedAtIsNull(1L, 100L))
                .thenReturn(Optional.of(f));

        homeworkService.deleteAssignmentFile(1L, 100L);

        assertNotNull(f.getDeletedAt(), "附件关系应被软删除");
        verify(fileStorageService).softDeleteFileRecord(100L);
        verify(assignmentFileRepository).save(f);
    }

    @Test
    void deleteAssignment_cascadesAttachmentFiles() {
        setActor("林涛", "秘书处", "副会长", 1L, 6L);
        HomeworkAssignment a = assignment(1L, "PUBLISHED", 5L, "ALL", null);
        HomeworkAssignmentFile f = af(10L, 1L, 100L);
        when(assignmentRepository.findAllByIdInAndDeletedAtIsNull(anyList())).thenReturn(List.of(a));
        when(assignmentFileRepository.findAllByAssignmentIdInAndDeletedAtIsNull(anyList()))
                .thenReturn(List.of(f));

        int count = homeworkService.deleteAssignments(List.of(1L));

        assertEquals(1, count);
        assertNotNull(f.getDeletedAt(), "作业删除后附件关系应被级联软删除");
        verify(fileStorageService).softDeleteFileRecord(100L);
    }

    // ==================== 空附件 / 列表 ====================

    @Test
    void upload_emptyFiles_noRelationCreated() throws Exception {
        setActor("林涛", "秘书处", "副会长", 1L, 6L);
        HomeworkAssignment a = assignment(1L, "DRAFT", 5L, "ALL", null);
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));

        List<HomeworkAssignmentFileResponse> result = homeworkService.uploadAssignmentFiles(1L, List.of());

        assertTrue(result.isEmpty());
        verify(fileStorageService, never()).saveFile(any(), any(), any(), any());
    }

    @Test
    void listFiles_noAttachments_returnsEmpty() {
        setActor("张三", "技术部", "社员", 2L, 5L);
        HomeworkAssignment a = assignment(1L, "PUBLISHED", 5L, "ALL", null);
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));
        when(assignmentFileRepository.findAllByAssignmentIdAndDeletedAtIsNull(1L)).thenReturn(List.of());

        List<HomeworkAssignmentFileResponse> result = homeworkService.listAssignmentFiles(1L);

        assertTrue(result.isEmpty());
    }

    @Test
    void listFiles_multipleAttachments_returnsMeta() {
        setActor("林涛", "秘书处", "副会长", 1L, 6L);
        HomeworkAssignment a = assignment(1L, "DRAFT", 5L, "ALL", null);
        HomeworkAssignmentFile f1 = af(10L, 1L, 100L);
        HomeworkAssignmentFile f2 = af(11L, 1L, 101L);
        FileRecord r1 = new FileRecord();
        r1.setId(100L);
        r1.setOriginalName("实验说明.pdf");
        r1.setFileSize(1024L);
        FileRecord r2 = new FileRecord();
        r2.setId(101L);
        r2.setOriginalName("代码模板.zip");
        r2.setFileSize(2048L);
        when(assignmentRepository.findByIdAndDeletedAtIsNull(1L)).thenReturn(Optional.of(a));
        when(assignmentFileRepository.findAllByAssignmentIdAndDeletedAtIsNull(1L)).thenReturn(List.of(f1, f2));
        when(fileStorageService.getFileRecord(100L)).thenReturn(r1);
        when(fileStorageService.getFileRecord(101L)).thenReturn(r2);

        List<HomeworkAssignmentFileResponse> result = homeworkService.listAssignmentFiles(1L);

        assertEquals(2, result.size());
        assertEquals("实验说明.pdf", result.get(0).getOriginalName());
        assertEquals("代码模板.zip", result.get(1).getOriginalName());
    }
}
