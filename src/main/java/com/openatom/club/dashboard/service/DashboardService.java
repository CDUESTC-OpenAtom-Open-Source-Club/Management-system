package com.openatom.club.dashboard.service;

import com.openatom.club.archive.repository.ArchiveLinkRepository;
import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.dashboard.dto.DashboardStatsResponse;
import com.openatom.club.finance.repository.FinancePeriodRepository;
import com.openatom.club.log.entity.OperationLog;
import com.openatom.club.log.repository.OperationLogRepository;
import com.openatom.club.meeting.repository.MeetingMinutesRepository;
import com.openatom.club.member.repository.MemberRepository;
import com.openatom.club.point.repository.PointApplicationRepository;
import com.openatom.club.point.repository.PointItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final MemberRepository memberRepository;
    private final PointItemRepository pointItemRepository;
    private final PointApplicationRepository pointApplicationRepository;
    private final ArchiveLinkRepository archiveLinkRepository;
    private final MeetingMinutesRepository meetingMinutesRepository;
    private final FinancePeriodRepository financePeriodRepository;
    private final OperationLogRepository operationLogRepository;
    private final UserAccountRepository userAccountRepository;

    public DashboardStatsResponse getStats(Long cohortId) {
        // 基础统计
        long totalMembers = countMembersByCohort(cohortId);
        long totalPointItems = pointItemRepository.countByDeletedAtIsNull();
        long pendingApplications = pointApplicationRepository.countByStatusAndCohortIdAndDeletedAtIsNull("PENDING", cohortId);
        long totalArchiveLinks = archiveLinkRepository.countByDeletedAtIsNull();
        long totalMeetingMinutes = meetingMinutesRepository.countByDeletedAtIsNull();
        long totalFinancePeriods = financePeriodRepository.count();

        // 部门分布
        List<Object[]> deptRows = memberRepository.countGroupByDepartmentByCohort(cohortId);
        List<DashboardStatsResponse.DepartmentStat> departmentDistribution = new ArrayList<>();
        for (Object[] row : deptRows) {
            String label = (String) row[0];
            if (label == null || label.isEmpty()) label = "未填写";
            long value = ((Number) row[1]).longValue();
            departmentDistribution.add(DashboardStatsResponse.DepartmentStat.builder()
                    .label(label).value(value).build());
        }

        // 近 7 周积分登记趋势
        List<DashboardStatsResponse.WeeklyTrend> weeklyTrend = buildWeeklyTrend(cohortId);

        // 待办事项
        long incompleteProfileCount = userAccountRepository.countByProfileCompletedAndDeletedAtIsNull(false);
        List<DashboardStatsResponse.PendingTask> pendingTasks = List.of(
                DashboardStatsResponse.PendingTask.builder()
                        .title("待审核积分登记")
                        .desc("处理成员提交的积分申请")
                        .count(pendingApplications)
                        .to("/point-applications")
                        .build(),
                DashboardStatsResponse.PendingTask.builder()
                        .title("待完善资料成员")
                        .desc("引导新成员完善个人资料")
                        .count(incompleteProfileCount)
                        .to("/members")
                        .build()
        );

        // 最新动态（最近 5 条操作日志）
        List<OperationLog> recentLogs = operationLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5));
        List<DashboardStatsResponse.RecentActivity> recentActivities = recentLogs.stream()
                .map(log -> DashboardStatsResponse.RecentActivity.builder()
                        .title(log.getModuleName() != null ? log.getModuleName() : "系统操作")
                        .desc(log.getDescription() != null ? log.getDescription() : "")
                        .time(formatRelativeTime(log.getCreatedAt()))
                        .build())
                .toList();

        return DashboardStatsResponse.builder()
                .totalMembers(totalMembers)
                .totalPointItems(totalPointItems)
                .pendingApplications(pendingApplications)
                .totalArchiveLinks(totalArchiveLinks)
                .totalMeetingMinutes(totalMeetingMinutes)
                .totalFinancePeriods(totalFinancePeriods)
                .departmentDistribution(departmentDistribution)
                .weeklyTrend(weeklyTrend)
                .pendingTasks(pendingTasks)
                .recentActivities(recentActivities)
                .build();
    }

    private List<DashboardStatsResponse.WeeklyTrend> buildWeeklyTrend(Long cohortId) {
        List<DashboardStatsResponse.WeeklyTrend> trend = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate weekStart = today.minusWeeks(i).with(DayOfWeek.MONDAY);
            LocalDate weekEnd = weekStart.plusDays(6);
            OffsetDateTime start = weekStart.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
            OffsetDateTime end = weekEnd.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
            long count = pointApplicationRepository.countByCreatedAtBetweenAndCohortIdAndDeletedAtIsNull(start, end, cohortId);
            trend.add(DashboardStatsResponse.WeeklyTrend.builder()
                    .week(7 - i)
                    .count(count)
                    .build());
        }
        return trend;
    }

    private long countMembersByCohort(Long cohortId) {
        if (cohortId == null) return memberRepository.countByDeletedAtIsNull();
        if (cohortId == -1) return memberRepository.countByCohortId(null);
        return memberRepository.countByCohortId(cohortId);
    }

    private String formatRelativeTime(OffsetDateTime time) {
        if (time == null) return "";
        long minutes = ChronoUnit.MINUTES.between(time.toInstant(), OffsetDateTime.now().toInstant());
        if (minutes < 1) return "刚刚";
        if (minutes < 60) return minutes + " 分钟前";
        long hours = minutes / 60;
        if (hours < 24) return hours + " 小时前";
        long days = hours / 24;
        if (days < 7) return days + " 天前";
        return (days / 7) + " 周前";
    }
}
