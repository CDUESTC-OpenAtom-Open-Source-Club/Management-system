package com.openatom.club.auth.config;

import com.openatom.club.auth.entity.UserAccount;
import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DefaultAdminInitializer implements ApplicationRunner {

    private final UserAccountRepository userRepo;
    private final MemberRepository memberRepo;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!memberRepo.existsByStudentNoAndDeletedAtIsNull("2024402011133")) {
            Member member = new Member();
            member.setName("林涛");
            member.setStudentNo("2024402011133");
            member.setPhone("18289868707");
            member.setMajor("软件工程");
            member.setDepartment("其他");
            member.setPosition("副会长");
            memberRepo.save(member);
        }

        if (!userRepo.existsByUsernameAndDeletedAtIsNull("lintao")) {
            Member member = memberRepo.searchMembers("2024402011133", org.springframework.data.domain.PageRequest.of(0, 1))
                    .getContent().stream().findFirst().orElse(null);
            UserAccount lintao = new UserAccount();
            lintao.setUsername("lintao");
            lintao.setPasswordHash(passwordEncoder.encode("123456"));
            lintao.setMemberId(member != null ? member.getId() : null);
            lintao.setEnabled(true);
            lintao.setProfileCompleted(true);
            lintao.setInitialPasswordChanged(true);
            userRepo.save(lintao);
        } else {
            // 每次启动时重置 lintao 密码为 123456
            userRepo.findByUsernameAndDeletedAtIsNull("lintao").ifPresent(u -> {
                u.setPasswordHash(passwordEncoder.encode("123456"));
                userRepo.save(u);
            });
        }
    }
}
