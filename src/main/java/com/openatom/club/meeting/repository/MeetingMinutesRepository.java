package com.openatom.club.meeting.repository;

import com.openatom.club.meeting.entity.MeetingMinutes;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface MeetingMinutesRepository extends JpaRepository<MeetingMinutes, Long> {
    @Query("SELECT m FROM MeetingMinutes m WHERE m.deletedAt IS NULL AND " +
           "(:year IS NULL OR m.meetingYear = :year) AND " +
           "(:month IS NULL OR m.meetingMonth = :month) AND " +
           "(:keyword IS NULL OR m.title LIKE %:keyword%)")
    Page<MeetingMinutes> search(@Param("year") Integer year,
                                 @Param("month") Integer month,
                                 @Param("keyword") String keyword,
                                 Pageable pageable);

    Optional<MeetingMinutes> findByIdAndDeletedAtIsNull(Long id);

    long countByDeletedAtIsNull();
}
