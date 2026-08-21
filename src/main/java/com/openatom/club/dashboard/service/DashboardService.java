package com.openatom.club.dashboard.service;

import com.openatom.club.auth.repository.UserAccountRepository;
import com.openatom.club.common.security.ActorContext;
import com.openatom.club.common.security.ActorHolder;
import com.openatom.club.dashboard.dto.DashboardStatsResponse;
import com.openatom.club.finance.repository.FinancePeriodRepository;
import com.openatom.club.homework.repository.HomeworkSubmissionRepository;
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
    private final MeetingMinutesRepository meetingMinutesRepository;
    private final FinancePeriodRepository financePeriodRepository;
    private final OperationLogRepository operationLogRepository;
    private final UserAccountRepository userAccountRepository;
    private final HomeworkSubmissionRepository homeworkSubmissionRepository;

    public DashboardStatsResponse getStats(Long cohortId) {
        ActorContext actor = ActorHolder.get();
        boolean fullAccess = actor.isFullAccess();
        boolean minister = "部长".equals(actor.getPosition());

        // 基础统计
        long totalMembers = countMembersByCohort(cohortId);
        long totalPointItems = pointItemRepository.countByDeletedAtIsNull();
        long pendingApplications = fullAccess
                ? pointApplicationRepository.countByStatusAndCohortIdAndDeletedAtIsNull("PENDING", cohortId)
                : 0;
        long totalMeetingMinutes = meetingMinutesRepository.countByDeletedAtIsNull();
        long totalFinancePeriods = financePeriodRepository.count();

        // 待批改作业（fullAccess 全部；部长仅本部门）
        long pendingHomeworkReviews = 0;
        if (fullAccess) {
            pendingHomeworkReviews = homeworkSubmissionRepository.countPendingReviews(null, cohortId);
        } else if (minister) {
            pendingHomeworkReviews = homeworkSubmissionRepository.countPendingReviews(actor.getDepartment(), cohortId);
        }

        // 本月财务台账是否待建立（仅财务权限可见其含义，非 fullAccess 一律 0）
        long currentMonthFinancePending = fullAccess ? currentMonthFinancePending() : 0;

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

        // 待办事项（按角色个性化：只展示当前用户真正能处理的）
        List<DashboardStatsResponse.PendingTask> pendingTasks = buildPendingTasks(
                fullAccess, minister, pendingApplications, pendingHomeworkReviews, currentMonthFinancePending);

        // 最新动态（最近 5 条操作日志，产品化文案）
        List<OperationLog> recentLogs = operationLogRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 5));
        List<DashboardStatsResponse.RecentActivity> recentActivities = recentLogs.stream()
                .map(this::toRecentActivity)
                .toList();

        return DashboardStatsResponse.builder()
                .totalMembers(totalMembers)
                .totalPointItems(totalPointItems)
                .pendingApplications(pendingApplications)
                .totalMeetingMinutes(totalMeetingMinutes)
                .totalFinancePeriods(totalFinancePeriods)
                .pendingHomeworkReviews(pendingHomeworkReviews)
                .currentMonthFinancePending(currentMonthFinancePending)
                .departmentDistribution(departmentDistribution)
                .weeklyTrend(weeklyTrend)
                .pendingTasks(pendingTasks)
                .recentActivities(recentActivities)
                .build();
    }

    private List<DashboardStatsResponse.PendingTask> buildPendingTasks(
            boolean fullAccess, boolean minister,
            long pendingApplications, long pendingHomeworkReviews, long currentMonthFinancePending) {
        List<DashboardStatsResponse.PendingTask> tasks = new ArrayList<>();
        if (!fullAccess && !minister) {
            return tasks; // 普通成员无管理待办
        }
        if (fullAccess) {
            long incompleteProfileCount = userAccountRepository.countIncompleteProfilesOfActiveMembers();
            tasks.add(DashboardStatsResponse.PendingTask.builder()
                    .type("profile")
                    .title("待完善资料成员")
                    .desc("引导新成员完善个人资料")
                    .count(incompleteProfileCount)
                    .to("/members")
                    .build());
        }
        if (fullAccess) {
            tasks.add(DashboardStatsResponse.PendingTask.builder()
                    .type("review")
                    .title("待审核积分申请")
                    .desc("处理成员提交的积分申请")
                    .count(pendingApplications)
                    .to("/point-applications")
                    .build());
        }
        tasks.add(DashboardStatsResponse.PendingTask.builder()
                .type("homework")
                .title("待批改作业")
                .desc(minister ? "处理本部门已提交但尚未批改的作业" : "处理已提交但尚未批改的作业")
                .count(pendingHomeworkReviews)
                .to("/homework-review")
                .build());
        if (fullAccess) {
            tasks.add(DashboardStatsResponse.PendingTask.builder()
                    .type("finance")
                    .title("本月财务待更新")
                    .desc("本月财务台账尚未建立")
                    .count(currentMonthFinancePending)
                    .to("/finance")
                    .build());
        }
        return tasks;
    }

    private DashboardStatsResponse.RecentActivity toRecentActivity(OperationLog log) {
        String module = log.getModuleName() != null ? log.getModuleName() : "";
        String action = log.getActionType() != null ? log.getActionType() : "";
        String operator = log.getOperatorName() != null ? log.getOperatorName() : "系统";
        return DashboardStatsResponse.RecentActivity.builder()
                .operatorName(operator)
                .operatorDepartment(log.getOperatorDepartment())
                .moduleName(module)
                .actionType(action)
                .title(buildActionTitle(operator, module, action, log.getDescription()))
                .desc(log.getDescription() != null ? log.getDescription() : "")
                .time(formatRelativeTime(log.getCreatedAt()))
                .build();
    }

    /** 将 module + action 翻译为一句人类可读的动作标题，避免暴露英文枚举。 */
    private String buildActionTitle(String operator, String module, String action, String description) {
        String noun = moduleLabel(module);
        String verb = actionVerb(action);
        String base = (verb == null)
                ? operator + " 进行了" + noun + "操作"
                : operator + verb + "了" + noun;
        // 从描述中提取《标题》附加到标题，让标题更具体（如「张强 提交作业《A2》」）
        if (description != null) {
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("《([^》]+)》").matcher(description);
            if (m.find()) {
                return base + "《" + m.group(1) + "》";
            }
        }
        return base;
    }

    private String moduleLabel(String module) {
        return switch (module) {
            case "member" -> "成员";
            case "point_item" -> "积分项目";
            case "point_application" -> "积分申请";
            case "point_record" -> "积分记录";
            case "homework" -> "作业";
            case "homework_submission" -> "作业";
            case "meeting" -> "会议纪要";
            case "finance" -> "财务";
            case "auth" -> "账号";
            case "cohort" -> "届次";
            default -> "系统";
        };
    }

    private String actionVerb(String action) {
        return switch (action) {
            case "CREATE" -> "新增";
            case "UPDATE" -> "更新";
            case "DELETE" -> "删除";
            case "UPLOAD" -> "上传";
            case "APPROVE" -> "审核通过";
            case "REJECT" -> "驳回";
            case "PUBLISH" -> "发布";
            case "CLOSE" -> "关闭";
            case "SUBMIT" -> "提交";
            case "RESUBMIT" -> "重新提交";
            case "GRADE" -> "批改";
            case "REGRADE" -> "重新批改";
            default -> null;
        };
    }

    private long currentMonthFinancePending() {
        LocalDate now = LocalDate.now();
        return financePeriodRepository.findByFinanceYearAndFinanceMonth(now.getYear(), now.getMonthValue())
                .map(period -> 0L)
                .orElse(1L);
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
