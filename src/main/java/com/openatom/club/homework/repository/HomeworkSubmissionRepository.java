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
}
