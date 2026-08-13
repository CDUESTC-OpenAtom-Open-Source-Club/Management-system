package com.openatom.club.finance.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.finance.dto.*;
import com.openatom.club.finance.service.FinanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.util.List;

@Tag(name = "财务台账")
@RestController
@RequestMapping("/api/finance")
@RequiredArgsConstructor
public class FinanceController {
    private final FinanceService financeService;

    @Operation(summary = "查询财务月份列表")
    @GetMapping("/periods")
    public ApiResponse<List<FinancePeriodResponse>> listPeriods(
            @RequestParam(required = false) Integer year) {
        return ApiResponse.success(financeService.listPeriods(year));
    }

    @Operation(summary = "获取某月财务详情（报表+凭据）")
    @GetMapping("/{year}/{month}")
    public ApiResponse<FinanceMonthDetailResponse> getMonthDetail(
            @PathVariable Integer year,
            @PathVariable Integer month) {
        return ApiResponse.success(financeService.getMonthDetail(year, month));
    }

    @Operation(summary = "上传每月支出报表（替换旧版本）")
    @PostMapping(value = "/{year}/{month}/report", consumes = "multipart/form-data")
    public ApiResponse<FinanceFileResponse> uploadReport(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestPart MultipartFile file,
            @RequestParam(required = false) String remark) throws Exception {
        return ApiResponse.success(financeService.uploadReport(year, month, file, remark));
    }

    @Operation(summary = "上传凭据文件（可多个）")
    @PostMapping(value = "/{year}/{month}/vouchers", consumes = "multipart/form-data")
    public ApiResponse<List<FinanceFileResponse>> uploadVouchers(
            @PathVariable Integer year,
            @PathVariable Integer month,
            @RequestPart List<MultipartFile> files,
            @RequestParam(required = false) String remark) throws Exception {
        return ApiResponse.success(financeService.uploadVouchers(year, month, files, remark));
    }

    @Operation(summary = "下载财务文件")
    @GetMapping("/files/{financeFileId}/download")
    public void download(@PathVariable Long financeFileId, HttpServletResponse response) throws Exception {
        financeService.download(financeFileId, response);
    }

    @Operation(summary = "在线查看财务文件")
    @GetMapping("/files/{financeFileId}/view")
    public void view(@PathVariable Long financeFileId, HttpServletResponse response) throws Exception {
        financeService.view(financeFileId, response);
    }

    @Operation(summary = "删除财务文件")
    @DeleteMapping("/files/{financeFileId}")
    public ApiResponse<Void> delete(@PathVariable Long financeFileId) {
        financeService.delete(financeFileId);
        return ApiResponse.success();
    }
}
