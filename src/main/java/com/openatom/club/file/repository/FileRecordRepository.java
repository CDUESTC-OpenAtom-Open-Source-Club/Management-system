package com.openatom.club.file.repository;

import com.openatom.club.file.entity.FileRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface FileRecordRepository extends JpaRepository<FileRecord, Long> {
    Optional<FileRecord> findByIdAndDeletedAtIsNull(Long id);
}
