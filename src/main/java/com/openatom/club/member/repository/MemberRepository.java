package com.openatom.club.member.repository;

import com.openatom.club.member.entity.Member;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.Optional;

public interface MemberRepository extends JpaRepository<Member, Long> {
    boolean existsByStudentNoAndDeletedAtIsNull(String studentNo);

    @Query("SELECT m FROM Member m WHERE m.deletedAt IS NULL AND " +
           "(:cohortId IS NULL OR (:cohortId = -1 AND m.cohortId IS NULL) OR m.cohortId = :cohortId) AND " +
           "(:keyword IS NULL OR m.name LIKE %:keyword% OR m.studentNo LIKE %:keyword% OR " +
           "m.major LIKE %:keyword% OR m.department LIKE %:keyword% OR m.position LIKE %:keyword%)")
    Page<Member> searchMembers(@Param("cohortId") Long cohortId, @Param("keyword") String keyword, Pageable pageable);

    Optional<Member> findByIdAndDeletedAtIsNull(Long id);

    List<Member> findAllByIdInAndDeletedAtIsNull(List<Long> ids);

    List<Member> findAllByDeletedAtIsNullOrderByIdAsc();

    List<Member> findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(Long cohortId);

    List<Member> findAllByCohortIdIsNullAndDeletedAtIsNullOrderByIdAsc();

    boolean existsByStudentNoAndDeletedAtIsNullAndIdNot(String studentNo, Long id);

    long countByDeletedAtIsNull();

    long countByCohortId(Long cohortId);

    @Query("SELECT COALESCE(m.department, '未填写'), COUNT(m) FROM Member m WHERE m.deletedAt IS NULL GROUP BY m.department")
    List<Object[]> countGroupByDepartment();

    @Query("SELECT COALESCE(m.department, '未填写'), COUNT(m) FROM Member m WHERE m.deletedAt IS NULL AND " +
           "(:cohortId IS NULL OR (:cohortId = -1 AND m.cohortId IS NULL) OR m.cohortId = :cohortId) GROUP BY m.department")
    List<Object[]> countGroupByDepartmentByCohort(@Param("cohortId") Long cohortId);
}
