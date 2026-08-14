package com.openatom.club.point.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.point.dto.PointTableResult;
import com.openatom.club.point.dto.SearchPositionResult;
import com.openatom.club.point.service.PointTableService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "积分表")
@RestController
@RequestMapping("/api/points")
@RequiredArgsConstructor
public class PointTableController {
    private final PointTableService pointTableService;

    @Operation(summary = "获取积分表（动态列，cohortId: 空=全部，-1=未分届）")
    @GetMapping("/table")
    public ApiResponse<PointTableResult> getTable(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "50") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long cohortId) {
        return ApiResponse.success(pointTableService.getTable(page, size, keyword, cohortId));
    }

    @Operation(summary = "搜索成员在积分表中的位置")
    @GetMapping("/table/search-position")
    public ApiResponse<SearchPositionResult> searchPosition(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "50") int pageSize,
            @RequestParam(defaultValue = "0") int matchIndex,
            @RequestParam(required = false) Long cohortId) {
        return ApiResponse.success(pointTableService.searchPosition(keyword, pageSize, matchIndex, cohortId));
    }
}
