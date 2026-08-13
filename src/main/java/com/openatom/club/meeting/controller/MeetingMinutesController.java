package com.openatom.club.meeting.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.meeting.dto.MeetingMinutesResponse;
import com.openatom.club.meeting.service.MeetingMinutesService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "会议纪要管理")
@RestController
@RequestMapping("/api/meeting-minutes")
@RequiredArgsConstructor
public class MeetingMinutesController {
    private final MeetingMinutesService meetingMinutesService;

    @Operation(summary = "查询会议纪要列表")
    @GetMapping
    public ApiResponse<PageResult<MeetingMinutesResponse>> list(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(meetingMinutesService.list(year, month, keyword, page, size));
    }

    @Operation(summary = "上传会议纪要")
    @PostMapping(consumes = "multipart/form-data")
    public ApiResponse<MeetingMinutesResponse> create(
            @RequestParam String title,
            @RequestParam String meetingDate,
            @RequestParam(required = false) String remark,
            @RequestPart MultipartFile file) throws Exception {
        return ApiResponse.success(meetingMinutesService.create(title, meetingDate, remark, file));
    }

    @Operation(summary = "下载会议纪要文件")
    @GetMapping("/{id}/download")
    public void download(@PathVariable Long id, HttpServletResponse response) throws Exception {
        meetingMinutesService.download(id, response);
    }

    @Operation(summary = "修改会议纪要")
    @PutMapping(value = "/{id}", consumes = "multipart/form-data")
    public ApiResponse<MeetingMinutesResponse> update(
            @PathVariable Long id,
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String meetingDate,
            @RequestParam(required = false) String remark,
            @RequestPart(required = false) MultipartFile file) throws Exception {
        return ApiResponse.success(meetingMinutesService.update(id, title, meetingDate, remark, file));
    }

    @Operation(summary = "删除会议纪要")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        meetingMinutesService.delete(id);
        return ApiResponse.success();
    }
}
