package com.openatom.club.point.service;

import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.dto.PointApplicationResponse;
import com.openatom.club.point.dto.PointApplicationSubmitRequest;
import com.openatom.club.point.dto.RejectRequest;
import com.openatom.club.point.entity.PointApplication;
import com.openatom.club.point.entity.PointItem;
import com.openatom.club.point.entity.PointItemCohort;
import com.openatom.club.point.entity.PointRecord;
import com.openatom.club.point.repository.PointApplicationRepository;
import com.openatom.club.point.repository.PointItemCohortRepository;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointApplicationService {
    private final PointApplicationRepository applicationRepository;
    private final PointItemRepository pointItemRepository;
    private final PointItemCohortRepository pointItemCohortRepository;
    private final PointRecordRepository pointRecordRepository;
    private final MemberRepository memberRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    @Transactional
    public List<PointApplicationResponse> submit(PointApplicationSubmitRequest req) {
        Long memberId = ActorHolder.get().getMemberId();
        if (memberId == null) {
            throw BizException.of("当前账号未绑定成员资料");
        }
        Member member = memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BizException.of("成员不存在"));
        List<PointApplication> results = new ArrayList<>();
        for (Long itemId : req.getPointItemIds()) {
            PointItem item = pointItemRepository.findByIdAndDeletedAtIsNull(itemId)
                    .orElseThrow(() -> BizException.of("积分项目不存在: " + itemId));
            if (!item.getEnabled()) throw BizException.of("积分项目已禁用: " + item.getItemName());
            if (!item.getAllowMemberApply()) throw BizException.of("该项目不允许成员自行登记: " + item.getItemName());
            if (!isApplicable(item, member.getCohortId())) throw BizException.of("该项目不适用于你的届次: " + item.getItemName());
            List<PointApplication> existing = applicationRepository.findActiveByMemberAndItem(memberId, itemId);
            if (!existing.isEmpty()) throw BizException.of("已提交或已通过: " + item.getItemName());
            PointApplication app = new PointApplication();
            app.setMemberId(memberId);
            app.setPointItemId(itemId);
            app.setStatus("PENDING");
            results.add(applicationRepository.save(app));
        }
        return results.stream().map(PointApplicationResponse::from).toList();
    }

    public List<PointApplicationResponse> myApplications() {
        Long memberId = ActorHolder.get().getMemberId();
        if (memberId == null) return List.of();
        return applicationRepository.findAllByMemberIdAndDeletedAtIsNull(memberId)
                .stream().map(app -> {
                    PointApplicationResponse dto = PointApplicationResponse.from(app);
                    pointItemRepository.findByIdAndDeletedAtIsNull(app.getPointItemId())
                            .ifPresent(item -> dto.setItemName(item.getItemName()));
                    memberRepository.findByIdAndDeletedAtIsNull(app.getMemberId())
                            .ifPresent(m -> { dto.setMemberName(m.getName()); dto.setStudentNo(m.getStudentNo()); });
                    return dto;
                }).toList();
    }

    public PageResult<PointApplicationResponse> listAll(String status, Long cohortId, String keyword, int page, int size) {
        permissionChecker.requireManage();
        Page<PointApplication> appPage = applicationRepository.searchApplications(
                StringUtils.hasText(status) ? status : null,
                cohortId,
                StringUtils.hasText(keyword) ? keyword : null,
                PageRequest.of(page - 1, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        );
        List<PointApplicationResponse> list = appPage.getContent().stream().map(app -> {
            PointApplicationResponse dto = PointApplicationResponse.from(app);
            pointItemRepository.findByIdAndDeletedAtIsNull(app.getPointItemId())
                    .ifPresent(item -> dto.setItemName(item.getItemName()));
            memberRepository.findByIdAndDeletedAtIsNull(app.getMemberId())
                    .ifPresent(m -> { dto.setMemberName(m.getName()); dto.setStudentNo(m.getStudentNo()); });
            return dto;
        }).toList();
        return new PageResult<>(list, appPage.getTotalElements(), page, size);
    }

    @Transactional
    public void approve(Long id) {
        permissionChecker.requireManage();
        PointApplication app = applicationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("登记不存在"));
        if (!"PENDING".equals(app.getStatus())) {
            throw BizException.of("只有待审核状态可以审核通过");
        }
        String actor = ActorHolder.get().getName();
        app.setStatus("APPROVED");
        app.setReviewedAt(OffsetDateTime.now());
        app.setReviewedBy(actor);
        applicationRepository.save(app);

        PointItem item = pointItemRepository.findByIdAndDeletedAtIsNull(app.getPointItemId())
                .orElseThrow(() -> BizException.of("积分项目不存在"));
        PointRecord record = new PointRecord();
        record.setMemberId(app.getMemberId());
        record.setPointItemId(app.getPointItemId());
        record.setApplicationId(app.getId());
        record.setScore(item.getPointValue());
        record.setReason("成员登记：" + item.getItemName());
        record.setSourceType("APPLICATION");
        record.setOperatorName(actor);
        pointRecordRepository.save(record);

        logService.log("point_application", "APPROVE", String.valueOf(id),
                "审核通过登记 #" + id + "，成员ID: " + app.getMemberId());
    }

    @Transactional
    public void reject(Long id, RejectRequest req) {
        permissionChecker.requireManage();
        PointApplication app = applicationRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("登记不存在"));
        if (!"PENDING".equals(app.getStatus())) {
            throw BizException.of("只有待审核状态可以驳回");
        }
        String actor = ActorHolder.get().getName();
        app.setStatus("REJECTED");
        app.setReviewedAt(OffsetDateTime.now());
        app.setReviewedBy(actor);
        app.setReviewComment(req.getReviewComment());
        applicationRepository.save(app);
        logService.log("point_application", "REJECT", String.valueOf(id),
                "驳回登记 #" + id + "，原因: " + req.getReviewComment());
    }

    private boolean isApplicable(PointItem item, Long cohortId) {
        List<PointItemCohort> links = pointItemCohortRepository.findByPointItemId(item.getId());
        if (links.isEmpty()) return true; // 全局适用
        return cohortId != null && links.stream().anyMatch(l -> l.getCohortId().equals(cohortId));
    }
}
