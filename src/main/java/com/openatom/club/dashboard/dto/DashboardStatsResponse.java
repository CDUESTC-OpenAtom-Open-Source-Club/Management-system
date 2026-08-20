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
        private String title;
        private String desc;
        private String time;
    }
}
