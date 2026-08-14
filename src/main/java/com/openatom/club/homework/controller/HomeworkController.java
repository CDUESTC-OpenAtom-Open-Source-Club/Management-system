package com.openatom.club.homework.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.homework.dto.HomeworkAssignmentRequest;
import com.openatom.club.homework.dto.HomeworkAssignmentResponse;
import com.openatom.club.homework.service.HomeworkService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "作业管理")
@RestController
@RequiredArgsConstructor
public class HomeworkController {
    private final HomeworkService homeworkService;

    @Operation(summary = "管理员：列出作业（cohortId/department 可选，部长固定本部门）")
    @GetMapping("/api/homeworks/manage")
    public ApiResponse<PageResult<HomeworkAssignmentResponse>> listManage(@RequestParam(required = false) String status,
                                                                           @RequestParam(required = false) Long cohortId,
                                                                           @RequestParam(required = false) String department,
                                                                           @RequestParam(defaultValue = "1") int page,
                                                                           @RequestParam(defaultValue = "20") int size) {
        Page<HomeworkAssignmentResponse> result = homeworkService.listAssignments(status, cohortId, department, page, size);
        return ApiResponse.success(new PageResult<>(result.getContent(), result.getTotalElements(), page, size));
    }

    @Operation(summary = "成员：列出我的作业")
    @GetMapping("/api/homeworks")
    public ApiResponse<List<HomeworkAssignmentResponse>> listMy() {
        return ApiResponse.success(homeworkService.listMyHomework());
    }

    @Operation(summary = "查看作业详情")
    @GetMapping("/api/homeworks/{id}")
    public ApiResponse<HomeworkAssignmentResponse> getDetail(@PathVariable Long id) {
        return ApiResponse.success(homeworkService.getAssignment(id));
    }

    @Operation(summary = "创建作业")
    @PostMapping("/api/homeworks")
    public ApiResponse<HomeworkAssignmentResponse> create(@Valid @RequestBody HomeworkAssignmentRequest req) {
        return ApiResponse.success(homeworkService.createAssignment(req));
    }

    @Operation(summary = "编辑作业")
    @PutMapping("/api/homeworks/{id}")
    public ApiResponse<HomeworkAssignmentResponse> update(@PathVariable Long id,
                                                           @Valid @RequestBody HomeworkAssignmentRequest req) {
        return ApiResponse.success(homeworkService.updateAssignment(id, req));
    }

    @Operation(summary = "发布作业")
    @PostMapping("/api/homeworks/{id}/publish")
    public ApiResponse<HomeworkAssignmentResponse> publish(@PathVariable Long id) {
        return ApiResponse.success(homeworkService.publishAssignment(id));
    }

    @Operation(summary = "关闭作业")
    @PostMapping("/api/homeworks/{id}/close")
    public ApiResponse<HomeworkAssignmentResponse> close(@PathVariable Long id) {
        return ApiResponse.success(homeworkService.closeAssignment(id));
    }

    @Operation(summary = "删除作业")
    @DeleteMapping("/api/homeworks/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        homeworkService.deleteAssignment(id);
        return ApiResponse.success();
    }
}
