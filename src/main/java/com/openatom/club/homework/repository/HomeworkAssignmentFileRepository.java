package com.openatom.club.homework.repository;

import com.openatom.club.homework.entity.HomeworkAssignmentFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface HomeworkAssignmentFileRepository extends JpaRepository<HomeworkAssignmentFile, Long> {

    List<HomeworkAssignmentFile> findAllByAssignmentIdAndDeletedAtIsNull(Long assignmentId);

    List<HomeworkAssignmentFile> findAllByAssignmentIdInAndDeletedAtIsNull(List<Long> assignmentIds);

    Optional<HomeworkAssignmentFile> findByAssignmentIdAndFileIdAndDeletedAtIsNull(Long assignmentId, Long fileId);

    Optional<HomeworkAssignmentFile> findByIdAndDeletedAtIsNull(Long id);
}
