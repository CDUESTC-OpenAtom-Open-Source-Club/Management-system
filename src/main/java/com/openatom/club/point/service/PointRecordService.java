package com.openatom.club.point.service;

import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.log.service.OperationLogService;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.dto.PointRecordRequest;
import com.openatom.club.point.dto.PointRecordResponse;
import com.openatom.club.point.entity.PointRecord;
import com.openatom.club.point.repository.PointRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PointRecordService {
    private final PointRecordRepository pointRecordRepository;
    private final MemberRepository memberRepository;
    private final PermissionChecker permissionChecker;
    private final OperationLogService logService;

    public List<PointRecordResponse> listByMember(Long memberId) {
        memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BizException.of("成员不存在"));
        return pointRecordRepository.findAllByMemberIdAndDeletedAtIsNull(memberId)
                .stream().map(PointRecordResponse::from).toList();
    }

    @Transactional
    public PointRecordResponse create(Long memberId, PointRecordRequest req) {
        permissionChecker.requireManage();
        memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BizException.of("成员不存在"));
        PointRecord record = new PointRecord();
        record.setMemberId(memberId);
        record.setPointItemId(req.getPointItemId());
        record.setScore(req.getScore());
        record.setReason(req.getReason());
        record.setSourceType("MANUAL");
        record.setOperatorName(ActorHolder.get().getName());
        record.setOccurredAt(req.getOccurredAt() != null ? req.getOccurredAt() : OffsetDateTime.now());
        PointRecord saved = pointRecordRepository.save(record);
        logService.log("point_record", "CREATE", String.valueOf(saved.getId()),
                "手动新增积分记录, 成员ID: " + memberId + ", 分値: " + req.getScore());
        return PointRecordResponse.from(saved);
    }

    @Transactional
    public PointRecordResponse update(Long id, PointRecordRequest req) {
        permissionChecker.requireManage();
        PointRecord record = pointRecordRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("积分记录不存在"));
        record.setScore(req.getScore());
        record.setReason(req.getReason());
        if (req.getOccurredAt() != null) record.setOccurredAt(req.getOccurredAt());
        record.setOperatorName(ActorHolder.get().getName());
        PointRecord saved = pointRecordRepository.save(record);
        logService.log("point_record", "UPDATE", String.valueOf(id),
                "修改积分记录 #" + id);
        return PointRecordResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        permissionChecker.requireManage();
        PointRecord record = pointRecordRepository.findByIdAndDeletedAtIsNull(id)
                .orElseThrow(() -> BizException.of("积分记录不存在"));
        record.setDeletedAt(OffsetDateTime.now());
        pointRecordRepository.save(record);
        logService.log("point_record", "DELETE", String.valueOf(id),
                "删除积分记录 #" + id);
    }
}
