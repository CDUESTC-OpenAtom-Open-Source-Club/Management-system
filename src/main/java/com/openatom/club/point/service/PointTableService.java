package com.openatom.club.point.service;

import com.openatom.club.common.exception.BizException;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.common.security.PermissionChecker;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.PointItemTypes;
import com.openatom.club.point.dto.PointDetailResponse;
import com.openatom.club.point.dto.PointTableResult;
import com.openatom.club.point.dto.PointTableRowDto;
import com.openatom.club.point.dto.SearchPositionResult;
import com.openatom.club.point.entity.PointItem;
import com.openatom.club.point.entity.PointRecord;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PointTableService {
    private final PointItemRepository pointItemRepository;
    private final PointRecordRepository pointRecordRepository;
    private final MemberRepository memberRepository;
    private final PermissionChecker permissionChecker;

    /**
     * 积分总表：按 PointItem.type 聚合为固定六大分类，不再动态生成项目列。
     *
     * @param cohortId null=全部；-1=未分届；其他=指定届次
     */
    public PointTableResult getTable(int page, int size, String keyword, Long cohortId) {
        List<PointTableRowDto> ranked = buildRankedRows(cohortId);

        List<PointTableRowDto> filtered = ranked;
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.toLowerCase();
            filtered = ranked.stream()
                    .filter(r -> contains(r.getName(), kw) || contains(r.getStudentNo(), kw))
                    .toList();
        }

        // 关键词过滤后按搜索结果内名次连续编号（保持原有语义）
        for (int i = 0; i < filtered.size(); i++) {
            filtered.get(i).setRankNo(i + 1);
        }

        long total = filtered.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, (int) total);
        List<PointTableRowDto> pageRows = fromIndex < total
                ? new ArrayList<>(filtered.subList(fromIndex, toIndex))
                : List.of();

        PointTableResult result = new PointTableResult();
        result.setRows(pageRows);
        result.setPage(page);
        result.setSize(size);
        result.setTotal(total);
        return result;
    }

    public SearchPositionResult searchPosition(String keyword, int pageSize, int matchIndex, Long cohortId) {
        if (!StringUtils.hasText(keyword)) throw BizException.of("关键词不能为空");

        List<PointTableRowDto> ranked = buildRankedRows(cohortId);
        String kw = keyword.toLowerCase();
        List<PointTableRowDto> matched = ranked.stream()
                .filter(r -> contains(r.getName(), kw))
                .toList();

        int matchCount = matched.size();
        if (matchCount == 0) throw BizException.of("未找到匹配的成员");
        if (matchIndex >= matchCount) throw BizException.of("匹配索引超出范围");

        PointTableRowDto target = matched.get(matchIndex);
        int rankNo = target.getRankNo() != null ? target.getRankNo() : 0;
        int pageNo = (int) Math.ceil((double) rankNo / pageSize);
        int rowNoInPage = rankNo - (pageNo - 1) * pageSize;

        SearchPositionResult result = new SearchPositionResult();
        result.setKeyword(keyword);
        result.setMatchCount(matchCount);
        result.setMatchIndex(matchIndex);
        result.setMemberId(target.getMemberId());
        result.setName(target.getName());
        result.setStudentNo(target.getStudentNo());
        result.setRankNo(rankNo);
        result.setPageNo(pageNo);
        result.setRowNoInPage(rowNoInPage);
        result.setTotalScore(target.getTotalScore());
        return result;
    }

    /**
     * 管理员查看某成员积分明细（fullAccess only，分页，按发生时间倒序）。
     */
    public PageResult<PointDetailResponse> listMemberDetails(Long memberId, int page, int size) {
        permissionChecker.requireFullAccess();
        memberRepository.findByIdAndDeletedAtIsNull(memberId)
                .orElseThrow(() -> BizException.of("成员不存在"));

        Page<PointRecord> recordPage = pointRecordRepository.findByMemberIdAndDeletedAtIsNull(memberId,
                PageRequest.of(page - 1, size,
                        Sort.by(Sort.Order.desc("occurredAt").nullsLast(),
                                Sort.Order.desc("createdAt"),
                                Sort.Order.desc("id"))));

        List<Long> itemIds = recordPage.getContent().stream()
                .map(PointRecord::getPointItemId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, PointItem> itemMap = itemIds.isEmpty() ? Map.of()
                : pointItemRepository.findAllByIdInAndDeletedAtIsNull(itemIds).stream()
                        .collect(Collectors.toMap(PointItem::getId, Function.identity()));

        List<PointDetailResponse> list = recordPage.getContent().stream()
                .map(r -> toDetail(r, itemMap))
                .toList();
        return new PageResult<>(list, recordPage.getTotalElements(), page, size);
    }

    // ==================== 内部方法 ====================

    /** 构建某届（或全部/未分届）成员的排名行，rankNo 为全局名次 1..n */
    private List<PointTableRowDto> buildRankedRows(Long cohortId) {
        List<Member> members = membersByCohort(cohortId);
        List<Long> memberIds = members.stream().map(Member::getId).toList();

        Map<Long, Map<String, BigDecimal>> memberTypeScores = aggregateByType(memberIds);
        Map<Long, BigDecimal> totalMap = new HashMap<>();
        for (Long memberId : memberIds) {
            totalMap.put(memberId, sumSix(memberTypeScores.get(memberId)));
        }

        List<Member> sorted = members.stream()
                .sorted(Comparator
                        .comparing((Member m) -> totalMap.getOrDefault(m.getId(), BigDecimal.ZERO))
                        .reversed()
                        .thenComparing(Member::getName, Comparator.nullsLast(String::compareTo))
                        .thenComparingLong(Member::getId))
                .toList();

        List<PointTableRowDto> rows = new ArrayList<>(sorted.size());
        for (int i = 0; i < sorted.size(); i++) {
            Member m = sorted.get(i);
            Map<String, BigDecimal> s = memberTypeScores.get(m.getId());
            PointTableRowDto row = new PointTableRowDto();
            row.setRankNo(i + 1);
            row.setMemberId(m.getId());
            row.setName(m.getName());
            row.setStudentNo(m.getStudentNo());
            row.setActivityScore(get(s, PointItemTypes.ACTIVITY));
            row.setCompetitionScore(get(s, PointItemTypes.COMPETITION));
            row.setOpenSourceLearningScore(get(s, PointItemTypes.OPEN_SOURCE_LEARNING));
            row.setCommunityContributionScore(get(s, PointItemTypes.COMMUNITY_CONTRIBUTION));
            row.setSpeechHostingScore(get(s, PointItemTypes.SPEECH_HOST));
            row.setOtherScore(get(s, PointItemTypes.OTHER));
            row.setTotalScore(totalMap.getOrDefault(m.getId(), BigDecimal.ZERO));
            rows.add(row);
        }
        return rows;
    }

    /** 一次查询 memberIds 的全部积分记录 + 全部积分项目，内存按 type 聚合（避免 N+1） */
    private Map<Long, Map<String, BigDecimal>> aggregateByType(List<Long> memberIds) {
        Map<Long, Map<String, BigDecimal>> result = new HashMap<>();
        if (memberIds.isEmpty()) return result;

        Map<Long, PointItem> itemById = pointItemRepository.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc()
                .stream().collect(Collectors.toMap(PointItem::getId, Function.identity()));

        List<PointRecord> records = pointRecordRepository.findAllByMemberIdInAndDeletedAtIsNull(memberIds);
        for (PointRecord r : records) {
            String type = normalizeType(r.getPointItemId(), itemById);
            result.computeIfAbsent(r.getMemberId(), k -> zeroSix())
                    .merge(type, r.getScore(), BigDecimal::add);
        }
        return result;
    }

    /** 分类依据是 PointItem.type；pointItemId 为空/已删除/非法类型一律归「其他」 */
    private String normalizeType(Long pointItemId, Map<Long, PointItem> itemById) {
        if (pointItemId != null) {
            PointItem item = itemById.get(pointItemId);
            if (item != null && PointItemTypes.isValid(item.getItemType())) {
                return item.getItemType();
            }
        }
        return PointItemTypes.OTHER;
    }

    private Map<String, BigDecimal> zeroSix() {
        Map<String, BigDecimal> m = new HashMap<>();
        for (String t : PointItemTypes.ALL) m.put(t, BigDecimal.ZERO);
        return m;
    }

    private BigDecimal sumSix(Map<String, BigDecimal> s) {
        if (s == null) return BigDecimal.ZERO;
        BigDecimal total = BigDecimal.ZERO;
        for (String t : PointItemTypes.ALL) total = total.add(s.getOrDefault(t, BigDecimal.ZERO));
        return total;
    }

    private BigDecimal get(Map<String, BigDecimal> s, String type) {
        return s == null ? BigDecimal.ZERO : s.getOrDefault(type, BigDecimal.ZERO);
    }

    private List<Member> membersByCohort(Long cohortId) {
        if (cohortId == null) return memberRepository.findAllByDeletedAtIsNullOrderByIdAsc();
        if (cohortId == -1) return memberRepository.findAllByCohortIdIsNullAndDeletedAtIsNullOrderByIdAsc();
        return memberRepository.findAllByCohortIdAndDeletedAtIsNullOrderByIdAsc(cohortId);
    }

    private PointDetailResponse toDetail(PointRecord r, Map<Long, PointItem> itemMap) {
        PointDetailResponse d = new PointDetailResponse();
        d.setId(r.getId());
        d.setMemberId(r.getMemberId());
        d.setPointItemId(r.getPointItemId());
        if (r.getPointItemId() != null) {
            PointItem item = itemMap.get(r.getPointItemId());
            if (item != null) {
                d.setPointItemName(item.getItemName());
                d.setPointItemType(item.getItemType());
            }
        }
        d.setScore(r.getScore());
        d.setSourceType(r.getSourceType());
        d.setSourceLabel(sourceLabel(r.getSourceType()));
        d.setReason(r.getReason());
        d.setOperatorName(r.getOperatorName());
        d.setOccurredAt(r.getOccurredAt());
        d.setCreatedAt(r.getCreatedAt());
        return d;
    }

    private String sourceLabel(String sourceType) {
        if (sourceType == null) return null;
        return switch (sourceType) {
            case "APPLICATION" -> "活动登记";
            case "MANUAL" -> "手工调整";
            case "HOMEWORK" -> "作业";
            default -> sourceType;
        };
    }

    private boolean contains(String value, String kw) {
        return value != null && value.toLowerCase().contains(kw);
    }
}
