package com.openatom.club.point.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.point.dto.PointItemRequest;
import com.openatom.club.point.dto.PointItemResponse;
import com.openatom.club.point.service.PointItemService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "积分项目管理")
@RestController
@RequestMapping("/api/point-items")
@RequiredArgsConstructor
public class PointItemController {
    private final PointItemService pointItemService;

    @Operation(summary = "查询积分项目列表")
    @GetMapping
    public ApiResponse<List<PointItemResponse>> list() {
        return ApiResponse.success(pointItemService.list());
    }

    @Operation(summary = "获取成员登记复选框选项")
    @GetMapping("/apply-options")
    public ApiResponse<List<PointItemResponse>> applyOptions() {
        return ApiResponse.success(pointItemService.applyOptions());
    }

    @Operation(summary = "新增积分项目")
    @PostMapping
    public ApiResponse<PointItemResponse> create(@Valid @RequestBody PointItemRequest req) {
        return ApiResponse.success(pointItemService.create(req));
    }

    @Operation(summary = "修改积分项目")
    @PutMapping("/{id}")
    public ApiResponse<PointItemResponse> update(@PathVariable Long id,
                                                  @Valid @RequestBody PointItemRequest req) {
        return ApiResponse.success(pointItemService.update(id, req));
    }

    @Operation(summary = "删除积分项目")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        pointItemService.delete(id);
        return ApiResponse.success();
    }
}
