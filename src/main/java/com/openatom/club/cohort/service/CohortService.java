package com.openatom.club.cohort.service;

import com.openatom.club.cohort.dto.CohortRequest;
import com.openatom.club.cohort.dto.CohortResponse;
import com.openatom.club.cohort.entity.Cohort;
import com.openatom.club.cohort.repository.CohortRepository;
import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.homework.repository.HomeworkAssignmentRepository;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CohortService {
    private final CohortRepository cohortRepository;
    private final MemberRepository memberRepository;
    private final HomeworkAssignmentRepository homeworkAssignmentRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    public List<CohortResponse> list() {
        return cohortRepository.findAllByDeletedAtIsNullOrderByYearDesc().stream()
                .map(CohortResponse::from).toList();
    }

    @Transactional
    public CohortResponse create(CohortRequest req) {
        permissionChecker.requireFullAccess();
        if (cohortRepository.existsByYearAndDeletedAtIsNull(req.getYear())) {
            throw BizException.of("该届次已存在: " + req.getYear());
        }
        Cohort cohort = new Cohort();
        cohort.setYear(req.getYear());
        cohort.setEnabled(req.getEnabled() == null || req.getEnabled());
        Cohort saved = cohortRepository.save(cohort);
        logService.log("cohort", "CREATE", String.valueOf(saved.getId()),
                "新增届次: " + saved.getYear() + "届");
        return CohortResponse.from(saved);
    }

    @Transactional
    public CohortResponse update(Long id, CohortRequest req) {
        permissionChecker.requireFullAccess();
        Cohort cohort = cohortRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("届次不存在"));
        if (req.getYear() != null && !req.getYear().equals(cohort.getYear())) {
            if (cohortRepository.existsByYearAndDeletedAtIsNullAndIdNot(req.getYear(), id)) {
                throw BizException.of("该届次已存在: " + req.getYear());
            }
            cohort.setYear(req.getYear());
        }
        if (req.getEnabled() != null) {
            cohort.setEnabled(req.getEnabled());
        }
        Cohort saved = cohortRepository.save(cohort);
        logService.log("cohort", "UPDATE", String.valueOf(id),
                "修改届次: " + saved.getYear() + "届 (enabled=" + saved.getEnabled() + ")");
        return CohortResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.requireFullAccess();
        Cohort cohort = cohortRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("届次不存在"));
        if (memberRepository.countByCohortId(id) > 0 ||
                homeworkAssignmentRepository.countByCohortId(id) > 0) {
            throw BizException.of("该届次已被成员或作业引用，不能删除；可改为停用");
        }
        cohort.setDeletedAt(OffsetDateTime.now());
        cohortRepository.save(cohort);
        logService.log("cohort", "DELETE", String.valueOf(id),
                "删除届次: " + cohort.getYear() + "届");
    }
}
