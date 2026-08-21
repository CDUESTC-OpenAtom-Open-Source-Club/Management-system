package com.openatom.club.auth.repository;

import com.openatom.club.auth.entity.UserAccount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserAccountRepository extends JpaRepository<UserAccount, Long> {

    Optional<UserAccount> findByUsernameAndDeletedAtIsNull(String username);

    Optional<UserAccount> findByIdAndDeletedAtIsNull(Long id);

    java.util.List<UserAccount> findAllByMemberIdInAndDeletedAtIsNull(java.util.List<Long> memberIds);

    boolean existsByUsernameAndDeletedAtIsNull(String username);

    @Query("""
        SELECT u FROM UserAccount u
        LEFT JOIN Member m ON m.id = u.memberId
        WHERE u.deletedAt IS NULL
          AND (:cohortId IS NULL OR (:cohortId = -1 AND m.cohortId IS NULL) OR m.cohortId = :cohortId)
          AND (:keyword IS NULL OR :keyword = ''
               OR LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(m.name)     LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(m.studentNo) LIKE LOWER(CONCAT('%', :keyword, '%')))
        ORDER BY u.createdAt DESC
    """)
    Page<UserAccount> searchUsers(@Param("keyword") String keyword, @Param("cohortId") Long cohortId, Pageable pageable);

    long countByProfileCompletedAndDeletedAtIsNull(boolean profileCompleted);

    /**
     * 待完善资料的成员数：只统计「资料未完善」且「绑定成员未被软删除」的账号，
     * 避免把绑定了已删除成员的孤儿账号计入。
     */
    @Query("SELECT COUNT(u) FROM UserAccount u " +
           "JOIN com.openatom.club.member.entity.Member m ON m.id = u.memberId AND m.deletedAt IS NULL " +
           "WHERE u.deletedAt IS NULL AND u.profileCompleted = false")
    long countIncompleteProfilesOfActiveMembers();
}
