package com.openatom.club.point.service;

import com.openatom.club.common.exception.BizException;
import com.openatom.club.member.entity.Member;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.dto.*;
import com.openatom.club.point.entity.PointItem;
import com.openatom.club.point.entity.PointItemCohort;
import com.openatom.club.point.repository.PointItemCohortRepository;
import com.openatom.club.point.repository.PointItemRepository;
import com.openatom.club.point.repository.PointRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
public class PointTableService {
    private final PointItemRepository pointItemRepository;
    private final PointItemCohortRepository pointItemCohortRepository;
    private final PointRecordRepository pointRecordRepository;
    private final MemberRepository memberRepository;

    /**
     * @param cohortId null=全部；-1=未分届；其他=指定届次
     */
    public PointTableResult getTable(int page, int size, String keyword, Long cohortId) {
        // 1. 获取适用于该届次的积分项目作为列（全局 + 绑定该届）
        List<PointItem> items = applicableItems(cohortId);
        List<PointTableColumnDto> columns = items.stream()
                .map(i -> new PointTableColumnDto(i.getId(), i.getItemName(), i.getPointValue()))
                .toList();

        // 2. 全部成员总分
        List<Object[]> memberTotals = pointRecordRepository.sumScoreGroupByMember();
        Map<Long, BigDecimal> totalScoreMap = new HashMap<>();
        for (Object[] row : memberTotals) {
            totalScoreMap.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);
        }

        // 3. 按届次过滤成员
        List<Member> allMembers = memberRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream().filter(m -> m.getDeletedAt() == null).toList();
        List<Member> cohortMembers = filterByCohort(allMembers, cohortId);

        List<Member> filteredMembers = cohortMembers;
        if (StringUtils.hasText(keyword)) {
            String kw = keyword.toLowerCase();
            filteredMembers = cohortMembers.stream()
                    .filter(m -> m.getName().toLowerCase().contains(kw) ||
                                 m.getStudentNo().toLowerCase().contains(kw))
                    .toList();
        }

        // 4. 按总分倒序、memberId 升序
        List<Member> sortedMembers = filteredMembers.stream()
                .sorted(Comparator
                        .comparing((Member m) -> totalScoreMap.getOrDefault(m.getId(), BigDecimal.ZERO))
                        .reversed()
                        .thenComparingLong(Member::getId))
                .toList();

        long total = sortedMembers.size();

