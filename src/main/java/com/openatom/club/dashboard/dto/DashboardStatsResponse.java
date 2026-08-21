package com.openatom.club.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsResponse {

    private long totalMembers;
    private long totalPointItems;
    private long pendingApplications;
    private long totalMeetingMinutes;
    private long totalFinancePeriods;

    /** 待批改作业数（按权限 + 届次计算） */
    private long pendingHomeworkReviews;
    /** 本月财务台账是否待建立：1=待更新，0=已建立 */
    private long currentMonthFinancePending;

    private List<DepartmentStat> departmentDistribution;
    private List<WeeklyTrend> weeklyTrend;
    private List<PendingTask> pendingTasks;
    private List<RecentActivity> recentActivities;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DepartmentStat {
        private String label;
        private long value;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeeklyTrend {
        private int week;
        private long count;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PendingTask {
        /** 任务类型，供前端映射图标/配色：profile / review / homework / finance */
        private String type;
        private String title;
        private String desc;
        private long count;
        private String to;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentActivity {
        private String operatorName;
        private String operatorDepartment;
        private String moduleName;
        private String actionType;
        private String title;
        private String desc;
        private String time;
    }
}
