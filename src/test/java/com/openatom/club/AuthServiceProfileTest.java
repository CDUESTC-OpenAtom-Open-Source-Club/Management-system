package com.openatom.club;

import com.openatom.club.auth.dto.UpdateMyProfileRequest;
import com.openatom.club.auth.entity.UserAccount;
import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.auth.security.JwtTokenProvider;
import com.openatom.club.auth.service.AuthService;
import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * 个人资料自改「部门/职务」的防自提权单元测试（Mockito，不依赖 Spring 上下文 / 数据库）。
 * 覆盖：普通成员可改非管理员身份、禁止自设秘书处/部长/会长/副会长、已持有者保持不变。
 */
@ExtendWith(MockitoExtension.class)
class AuthServiceProfileTest {

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

    private UserAccount user(Long id, Long memberId) {
        UserAccount u = new UserAccount();
        u.setId(id);
        u.setUsername("u" + id);
        u.setMemberId(memberId);
        u.setEnabled(true);
        u.setProfileCompleted(false);
        u.setInitialPasswordChanged(true);
        return u;
    }

    private Member member(Long id, String studentNo, String department, String position) {
        Member m = new Member();
        m.setId(id);
        m.setName("测试成员");
        m.setStudentNo(studentNo);
        m.setDepartment(department);
        m.setPosition(position);
        return m;
    }

    private UpdateMyProfileRequest req(String studentNo, String department, String position) {
        UpdateMyProfileRequest r = new UpdateMyProfileRequest();
        r.setName("测试成员");
        r.setStudentNo(studentNo);
        r.setPhone("");
        r.setMajor("");
        r.setDepartment(department);
        r.setPosition(position);
        return r;
    }

    private void stubFindAndSave(Long userId, UserAccount u, Member m) {
        when(userRepo.findByIdAndDeletedAtIsNull(userId)).thenReturn(Optional.of(u));
        when(memberRepo.findByIdAndDeletedAtIsNull(u.getMemberId())).thenReturn(Optional.of(m));
        // 抛出校验异常时 save 不会被调用，故用 lenient 避免 UnnecessaryStubbing
        lenient().when(memberRepo.save(any(Member.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(userRepo.save(any(UserAccount.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void ordinaryMember_canChangeDepartmentToNonPrivileged() {
        Member m = member(10L, "S100", "技术部", "社员");
        stubFindAndSave(1L, user(1L, 10L), m);

        authService.updateMyProfile(1L, req("S100", "宣策部", "社员"));

        assertEquals("宣策部", m.getDepartment());
    }

    @Test
    void ordinaryMember_cannotSetDepartmentToSecretary() {
        Member m = member(10L, "S100", "技术部", "社员");
        stubFindAndSave(1L, user(1L, 10L), m);

        assertThrows(IllegalArgumentException.class,
                () -> authService.updateMyProfile(1L, req("S100", "秘书处", "社员")));
        assertEquals("技术部", m.getDepartment());
    }

    @Test
    void ordinaryMember_cannotSetPositionToMinister() {
        Member m = member(10L, "S100", "技术部", "社员");
        stubFindAndSave(1L, user(1L, 10L), m);

        assertThrows(IllegalArgumentException.class,
                () -> authService.updateMyProfile(1L, req("S100", "技术部", "部长")));
        assertEquals("社员", m.getPosition());
    }

    @Test
    void ordinaryMember_cannotSetPositionToPresident() {
        Member m = member(10L, "S100", "技术部", "社员");
        stubFindAndSave(1L, user(1L, 10L), m);

        assertThrows(IllegalArgumentException.class,
                () -> authService.updateMyProfile(1L, req("S100", "技术部", "会长")));
        assertEquals("社员", m.getPosition());
    }

    @Test
    void ministerKeepsOwnPosition() {
        Member m = member(10L, "S100", "技术部", "部长");
        stubFindAndSave(1L, user(1L, 10L), m);

        authService.updateMyProfile(1L, req("S100", "技术部", "部长"));

        assertEquals("部长", m.getPosition());
    }

    @Test
    void secretaryKeepsOwnDepartmentAndPosition() {
        Member m = member(10L, "S100", "秘书处", "副会长");
        stubFindAndSave(1L, user(1L, 10L), m);

        authService.updateMyProfile(1L, req("S100", "秘书处", "副会长"));

        assertEquals("秘书处", m.getDepartment());
        assertEquals("副会长", m.getPosition());
    }
}
