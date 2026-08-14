package com.openatom.club.point.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.point.dto.PointApplicationResponse;
import com.openatom.club.point.dto.PointApplicationSubmitRequest;
import com.openatom.club.point.dto.RejectRequest;
import com.openatom.club.point.service.PointApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "成员活动登记与审核")
@RestController
@RequestMapping("/api/point-applications")
@RequiredArgsConstructor
public class PointApplicationController {
    private final PointApplicationService applicationService;

    @Operation(summary = "提交活动登记")
    @PostMapping
    public ApiResponse<List<PointApplicationResponse>> submit(
            @Valid @RequestBody PointApplicationSubmitRequest req) {
        return ApiResponse.success(applicationService.submit(req));
    }

    @Operation(summary = "查询我的登记记录")
    @GetMapping("/my")
    public ApiResponse<List<PointApplicationResponse>> myApplications() {
        return ApiResponse.success(applicationService.myApplications());
    }

    @Operation(summary = "查询全部登记（秘书处及以上，cohortId: 空=全部，-1=未分届）")
    @GetMapping
    public ApiResponse<PageResult<PointApplicationResponse>> listAll(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long cohortId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(applicationService.listAll(status, cohortId, keyword, page, size));
    }

    @Operation(summary = "审核通过")
    @PostMapping("/{id}/approve")
    public ApiResponse<Void> approve(@PathVariable Long id) {
        applicationService.approve(id);
        return ApiResponse.success();
    }

    @Operation(summary = "驳回")
    @PostMapping("/{id}/reject")
    public ApiResponse<Void> reject(@PathVariable Long id,
                                     @Valid @RequestBody RejectRequest req) {
        applicationService.reject(id, req);
        return ApiResponse.success();
    }
}
