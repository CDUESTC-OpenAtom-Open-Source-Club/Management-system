package com.openatom.club.archive.controller;

import com.openatom.club.archive.dto.ArchiveLinkRequest;
import com.openatom.club.archive.dto.ArchiveLinkResponse;
import com.openatom.club.archive.service.ArchiveLinkService;
import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.common.response.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@Tag(name = "活动资料归档链接")
@RestController
@RequestMapping("/api/archive-links")
@RequiredArgsConstructor
public class ArchiveLinkController {
    private final ArchiveLinkService archiveLinkService;

    @Operation(summary = "查询归档链接列表")
    @GetMapping
    public ApiResponse<PageResult<ArchiveLinkResponse>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(archiveLinkService.list(year, type, keyword, page, size));
    }

    @Operation(summary = "新增归档链接")
    @PostMapping
    public ApiResponse<ArchiveLinkResponse> create(@Valid @RequestBody ArchiveLinkRequest req) {
        return ApiResponse.success(archiveLinkService.create(req));
    }

    @Operation(summary = "修改归档链接")
    @PutMapping("/{id}")
    public ApiResponse<ArchiveLinkResponse> update(@PathVariable Long id,
                                                    @Valid @RequestBody ArchiveLinkRequest req) {
        return ApiResponse.success(archiveLinkService.update(id, req));
    }

    @Operation(summary = "删除归档链接")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        archiveLinkService.delete(id);
        return ApiResponse.success();
    }
}
