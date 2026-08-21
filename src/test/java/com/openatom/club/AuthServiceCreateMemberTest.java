package com.openatom.club;

import com.openatom.club.auth.dto.BatchCreateAccountItem;
import com.openatom.club.auth.dto.BatchCreateUsersRequest;
import com.openatom.club.auth.dto.BatchCreateUsersResponse;
import com.openatom.club.auth.dto.CreateUserRequest;
import com.openatom.club.auth.entity.UserAccount;
import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.auth.security.JwtTokenProvider;
import com.openatom.club.auth.service.AuthService;
import com.openatom.club.cohort.entity.Cohort;
import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * 业务不变量单元测试：Member 只能在创建 UserAccount 时由系统自动创建。
 * 覆盖：createUser / batchCreateUsers 成功后必定创建并绑定默认 Member，
 * 且默认档案字段与 profileCompleted=false。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceCreateMemberTest {

    @Mock UserAccountRepository userRepo;
    @Mock MemberRepository memberRepo;
    @Mock CohortRepository cohortRepository;
    @Mock JwtTokenProvider jwtTokenProvider;
    @Mock PasswordEncoder passwordEncoder;
    @Mock OperationLogService logService;
    @Mock PermissionChecker permissionChecker;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        authService = new AuthService(userRepo, memberRepo, cohortRepository,
                jwtTokenProvider, passwordEncoder, logService, permissionChecker);
    }

    private Cohort cohort(Long id, Integer year) {
        Cohort c = new Cohort();
        c.setId(id);
        c.setYear(year);
        c.setEnabled(true);
        return c;
    }

    private CreateUserRequest createUserRequest(String username, Long cohortId) {
        CreateUserRequest r = new CreateUserRequest();
        r.setUsername(username);
        r.setInitialPassword("123456");
        r.setCohortId(cohortId);
        return r;
    }

    @Test
    void createUser_createsAndBindsDefaultMember() {
        when(cohortRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(cohort(5L, 2026)));
        when(userRepo.existsByUsernameAndDeletedAtIsNull("stu1")).thenReturn(false);
        when(passwordEncoder.encode("123456")).thenReturn("encoded");
        when(memberRepo.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            m.setId(100L);
            return m;
        });
        when(userRepo.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cohortRepository.findAllByDeletedAtIsNullOrderByYearDesc()).thenReturn(List.of());

        authService.createUser(createUserRequest("stu1", 5L));

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepo).save(memberCaptor.capture());
        Member m = memberCaptor.getValue();
        assertEquals("stu1", m.getName());
        assertEquals("stu1", m.getStudentNo());
        assertEquals("", m.getPhone());
        assertEquals("", m.getMajor());
        assertEquals("其他", m.getDepartment());
        assertEquals("社员", m.getPosition());
        assertEquals(5L, m.getCohortId());

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepo).save(userCaptor.capture());
        UserAccount u = userCaptor.getValue();
        assertEquals(100L, u.getMemberId());
        assertFalse(Boolean.TRUE.equals(u.getProfileCompleted()));
    }

    @Test
    void batchCreateUsers_createsAndBindsMemberPerAccount() {
        when(cohortRepository.findByIdAndDeletedAtIsNull(5L)).thenReturn(Optional.of(cohort(5L, 2026)));
        when(userRepo.existsByUsernameAndDeletedAtIsNull(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded");
        AtomicLong idGen = new AtomicLong(100);
        when(memberRepo.save(any(Member.class))).thenAnswer(inv -> {
            Member m = inv.getArgument(0);
            if (m.getId() == null) m.setId(idGen.getAndIncrement());
            return m;
        });
        when(userRepo.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));

        BatchCreateAccountItem item1 = new BatchCreateAccountItem();
        item1.setUsername("stu1");
        item1.setInitialPassword("123456");
        BatchCreateAccountItem item2 = new BatchCreateAccountItem();
        item2.setUsername("stu2");
        item2.setInitialPassword("123456");

        BatchCreateUsersRequest req = new BatchCreateUsersRequest();
        req.setCohortId(5L);
        req.setAccounts(List.of(item1, item2));

        BatchCreateUsersResponse resp = authService.batchCreateUsers(req);

        assertEquals(2, resp.getCreated().size());
        assertEquals(0, resp.getFailed().size());

        ArgumentCaptor<Member> memberCaptor = ArgumentCaptor.forClass(Member.class);
        verify(memberRepo, times(2)).save(memberCaptor.capture());
        List<Member> members = memberCaptor.getAllValues();
        assertEquals("stu1", members.get(0).getName());
        assertEquals("stu1", members.get(0).getStudentNo());
        assertEquals("其他", members.get(0).getDepartment());
        assertEquals("社员", members.get(0).getPosition());
        assertEquals(5L, members.get(0).getCohortId());
        assertEquals("stu2", members.get(1).getName());
        assertEquals("stu2", members.get(1).getStudentNo());

        ArgumentCaptor<UserAccount> userCaptor = ArgumentCaptor.forClass(UserAccount.class);
        verify(userRepo, times(2)).save(userCaptor.capture());
        List<UserAccount> accounts = userCaptor.getAllValues();
        assertTrue(accounts.stream().allMatch(u -> u.getMemberId() != null));
        assertTrue(accounts.stream().allMatch(u -> !Boolean.TRUE.equals(u.getProfileCompleted())));
    }
}
