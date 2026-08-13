package com.openatom.club.archive.repository;

import com.openatom.club.archive.entity.ArchiveLink;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface ArchiveLinkRepository extends JpaRepository<ArchiveLink, Long> {
    @Query("SELECT a FROM ArchiveLink a WHERE a.deletedAt IS NULL AND " +
           "(:year IS NULL OR a.archiveYear = :year) AND " +
           "(:type IS NULL OR a.archiveType = :type) AND " +
           "(:keyword IS NULL OR a.title LIKE %:keyword% OR a.description LIKE %:keyword%)")
    Page<ArchiveLink> search(@Param("year") Integer year,
                              @Param("type") String type,
                              @Param("keyword") String keyword,
                              Pageable pageable);

    Optional<ArchiveLink> findByIdAndDeletedAtIsNull(Long id);

    long countByDeletedAtIsNull();
}
