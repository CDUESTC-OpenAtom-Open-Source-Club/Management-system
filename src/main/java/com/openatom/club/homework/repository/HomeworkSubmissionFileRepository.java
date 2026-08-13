package com.openatom.club.homework.repository;

import com.openatom.club.homework.entity.HomeworkSubmissionFile;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface HomeworkSubmissionFileRepository extends JpaRepository<HomeworkSubmissionFile, Long> {

    List<HomeworkSubmissionFile> findAllBySubmissionId(Long submissionId);

    void deleteAllBySubmissionId(Long submissionId);
}
