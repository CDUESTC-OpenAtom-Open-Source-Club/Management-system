package com.openatom.club.cohort.controller;

import com.openatom.club.cohort.dto.CohortRequest;
import com.openatom.club.cohort.dto.CohortResponse;
import com.openatom.club.cohort.service.CohortService;
import com.openatom.club.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "届次管理")
@RestController
@RequestMapping("/api/cohorts")
@RequiredArgsConstructor
public class CohortController {
    private final CohortService cohortService;

    @Operation(summary = "查询届次列表（按年份倒序）")
    @GetMapping
    public ApiResponse<List<CohortResponse>> list() {
        return ApiResponse.success(cohortService.list());
    }

    @Operation(summary = "新增届次")
    @PostMapping
    public ApiResponse<CohortResponse> create(@Valid @RequestBody CohortRequest req) {
        return ApiResponse.success(cohortService.create(req));
    }

    @Operation(summary = "修改届次（启用/停用/改年份）")
    @PutMapping("/{id}")
    public ApiResponse<CohortResponse> update(@PathVariable Long id,
                                               @Valid @RequestBody CohortRequest req) {
        return ApiResponse.success(cohortService.update(id, req));
    }

    @Operation(summary = "删除届次（软删除，被引用时拒绝）")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        cohortService.delete(id);
        return ApiResponse.success();
    }
}
