package com.openatom.club.homework.controller;

import com.openatom.club.common.response.ApiResponse;
import com.openatom.club.common.response.PageResult;
import com.openatom.club.homework.dto.GradeSubmissionRequest;
import com.openatom.club.homework.dto.HomeworkSubmissionResponse;
import com.openatom.club.homework.service.HomeworkSubmissionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "作业提交与批改")
@RestController
@RequiredArgsConstructor
public class HomeworkSubmissionController {
    private final HomeworkSubmissionService submissionService;

    @Operation(summary = "成员：提交/更新作业")
    @PostMapping("/api/homeworks/{homeworkId}/submit")
    public ApiResponse<HomeworkSubmissionResponse> submit(@PathVariable Long homeworkId,
                                                           @RequestParam(required = false) String content,
                                                           @RequestParam(required = false) List<MultipartFile> files) throws IOException {
        return ApiResponse.success(submissionService.submitHomework(homeworkId, content, files));
    }

    @Operation(summary = "成员：查看自己的提交状态")
    @GetMapping("/api/homeworks/{homeworkId}/my-submission")
    public ApiResponse<HomeworkSubmissionResponse> mySubmission(@PathVariable Long homeworkId) {
        return ApiResponse.success(submissionService.getMySubmission(homeworkId));
    }

    @Operation(summary = "管理员：查看作业的所有提交")
    @GetMapping("/api/homeworks/{homeworkId}/submissions")
    public ApiResponse<PageResult<HomeworkSubmissionResponse>> listSubmissions(@PathVariable Long homeworkId,
                                                                                @RequestParam(required = false) String status,
                                                                                @RequestParam(defaultValue = "1") int page,
                                                                                @RequestParam(defaultValue = "20") int size) {
        var resultPage = submissionService.listSubmissions(homeworkId, status, page, size);
        return ApiResponse.success(
                new PageResult<>(resultPage.getContent(), resultPage.getTotalElements(), page, size));
    }

    @Operation(summary = "管理员：查看单个提交")
    @GetMapping("/api/homework-submissions/{id}")
    public ApiResponse<HomeworkSubmissionResponse> getSubmission(@PathVariable Long id) {
        return ApiResponse.success(submissionService.getSubmission(id));
    }

    @Operation(summary = "批改作业")
    @PostMapping("/api/homework-submissions/{id}/grade")
    public ApiResponse<HomeworkSubmissionResponse> grade(@PathVariable Long id,
                                                          @Valid @RequestBody GradeSubmissionRequest req) {
        return ApiResponse.success(submissionService.gradeSubmission(id, req));
    }

    @Operation(summary = "下载作业附件（带权限校验）")
    @GetMapping("/api/homework-submissions/{submissionId}/files/{fileId}/download")
    public void downloadFile(@PathVariable Long submissionId, @PathVariable Long fileId,
                              HttpServletResponse response) throws IOException {
        submissionService.downloadSubmissionFile(submissionId, fileId, response);
    }

    @Operation(summary = "在线查看作业附件（带权限校验）")
    @GetMapping("/api/homework-submissions/{submissionId}/files/{fileId}/view")
    public void viewFile(@PathVariable Long submissionId, @PathVariable Long fileId,
                          HttpServletResponse response) throws IOException {
        submissionService.viewSubmissionFile(submissionId, fileId, response);
    }
}
