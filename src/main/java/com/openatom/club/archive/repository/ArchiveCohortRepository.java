package com.openatom.club.archive.repository;

import com.openatom.club.archive.entity.ArchiveCohort;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArchiveCohortRepository extends JpaRepository<ArchiveCohort, Long> {
    List<ArchiveCohort> findByArchiveId(Long archiveId);

    List<ArchiveCohort> findByCohortId(Long cohortId);

    long countByCohortId(Long cohortId);

    void deleteByArchiveId(Long archiveId);
}
