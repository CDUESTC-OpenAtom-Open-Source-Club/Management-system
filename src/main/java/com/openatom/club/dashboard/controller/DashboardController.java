package com.openatom.club.dashboard.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.dashboard.dto.DashboardStatsResponse;
import com.openatom.club.dashboard.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "首页概览")
@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @Operation(summary = "获取首页统计数据（cohortId: 空=全部，-1=未分届）")
    @GetMapping("/stats")
    public ApiResponse<DashboardStatsResponse> getStats(@RequestParam(required = false) Long cohortId) {
        return ApiResponse.success(dashboardService.getStats(cohortId));
    }
}
