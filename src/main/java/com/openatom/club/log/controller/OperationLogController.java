package com.openatom.club.log.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.log.dto.OperationLogResponse;
import com.openatom.club.log.service.OperationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "操作日志")
@RestController
@RequestMapping("/api/operation-logs")
@RequiredArgsConstructor
public class OperationLogController {
    private final OperationLogService logService;

    @Operation(summary = "查询操作日志（仅会长、副会长）")
    @GetMapping
    public ApiResponse<PageResult<OperationLogResponse>> list(
            @RequestParam(required = false) String moduleName,
            @RequestParam(required = false) String actionType,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(logService.list(moduleName, actionType, keyword, page, size));
    }
}
