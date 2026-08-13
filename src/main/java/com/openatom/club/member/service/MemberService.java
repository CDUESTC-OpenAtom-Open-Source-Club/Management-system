package com.openatom.club.member.service;

import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.dto.MemberRequest;
import com.openatom.club.member.dto.MemberResponse;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberService {
    private final MemberRepository memberRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    public PageResult<MemberResponse> list(String keyword, int page, int size) {
        Page<Member> memberPage = memberRepository.searchMembers(
                StringUtils.hasText(keyword) ? keyword : null,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.ASC, "id"))
        );
        List<MemberResponse> list = memberPage.getContent().stream()
                .map(MemberResponse::from).toList();
        return new PageResult<>(list, memberPage.getTotalElements(), page, size);
    }

    @Transactional
    public MemberResponse create(MemberRequest req) {
        permissionChecker.requireManage();
        if (memberRepository.existsByStudentNoAndDeletedAtIsNull(req.getStudentNo())) {
            throw BizException.of("学号已存在: " + req.getStudentNo());
        }
        Member member = new Member();
        member.setName(req.getName());
        member.setStudentNo(req.getStudentNo());
        member.setPhone(req.getPhone());
        member.setMajor(req.getMajor());
        member.setDepartment(req.getDepartment());
        member.setPosition(StringUtils.hasText(req.getPosition()) ? req.getPosition() : "社员");
        Member saved = memberRepository.save(member);
        logService.log("member", "CREATE", String.valueOf(saved.getId()),
                "新增成员: " + saved.getName());
        return MemberResponse.from(saved);
    }

    @Transactional
    public MemberResponse update(Long id, MemberRequest req) {
        permissionChecker.requireManage();
        Member member = memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("成员不存在"));
        if (!member.getStudentNo().equals(req.getStudentNo()) &&
                memberRepository.existsByStudentNoAndDeletedAtIsNullAndIdNot(req.getStudentNo(), id)) {
            throw BizException.of("学号已被其他成员使用: " + req.getStudentNo());
        }
        // 基础字段：秘书处及以上可修改
        member.setName(req.getName());
        member.setStudentNo(req.getStudentNo());
        member.setPhone(req.getPhone());
        member.setMajor(req.getMajor());
        // department 和 position 只有会长、副会长可以修改
        if (req.getDepartment() != null) {
            permissionChecker.requireAdmin();
            member.setDepartment(req.getDepartment());
        }
        if (StringUtils.hasText(req.getPosition())) {
            permissionChecker.requireAdmin();
            member.setPosition(req.getPosition());
        }
        Member saved = memberRepository.save(member);
        logService.log("member", "UPDATE", String.valueOf(id),
                "修改成员: " + saved.getName());
        return MemberResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.requireManage();
        Member member = memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("成员不存在"));
        member.setDeletedAt(OffsetDateTime.now());
        memberRepository.save(member);
        logService.log("member", "DELETE", String.valueOf(id),
                "删除成员: " + member.getName());
    }

    public MemberResponse getById(Long id) {
        Member member = memberRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("成员不存在"));
        return MemberResponse.from(member);
    }
}