        // 5. 分页
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, (int) total);
        List<Member> pageMembers = fromIndex < total ? sortedMembers.subList(fromIndex, toIndex) : List.of();

        // 6. 每人每项积分
        List<Object[]> itemScores = pointRecordRepository.sumScoreGroupByMemberAndItem();
        Map<Long, Map<Long, BigDecimal>> memberItemScoreMap = new HashMap<>();
        for (Object[] row : itemScores) {
            Long memberId = ((Number) row[0]).longValue();
            Long itemId = ((Number) row[1]).longValue();
            BigDecimal score = (BigDecimal) row[2];
            memberItemScoreMap.computeIfAbsent(memberId, k -> new HashMap<>()).put(itemId, score);
        }

        // 7. 构建行
        List<PointTableRowDto> rows = new ArrayList<>();
        for (int i = 0; i < pageMembers.size(); i++) {
            Member m = pageMembers.get(i);
            PointTableRowDto row = new PointTableRowDto();
            row.setRankNo(fromIndex + i + 1);
            row.setMemberId(m.getId());
            row.setName(m.getName());
            row.setStudentNo(m.getStudentNo());
            row.setPhone(m.getPhone());
            row.setMajor(m.getMajor());
            row.setDepartment(m.getDepartment());
            row.setPosition(m.getPosition());
            row.setTotalScore(totalScoreMap.getOrDefault(m.getId(), BigDecimal.ZERO));
            Map<String, BigDecimal> scores = new HashMap<>();
            Map<Long, BigDecimal> itemMap = memberItemScoreMap.getOrDefault(m.getId(), Map.of());
            for (PointItem item : items) {
                scores.put(String.valueOf(item.getId()), itemMap.getOrDefault(item.getId(), BigDecimal.ZERO));
            }
            row.setScores(scores);
            rows.add(row);
        }

        PointTableResult result = new PointTableResult();
        result.setColumns(columns);
        result.setRows(rows);
        result.setPage(page);
        result.setSize(size);
        result.setTotal(total);
        return result;
    }

    public SearchPositionResult searchPosition(String keyword, int pageSize, int matchIndex, Long cohortId) {
        if (!StringUtils.hasText(keyword)) throw BizException.of("关键词不能为空");

        List<Object[]> memberTotals = pointRecordRepository.sumScoreGroupByMember();
        Map<Long, BigDecimal> totalScoreMap = new HashMap<>();
        for (Object[] row : memberTotals) {
            totalScoreMap.put(((Number) row[0]).longValue(), (BigDecimal) row[1]);
        }
        List<Member> allMembers = memberRepository.findAll(Sort.by(Sort.Direction.ASC, "id"))
                .stream().filter(m -> m.getDeletedAt() == null).toList();
        List<Member> cohortMembers = filterByCohort(allMembers, cohortId);

        List<Member> ranked = cohortMembers.stream()
                .sorted(Comparator
                        .comparing((Member m) -> totalScoreMap.getOrDefault(m.getId(), BigDecimal.ZERO))
                        .reversed()
                        .thenComparingLong(Member::getId))
                .toList();

        Map<Long, Integer> rankMap = new HashMap<>();
        for (int i = 0; i < ranked.size(); i++) {
            rankMap.put(ranked.get(i).getId(), i + 1);
        }

        String kw = keyword.toLowerCase();
        List<Member> matched = cohortMembers.stream()
                .filter(m -> m.getName().toLowerCase().contains(kw))
                .sorted(Comparator.comparingInt(m -> rankMap.getOrDefault(m.getId(), Integer.MAX_VALUE)))
                .toList();

        int matchCount = matched.size();
        if (matchCount == 0) throw BizException.of("未找到匹配的成员");
        if (matchIndex >= matchCount) throw BizException.of("匹配索引超出范围");

        Member target = matched.get(matchIndex);
        int rankNo = rankMap.getOrDefault(target.getId(), 0);
        int pageNo = (int) Math.ceil((double) rankNo / pageSize);
        int rowNoInPage = rankNo - (pageNo - 1) * pageSize;

        SearchPositionResult result = new SearchPositionResult();
        result.setKeyword(keyword);
        result.setMatchCount(matchCount);
        result.setMatchIndex(matchIndex);
        result.setMemberId(target.getId());
        result.setName(target.getName());
        result.setStudentNo(target.getStudentNo());
        result.setRankNo(rankNo);
        result.setPageNo(pageNo);
        result.setRowNoInPage(rowNoInPage);
        result.setTotalScore(totalScoreMap.getOrDefault(target.getId(), BigDecimal.ZERO));
        return result;
    }

    private List<PointItem> applicableItems(Long cohortId) {
        return pointItemRepository.findAllByDeletedAtIsNullOrderBySortOrderAscIdAsc().stream()
                .filter(i -> isApplicable(i, cohortId))
                .toList();
    }

    private boolean isApplicable(PointItem item, Long cohortId) {
        List<PointItemCohort> links = pointItemCohortRepository.findByPointItemId(item.getId());
        if (links.isEmpty()) return true; // 全局适用
        return cohortId != null && links.stream().anyMatch(l -> l.getCohortId().equals(cohortId));
    }

    private List<Member> filterByCohort(List<Member> members, Long cohortId) {
        if (cohortId == null) return members;
        if (cohortId == -1) return members.stream().filter(m -> m.getCohortId() == null).toList();
        return members.stream().filter(m -> cohortId.equals(m.getCohortId())).toList();
    }
}
