package com.openatom.club.point.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.point.dto.PointRecordRequest;
import com.openatom.club.point.dto.PointRecordResponse;
import com.openatom.club.point.service.PointRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@Tag(name = "积分记录")
@RestController
@RequiredArgsConstructor
public class PointRecordController {
    private final PointRecordService pointRecordService;

    @Operation(summary = "查询成员积分明细")
    @GetMapping("/api/members/{memberId}/point-records")
    public ApiResponse<List<PointRecordResponse>> listByMember(@PathVariable Long memberId) {
        return ApiResponse.success(pointRecordService.listByMember(memberId));
    }

    @Operation(summary = "手动新增积分记录")
    @PostMapping("/api/members/{memberId}/point-records")
    public ApiResponse<PointRecordResponse> create(@PathVariable Long memberId,
                                                    @Valid @RequestBody PointRecordRequest req) {
        return ApiResponse.success(pointRecordService.create(memberId, req));
    }

    @Operation(summary = "修改积分记录")
    @PutMapping("/api/point-records/{id}")
    public ApiResponse<PointRecordResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody PointRecordRequest req) {
        return ApiResponse.success(pointRecordService.update(id, req));
    }

    @Operation(summary = "删除积分记录")
    @DeleteMapping("/api/point-records/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        pointRecordService.delete(id);
        return ApiResponse.success();
    }
}
