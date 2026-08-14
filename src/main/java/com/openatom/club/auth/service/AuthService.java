package com.openatom.club.auth.service;

import com.openatom.club.auth.dto.*;
import com.openatom.club.auth.entity.UserAccount;
import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.auth.security.JwtTokenProvider;
import com.openatom.club.cohort.entity.Cohort;
import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.exception.PermissionDeniedException;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserAccountRepository userRepo;
    private final MemberRepository memberRepo;
    private final CohortRepository cohortRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;
    private final OperationLogService logService;
    private final PermissionChecker permissionChecker;

    @Transactional
    public LoginResponse login(LoginRequest req) {
        UserAccount user = userRepo.findByUsernameAndDeletedAtIsNull(req.getUsername())
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!Boolean.TRUE.equals(user.getEnabled())) {
            throw new IllegalArgumentException("账号已被禁用，请联系管理员");
        }
        if (!passwordEncoder.matches(req.getPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        user.setLastLoginAt(OffsetDateTime.now());
        userRepo.save(user);
        return LoginResponse.builder()
                .token(jwtTokenProvider.generateToken(user.getId(), user.getUsername()))
                .user(buildCurrentUser(user))
                .build();
    }

    public CurrentUserResponse getCurrentUser(Long userId) {
        return buildCurrentUser(findUser(userId));
    }

    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest req) {
        UserAccount user = findUser(userId);
        if (!passwordEncoder.matches(req.getOldPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("旧密码不正确");
        }
        user.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        user.setInitialPasswordChanged(true);
        userRepo.save(user);
        logService.log("auth", "UPDATE", String.valueOf(userId), "修改密码");
    }

    /**
     * @param cohortId null=全部；-1=未分届；其他=指定届次
     */
    public PageResult<UserAccountResponse> listUsers(String keyword, Long cohortId, int page, int size) {
        permissionChecker.requireUserManage();
        Page<UserAccount> pg = userRepo.searchUsers(
                StringUtils.hasText(keyword) ? keyword.trim() : null,
                cohortId,
                PageRequest.of(Math.max(page - 1, 0), size));
        Map<Long, Integer> years = cohortYearMap();
        List<UserAccountResponse> list = pg.getContent().stream()
                .map(u -> toUserResponse(u, years)).toList();
        return new PageResult<>(list, pg.getTotalElements(), page, size);
    }

    @Transactional
    public UserAccountResponse createUser(CreateUserRequest req) {
        permissionChecker.requireUserManage();
        String username = normalizeUsername(req.getUsername());
        validateNewPassword(req.getInitialPassword(), "初始密码");
        requireActiveCohort(req.getCohortId());
        if (userRepo.existsByUsernameAndDeletedAtIsNull(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Member member = createDefaultMember(username, req.getCohortId());
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPasswordHash(passwordEncoder.encode(req.getInitialPassword()));
        user.setMemberId(member.getId());
        user.setEnabled(req.getEnabled() == null || req.getEnabled());
        user.setProfileCompleted(false);
        user.setInitialPasswordChanged(false);
        user = userRepo.save(user);
        logService.log("auth", "CREATE", String.valueOf(user.getId()),
                "创建账号 " + username + "（" + cohortLabel(req.getCohortId()) + "）");
        return toUserResponse(user, cohortYearMap());
    }

    @Transactional
    public BatchCreateUsersResponse batchCreateUsers(BatchCreateUsersRequest req) {
        permissionChecker.requireUserManage();
        requireActiveCohort(req.getCohortId());
        List<BatchCreateUsersResponse.BatchCreateSuccessItem> created = new ArrayList<>();
        List<BatchCreateUsersResponse.BatchCreateFailedItem> failed = new ArrayList<>();
        if (req.getAccounts() == null) {
            return BatchCreateUsersResponse.builder().created(created).failed(failed).build();
        }
        for (BatchCreateAccountItem item : req.getAccounts()) {
            try {
                String username = normalizeUsername(item.getUsername());
                validateNewPassword(item.getInitialPassword(), "初始密码");
                if (userRepo.existsByUsernameAndDeletedAtIsNull(username)) {
                    throw new IllegalArgumentException("用户名已存在");
                }
                Member member = createDefaultMember(username, req.getCohortId());
                UserAccount user = new UserAccount();
                user.setUsername(username);
                user.setPasswordHash(passwordEncoder.encode(item.getInitialPassword()));
                user.setMemberId(member.getId());
                user.setEnabled(true);
                user.setProfileCompleted(false);
                user.setInitialPasswordChanged(false);
                user = userRepo.save(user);
                created.add(BatchCreateUsersResponse.BatchCreateSuccessItem.builder()
                        .username(username)
                        .memberId(member.getId())
                        .build());
                logService.log("auth", "CREATE", String.valueOf(user.getId()),
                        "批量创建账号 " + username + "（" + cohortLabel(req.getCohortId()) + "）");
            } catch (Exception e) {
                failed.add(BatchCreateUsersResponse.BatchCreateFailedItem.builder()
                        .username(item == null ? null : item.getUsername())
                        .reason(e.getMessage())
                        .build());
            }
        }
        return BatchCreateUsersResponse.builder().created(created).failed(failed).build();
    }

    @Transactional
    public void updateEnabled(Long id, Boolean enabled) {
        permissionChecker.requireUserManage();
        Long currentUserId = ActorHolder.get().getUserId();
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new PermissionDeniedException("不能禁用当前登录账号");
        }
        UserAccount user = findUser(id);
        user.setEnabled(Boolean.TRUE.equals(enabled));
        userRepo.save(user);
        logService.log("auth", "UPDATE", String.valueOf(id), (Boolean.TRUE.equals(enabled) ? "启用" : "禁用") + "账号 " + user.getUsername());
    }

    @Transactional
    public void resetPassword(Long id, String newPassword) {
        permissionChecker.requireUserManage();
        validateNewPassword(newPassword, "新密码");
        UserAccount user = findUser(id);
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setInitialPasswordChanged(false);
        userRepo.save(user);
        logService.log("auth", "UPDATE", String.valueOf(id), "重置账号 " + user.getUsername() + " 的密码");
    }

    @Transactional
    public void deleteUser(Long id) {
        permissionChecker.requireUserManage();
        Long currentUserId = ActorHolder.get().getUserId();
        if (currentUserId != null && currentUserId.equals(id)) {
            throw new PermissionDeniedException("不能删除当前登录账号");
        }
        UserAccount user = findUser(id);
        user.setDeletedAt(OffsetDateTime.now());
        userRepo.save(user);
        logService.log("auth", "DELETE", String.valueOf(id), "删除账号 " + user.getUsername());
    }

    @Transactional
    public CurrentUserResponse updateMyProfile(Long userId, UpdateMyProfileRequest req) {
        UserAccount user = findUser(userId);
        Member member = getRequiredMember(user);
        // 组织身份（届次/部门/职务）不在此接口内修改，仅维护个人资料
        member.setName(req.getName());
        member.setStudentNo(req.getStudentNo());
        member.setPhone(req.getPhone());
        member.setMajor(req.getMajor());
        memberRepo.save(member);
        user.setProfileCompleted(true);
        userRepo.save(user);
        logService.log("auth", "UPDATE", String.valueOf(userId), "修改个人资料");
        return buildCurrentUser(user);
    }

    public CurrentUserResponse getMyProfile(Long userId) {
        return buildCurrentUser(findUser(userId));
    }

    public CurrentUserResponse buildCurrentUser(UserAccount user) {
        Member member = getMember(user);
        boolean full = isFullAccess(member);
        Long cohortId = member != null ? member.getCohortId() : null;
        return CurrentUserResponse.builder()
                .userId(user.getId())
                .username(user.getUsername())
                .memberId(user.getMemberId())
                .name(member != null ? member.getName() : user.getUsername())
                .studentNo(member != null ? member.getStudentNo() : null)
                .phone(member != null ? member.getPhone() : null)
                .major(member != null ? member.getMajor() : null)
                .department(member != null ? member.getDepartment() : null)
                .position(member != null ? member.getPosition() : null)
                .cohortId(cohortId)
                .cohortYear(cohortId == null ? null : cohortYear(cohortId))
                .fullAccess(full)
                .profileCompleted(Boolean.TRUE.equals(user.getProfileCompleted()))
                .initialPasswordChanged(Boolean.TRUE.equals(user.getInitialPasswordChanged()))
                .build();
    }

    private UserAccount findUser(Long id) {
        return userRepo.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
    }

    private Member getMember(UserAccount user) {
        return user.getMemberId() == null ? null : memberRepo.findByIdAndDeletedAtIsNull(user.getMemberId()).orElse(null);
    }

    private Member getRequiredMember(UserAccount user) {
        Member member = getMember(user);
        if (member == null) {
            throw new IllegalArgumentException("当前用户未绑定成员资料");
        }
        return member;
    }

    private boolean isFullAccess(Member m) {
        return m != null && ("会长".equals(m.getPosition()) || "副会长".equals(m.getPosition()) || "秘书处".equals(m.getDepartment()));
    }

    private Member createDefaultMember(String username, Long cohortId) {
        Member m = new Member();
        m.setName(username);
        m.setStudentNo(username);
        m.setPhone("");
        m.setMajor("");
        m.setDepartment("其他");
        m.setPosition("社员");
        m.setCohortId(cohortId);
        return memberRepo.save(m);
    }

    private UserAccountResponse toUserResponse(UserAccount u, Map<Long, Integer> years) {
        Member m = getMember(u);
        Long cohortId = m != null ? m.getCohortId() : null;
        return UserAccountResponse.builder()
                .id(u.getId())
                .username(u.getUsername())
                .memberId(u.getMemberId())
                .name(m != null ? m.getName() : null)
                .studentNo(m != null ? m.getStudentNo() : null)
                .phone(m != null ? m.getPhone() : null)
                .major(m != null ? m.getMajor() : null)
                .department(m != null ? m.getDepartment() : null)
                .position(m != null ? m.getPosition() : null)
                .cohortId(cohortId)
                .cohortYear(cohortId == null ? null : years.get(cohortId))
                .enabled(Boolean.TRUE.equals(u.getEnabled()))
                .profileCompleted(Boolean.TRUE.equals(u.getProfileCompleted()))
                .initialPasswordChanged(Boolean.TRUE.equals(u.getInitialPasswordChanged()))
                .lastLoginAt(u.getLastLoginAt())
                .createdAt(u.getCreatedAt())
                .build();
    }

    private void requireActiveCohort(Long cohortId) {
        Cohort cohort = cohortRepository.findByIdAndDeletedAtIsNull(cohortId)
                .orElseThrow(() -> new IllegalArgumentException("届次不存在"));
        if (!Boolean.TRUE.equals(cohort.getEnabled())) {
            throw new IllegalArgumentException("届次已停用，不能分配给新账号");
        }
    }

    private Integer cohortYear(Long cohortId) {
        if (cohortId == null) return null;
        return cohortRepository.findByIdAndDeletedAtIsNull(cohortId).map(Cohort::getYear).orElse(null);
    }

    private String cohortLabel(Long cohortId) {
        Integer y = cohortYear(cohortId);
        return y == null ? "未分届" : y + "届";
    }

    private Map<Long, Integer> cohortYearMap() {
        return cohortRepository.findAllByDeletedAtIsNullOrderByYearDesc().stream()
                .collect(Collectors.toMap(Cohort::getId, Cohort::getYear));
    }

    private String normalizeUsername(String username) {
        if (!StringUtils.hasText(username)) {
            throw new IllegalArgumentException("用户名不能为空");
        }
        return username.trim();
    }

    private void validateNewPassword(String password, String fieldName) {
        if (!StringUtils.hasText(password) || password.trim().length() < 6) {
            throw new IllegalArgumentException(fieldName + "至少 6 位");
        }
    }
}
