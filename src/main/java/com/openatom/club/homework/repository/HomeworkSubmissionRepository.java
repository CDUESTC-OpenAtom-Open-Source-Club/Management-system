package com.openatom.club.homework.repository;

import com.openatom.club.homework.entity.HomeworkSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface HomeworkSubmissionRepository extends JpaRepository<HomeworkSubmission, Long> {

    Optional<HomeworkSubmission> findByIdAndDeletedAtIsNull(Long id);

    Optional<HomeworkSubmission> findByHomeworkIdAndMemberIdAndDeletedAtIsNull(Long homeworkId, Long memberId);

    List<HomeworkSubmission> findAllByHomeworkIdAndDeletedAtIsNull(Long homeworkId);

    List<HomeworkSubmission> findAllByHomeworkIdInAndDeletedAtIsNull(List<Long> homeworkIds);

    List<HomeworkSubmission> findAllByMemberIdInAndDeletedAtIsNull(List<Long> memberIds);

    @Query("SELECT s FROM HomeworkSubmission s WHERE s.homeworkId = :homeworkId AND s.deletedAt IS NULL " +
           "AND (:status IS NULL OR s.status = :status)")
    Page<HomeworkSubmission> findByHomeworkIdWithStatus(@Param("homeworkId") Long homeworkId,
                                                         @Param("status") String status,
                                                         Pageable pageable);

    @Query("SELECT COUNT(s) FROM HomeworkSubmission s WHERE s.homeworkId = :homeworkId AND s.deletedAt IS NULL")
    long countByHomeworkId(@Param("homeworkId") Long homeworkId);

    @Query("SELECT COUNT(s) FROM HomeworkSubmission s WHERE s.homeworkId = :homeworkId AND s.deletedAt IS NULL AND s.status = :status")
    long countByHomeworkIdAndStatus(@Param("homeworkId") Long homeworkId, @Param("status") String status);

    @Query("SELECT s FROM HomeworkSubmission s WHERE s.deletedAt IS NULL AND s.status = :status")
    Page<HomeworkSubmission> findByStatus(@Param("status") String status, Pageable pageable);

    /**
     * 待批改作业数（status = SUBMITTED），按届次 + 提交人部门过滤。
     * department 为 null 表示不限部门（fullAccess）；cohortId 语义与成员一致：null=全部、-1=未分届、正数=指定届次。
     */
    @Query("SELECT COUNT(s) FROM HomeworkSubmission s " +
           "JOIN com.openatom.club.member.entity.Member m ON m.id = s.memberId AND m.deletedAt IS NULL " +
           "WHERE s.deletedAt IS NULL AND s.status = 'SUBMITTED' " +
           "AND (:department IS NULL OR m.department = :department) " +
           "AND (:cohortId IS NULL OR (:cohortId = -1 AND m.cohortId IS NULL) OR m.cohortId = :cohortId)")
    long countPendingReviews(@Param("department") String department, @Param("cohortId") Long cohortId);
}
